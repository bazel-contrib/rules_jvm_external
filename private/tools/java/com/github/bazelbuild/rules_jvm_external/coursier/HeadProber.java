// Copyright 2026 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.bazelbuild.rules_jvm_external.coursier;

import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * HEAD-only URI prober for verifying that a mirror hosts a given artifact.
 *
 * <p>Supported schemes: {@code http}, {@code https}, {@code file}. Any non-2xx response or I/O
 * failure collapses to {@code false}. Three categories of failure also emit a once-per-host
 * warning to {@code System.err}: authentication failures (401/403/407), transient upstream
 * conditions (408, 429, 5xx), and non-HTTP transport failures (timeout, DNS, TLS, malformed
 * URL). Routine 404s and missing local files are intentionally silent -- they are the common
 * "this mirror does not have the artifact" case and would otherwise drown out the warnings
 * that matter.
 *
 * <p>Probes are one-shot: there is no retry-with-backoff. A transient 408/429/5xx or transport
 * fault during lockfile regeneration ({@code bazel run @maven//:pin}) will drop the affected
 * mirror from the regenerated lockfile and surface as a single once-per-host warning. The
 * recovery is to re-run the pin once the upstream recovers -- the lockfile is regenerated
 * from scratch each time, so a single flaky run is not load-bearing on subsequent ones.
 */
final class HeadProber implements Predicate<URI> {

  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 10_000;

  private final Netrc netrc;
  private final Set<String> reportedAuthFailures = ConcurrentHashMap.newKeySet();
  private final Set<String> reportedTransientFailures = ConcurrentHashMap.newKeySet();
  private final Set<String> reportedTransportFailures = ConcurrentHashMap.newKeySet();

  HeadProber(Netrc netrc) {
    this.netrc = Objects.requireNonNull(netrc, "netrc");
  }

  @Override
  public boolean test(URI uri) {
    HttpURLConnection conn = null;
    try {
      if ("file".equals(uri.getScheme())) {
        return Files.exists(Paths.get(uri));
      }
      conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setRequestMethod("HEAD");
      conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
      conn.setReadTimeout(READ_TIMEOUT_MS);
      conn.setInstanceFollowRedirects(true);
      // Some upstreams reject unknown clients with 403; the probe should look identical to
      // what the actual download will look like, so this UA string is kept in sync with
      // HttpDownloader's. If the two ever drift, a private mirror could pass the probe and
      // fail the download (or vice versa).
      conn.setRequestProperty("User-Agent", "rules_jvm_external resolver");
      addBasicAuthIfKnown(conn, uri);
      int code = conn.getResponseCode();
      if (code >= 400) {
        // Drain the error body so the close is a clean FIN rather than an RST mid-response.
        // Some JDK versions and upstream load balancers log noisily or fail the next request
        // on the same socket when they see a mid-stream RST; 4xx bodies are small so the drain
        // is cheap.
        //
        // Disconnect rather than letting the JDK pool the socket: probes are interleaved
        // across hosts, so a pooled socket would rarely be reused before keep-alive timeout.
        drainAndClose(conn.getErrorStream());
        conn.disconnect();
        String host = dedupKey(uri);
        if (code == 401 || code == 403 || code == 407) {
          logOnce(reportedAuthFailures, host,
              "HeadProber: " + host + " returned HTTP " + code
                  + " -- check ~/.netrc credentials. Mirrors on this host will be excluded from"
                  + " the lockfile.");
        } else if (code == 408 || code == 429 || code >= 500) {
          // Transient upstream conditions that thin the lockfile silently if not surfaced:
          // 408 Request Timeout, 429 Too Many Requests (rate-limited by Maven Central /
          // Sonatype / JFrog SaaS under CI load), and any 5xx (upstream restart, misconfigured
          // reverse proxy, partial outage). Operator action is the same for all three: rerun
          // pin once the upstream recovers.
          logOnce(reportedTransientFailures, host,
              "HeadProber: " + host + " returned HTTP " + code
                  + " -- upstream is unhealthy or rate-limiting. Mirrors on this host will be"
                  + " excluded from the lockfile; rerun pin once the upstream recovers.");
        }
        return false;
      }
      // 2xx: closing the response stream lets the JDK return the underlying socket to its HTTP
      // keep-alive pool. Calling disconnect() instead would close the socket, forcing a fresh
      // TCP/TLS handshake on the next probe -- expensive when probing thousands of artifacts
      // against the same handful of hosts.
      //
      // The 2xx verdict is already known once getResponseCode() returns, so a body-teardown
      // IOException (chunked-framing error, mid-stream RST, etc.) must NOT flip the verdict
      // to false via the outer catch. That would log a transport failure and exclude a mirror
      // that legitimately has the artifact -- the exact false-positive thinning this design
      // works to prevent. Force disconnect on close-failure since the socket is in an
      // indeterminate state.
      try {
        InputStream stream = conn.getInputStream();
        if (stream != null) {
          stream.close();
        }
      } catch (IOException ignored) {
        conn.disconnect();
      }
      return code >= 200 && code < 300;
    } catch (IOException | ClassCastException | IllegalArgumentException e) {
      // Conservative answer is "skip this mirror" for any transport failure. A false negative
      // just drops a backup repo from this lockfile; a false positive would record a repo that
      // can't actually serve the artifact and resurrect the 404-warning noise. The connection
      // is in an indeterminate state, so disconnect rather than returning it to the keep-alive
      // pool.
      //
      // Catch is narrow on purpose: IOException covers timeouts, DNS failures, TLS faults,
      // and MalformedURLException (an IOException subclass) from uri.toURL();
      // ClassCastException covers non-HTTP schemes returning a non-HttpURLConnection from
      // openConnection(); IllegalArgumentException covers Paths.get(uri) on opaque file URIs.
      // Broader RuntimeExceptions (NPE, ArrayIndexOutOfBoundsException, etc.) are programmer
      // bugs and are deliberately allowed to propagate so CI catches them rather than
      // silently shipping a thinned lockfile.
      if (conn != null) {
        conn.disconnect();
      }
      // Log once per host so the operator sees the symptom of a network outage or
      // misconfiguration. Without this, a flaky upstream during pin produces a confidently-thin
      // lockfile and the user has no way to tell it happened.
      String host = dedupKey(uri);
      logOnce(reportedTransportFailures, host,
          "HeadProber: " + host + " -> " + e.getClass().getName()
              + (e.getMessage() == null ? "" : ": " + e.getMessage())
              + ". Mirrors on this host will be excluded from the lockfile.");
      return false;
    }
  }

  private static String dedupKey(URI uri) {
    // Opaque URIs (URI.getHost() returns null) reach this method only via the catch block --
    // hierarchical file URIs return early at the file-scheme branch. Fall back to the full
    // URI so opaque probes still occupy a slot in the dedup set rather than coalescing into
    // a single null key.
    String host = uri.getHost();
    return host != null ? host : uri.toString();
  }

  private static void drainAndClose(InputStream stream) {
    if (stream == null) {
      return;
    }
    try (InputStream s = stream) {
      byte[] buf = new byte[1024];
      while (s.read(buf) != -1) {
        // discard
      }
    } catch (IOException ignored) {
      // Already on the failure path; nothing useful to do.
    }
  }

  private static void logOnce(Set<String> seen, String key, String message) {
    if (key != null && seen.add(key)) {
      System.err.println(message);
    }
  }

  private void addBasicAuthIfKnown(HttpURLConnection conn, URI uri) {
    // Note: Netrc.getCredential falls back to the netrc default credential when there is no
    // per-host entry, so when this prober is constructed from Netrc.fromUserHome(), a
    // `default` entry in ~/.netrc will be sent on every probe regardless of host.
    Netrc.Credential credential = netrc.getCredential(uri.getHost());
    if (credential == null) {
      return;
    }
    String login = credential.login();
    String account = credential.account();
    String userName = (account != null && !account.isEmpty()) ? account : login;
    if (userName == null) {
      return;
    }
    String password = credential.password() == null ? "" : credential.password();
    String token =
        Base64.getEncoder()
            .encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
    conn.setRequestProperty("Authorization", "Basic " + token);
  }
}
