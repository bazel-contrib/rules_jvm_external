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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.collect.ImmutableMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HeadProberTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private HttpServer server;
  private URI baseUri;
  private PrintStream originalErr;
  private ByteArrayOutputStream capturedErr;

  @Before
  public void setUp() throws IOException {
    // Capture originalErr first so a failure in HttpServer.create can't leave the JVM with
    // a null System.err (which would cascade into every subsequent test in the same JVM).
    originalErr = System.err;
    capturedErr = new ByteArrayOutputStream();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.start();
    baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
    if (originalErr != null) {
      System.setErr(originalErr);
    }
  }

  private HeadProber emptyNetrcProber() {
    return new HeadProber(new Netrc(null, ImmutableMap.of()));
  }

  private void serve(String path, int status) {
    server.createContext(path, new FixedStatusHandler(status));
  }

  @Test
  public void returnsFalseOn404SilentlyWithoutLogging() {
    serve("/missing", 404);
    HeadProber prober = emptyNetrcProber();
    assertFalse(prober.test(baseUri.resolve("missing")));
    assertEquals("404 must not be logged (it is the routine 'not present' case)",
        "", capturedErr.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void rateLimit429LogsAsTransientFailure() {
    serve("/throttled", 429);
    HeadProber prober = emptyNetrcProber();
    assertFalse(prober.test(baseUri.resolve("throttled")));
    String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue("429 must be logged so a thinned lockfile is not silent; stderr=\n" + stderr,
        stderr.contains("HeadProber") && stderr.contains("429"));
  }

  @Test
  public void transportFailureLogsOncePerHost() {
    // Connect to a port nothing is listening on -- TCP refused, IOException.
    URI deadHost = URI.create("http://127.0.0.1:1/dead");
    HeadProber prober = emptyNetrcProber();
    assertFalse(prober.test(deadHost));
    assertFalse(prober.test(URI.create("http://127.0.0.1:1/dead2")));
    String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    long lineCount = stderr.lines().filter(l -> l.contains("HeadProber")).count();
    assertEquals(
        "two transport failures against the same host should produce one log line; stderr=\n"
            + stderr,
        1, lineCount);
  }

  @Test
  public void authFailureLogsOncePerDistinctHost() {
    // localhost and 127.0.0.1 point at the same machine but URI.getHost() returns distinct
    // strings, so each must produce its own log line. Stops a regression that globalises the
    // dedup set.
    serve("/h1", 401);
    HeadProber prober = emptyNetrcProber();
    int port = server.getAddress().getPort();
    assertFalse(prober.test(URI.create("http://127.0.0.1:" + port + "/h1")));
    assertFalse(prober.test(URI.create("http://localhost:" + port + "/h1")));
    String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    long lineCount = stderr.lines().filter(l -> l.contains("HeadProber")).count();
    assertEquals(
        "127.0.0.1 and localhost are distinct hosts under URI.getHost(); expected two log lines;"
            + " stderr=\n" + stderr,
        2, lineCount);
  }

  @Test
  public void followsRedirectToFinalTarget() {
    // 301 -> 200 within the same server. HttpURLConnection's per-connection follow-default
    // is true, but a JVM that has globally flipped HttpURLConnection.setFollowRedirects(false)
    // would otherwise stop the prober at the 301 and return false (the 301 status code does
    // not satisfy >=200 && <300). The explicit setInstanceFollowRedirects(true) call locks in
    // follow-on behavior regardless of process-wide state.
    server.createContext("/redir", new RedirectHandler("/target"));
    serve("/target", 200);
    HeadProber prober = emptyNetrcProber();
    assertTrue(prober.test(baseUri.resolve("redir")));
    assertEquals("redirect-followed 200 must not log; stderr=\n"
            + capturedErr.toString(StandardCharsets.UTF_8),
        "", capturedErr.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void distinctFailureClassesEachLogOncePerHost() {
    // Covers (a) each bucket dedups per host, (b) buckets don't collide with each other, and
    // (c) the auth/transient log strings name the actionable signal (.netrc / upstream) so an
    // operator reading stderr knows what to do.
    serve("/auth1", 401);
    serve("/auth2", 401);
    serve("/oops1", 503);
    serve("/oops2", 502);
    HeadProber prober = emptyNetrcProber();
    assertFalse(prober.test(baseUri.resolve("auth1")));
    assertFalse(prober.test(baseUri.resolve("auth2")));
    assertFalse(prober.test(baseUri.resolve("oops1")));
    assertFalse(prober.test(baseUri.resolve("oops2")));
    String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    long lineCount = stderr.lines().filter(l -> l.contains("HeadProber")).count();
    assertEquals(
        "two auth failures + two server errors against the same host should produce two log"
            + " lines (one per bucket, deduped); stderr=\n" + stderr,
        2, lineCount);
    assertTrue("auth log should mention .netrc; stderr=\n" + stderr, stderr.contains(".netrc"));
    assertTrue(
        "server-error log should mention 'upstream'; stderr=\n" + stderr,
        stderr.contains("upstream"));
  }

  @Test
  public void fileSchemeExistsReturnsTrue() throws IOException {
    Path file = tempFolder.newFile("present.jar").toPath();
    Files.write(file, "x".getBytes(StandardCharsets.UTF_8));
    assertTrue(emptyNetrcProber().test(file.toUri()));
  }

  @Test
  public void fileSchemeMissingReturnsFalseWithoutLogging() {
    Path missing = tempFolder.getRoot().toPath().resolve("missing.jar");
    HeadProber prober = emptyNetrcProber();
    assertFalse(prober.test(missing.toUri()));
    assertEquals("missing file is the routine 'not present' case; no log expected",
        "", capturedErr.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void opaqueFileUriCollapsesToFalseAndDedupKeyIsTheUri() {
    // Opaque file: URI (no //) parses fine but Paths.get(uri) throws
    // IllegalArgumentException "URI is not hierarchical" -- exercises the narrowed catch.
    // URI.getHost() returns null for opaque URIs, so the dedupKey fallback is what keeps the
    // log line from being suppressed. A second probe against the same opaque URI must coalesce.
    URI opaqueFileUri = URI.create("file:opaque-not-hierarchical");
    HeadProber prober = emptyNetrcProber();
    assertFalse(prober.test(opaqueFileUri));
    assertFalse(prober.test(opaqueFileUri));
    String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    long lineCount = stderr.lines().filter(l -> l.contains("HeadProber")).count();
    assertEquals("two probes of the same opaque URI must dedup; stderr=\n" + stderr,
        1, lineCount);
    assertTrue("dedup key for opaque URI must be the URI itself; stderr=\n" + stderr,
        stderr.contains("file:opaque-not-hierarchical"));
  }

  @Test
  public void unsupportedSchemeCollapsesToFalseAndLogs() {
    // Unknown protocol -> MalformedURLException (IOException subclass) at uri.toURL().
    // Goes through the transport-failure path; should log once with the URI host.
    URI exoticScheme = URI.create("notascheme://example.com/path");
    assertFalse(emptyNetrcProber().test(exoticScheme));
    String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(
        "unsupported scheme should log via transport-failure path; stderr=\n" + stderr,
        stderr.contains("HeadProber") && stderr.contains("example.com"));
  }

  // --- addBasicAuthIfKnown coverage -----------------------------------------------------
  // Every other test uses an empty Netrc, so addBasicAuthIfKnown short-circuits on the first
  // branch. These tests stand up an Authorization-header-capturing handler and verify each
  // credential-selection branch (account-vs-login precedence, null guards, empty password).

  @Test
  public void accountOverridesLoginWhenBothPresent() throws IOException {
    // Per netrc(5), `account` is the effective principal when present. login is the user-facing
    // identity. A regression that swaps the selection would silently authenticate as the wrong
    // principal -- catastrophic on hosts that have distinct service vs human accounts.
    AtomicReference<String> capturedAuth = captureAuth("/ap");
    HeadProber prober = proberWithCredential("127.0.0.1", "person", "secret", "bot");
    assertTrue(prober.test(baseUri.resolve("ap")));
    assertEquals(basic("bot", "secret"), capturedAuth.get());
  }

  @Test
  public void emptyAccountFallsBackToLogin() throws IOException {
    // The Credential builder defaults account to "", which is the most common shape from
    // NetrcParser. Empty-string must fall through to login.
    AtomicReference<String> capturedAuth = captureAuth("/empty");
    HeadProber prober = proberWithCredential("127.0.0.1", "person", "secret", "");
    assertTrue(prober.test(baseUri.resolve("empty")));
    assertEquals(basic("person", "secret"), capturedAuth.get());
  }

  @Test
  public void nullLoginAndAccountSendsNoAuthHeader() throws IOException {
    // NetrcParser can emit credentials with null fields when the entry is malformed. The
    // userName == null guard must short-circuit cleanly; deleting it would NPE in production.
    AtomicReference<String> capturedAuth = captureAuth("/nocreds");
    HeadProber prober = proberWithCredential("127.0.0.1", null, "secret", null);
    assertTrue(prober.test(baseUri.resolve("nocreds")));
    assertEquals("no userName -> no Authorization header", null, capturedAuth.get());
  }

  @Test
  public void nullPasswordEncodesAsEmpty() throws IOException {
    // A regression that drops the `password == null ? "" : password` ternary would NPE on
    // (login + ":" + null).getBytes(...). The encoded form is the empty-password Base64.
    AtomicReference<String> capturedAuth = captureAuth("/nopass");
    HeadProber prober = proberWithCredential("127.0.0.1", "person", null, null);
    assertTrue(prober.test(baseUri.resolve("nopass")));
    assertEquals(basic("person", ""), capturedAuth.get());
  }

  @Test
  public void noNetrcEntryForHostSendsNoAuthHeader() throws IOException {
    // The Netrc lookup is host-keyed; a per-host credential for other.example.com must not
    // leak onto a probe of 127.0.0.1 (this Netrc has no default credential).
    AtomicReference<String> capturedAuth = captureAuth("/other");
    HeadProber prober =
        proberWithCredential("other.example.com", "person", "secret", null);
    assertTrue(prober.test(baseUri.resolve("other")));
    assertEquals("no matching host -> no Authorization header", null, capturedAuth.get());
  }

  @Test
  public void defaultCredentialIsSentToHostsWithoutAPerHostEntry() throws IOException {
    // Per netrc(5), a `default` block applies to every host without a per-host entry. Netrc
    // resolves this via getOrDefault, so a probe of any host the file doesn't name will send
    // these credentials -- including public mirrors. This is the same behavior HttpDownloader
    // has, but it is the footgun the production comment on addBasicAuthIfKnown calls out: a
    // misconfigured `default` leaks credentials to every probed host. A regression that
    // suppressed the default for unknown hosts would silently disable auth on every probe
    // that relies on it and drop every authenticated mirror from the regenerated lockfile.
    AtomicReference<String> capturedAuth = captureAuth("/default");
    Netrc.Credential defaultCred = new Netrc.Credential("default", "bot", "secret", null);
    HeadProber prober = new HeadProber(Netrc.create(defaultCred, ImmutableMap.of()));
    assertTrue(prober.test(baseUri.resolve("default")));
    assertEquals(basic("bot", "secret"), capturedAuth.get());
  }

  @Test
  public void twoHundredCloseFailureDoesNotFlipVerdictToFalse() throws IOException {
    // Critical regression pin: a body-teardown IOException on a 2xx response must NOT flip
    // the verdict to false via the outer catch. That would log a transport failure and
    // exclude a mirror that legitimately has the artifact -- the exact false-positive
    // thinning the design is built to prevent.
    //
    // We induce close-time IOException by sending Content-Length: 100 with no body and
    // immediately closing the connection. The JDK's HEAD handling tolerates the empty body
    // for HEAD requests but the stream close races against the server-side TCP close, which
    // on some JDK / kernel combos surfaces an IOException at stream.close() or read time.
    server.createContext(
        "/truncated",
        exchange -> {
          // Announce a body length we won't deliver, then forcibly close the exchange.
          exchange.getResponseHeaders().add("Content-Length", "100");
          exchange.sendResponseHeaders(200, 100);
          exchange.close();
        });
    HeadProber prober = emptyNetrcProber();
    // The verdict must be true regardless of whether close() throws. If a future refactor
    // moves the close inside the outer catch, this assertion will fail (the catch would
    // route the IOException to the transport-failure path and return false).
    assertTrue(
        "2xx verdict must survive body-teardown failure; stderr=\n"
            + capturedErr.toString(StandardCharsets.UTF_8),
        prober.test(baseUri.resolve("truncated")));
    // And the transport-failure log must NOT have fired for this host.
    String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    assertFalse(
        "2xx close-failure must not log as a transport failure; stderr=\n" + stderr,
        stderr.contains("Mirrors on this host will be excluded"));
  }

  private static String basic(String user, String pass) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
  }

  private AtomicReference<String> captureAuth(String path) {
    AtomicReference<String> captured = new AtomicReference<>();
    server.createContext(
        path,
        exchange -> {
          captured.set(exchange.getRequestHeaders().getFirst("Authorization"));
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    return captured;
  }

  private HeadProber proberWithCredential(
      String host, String login, String password, String account) {
    Netrc.Credential credential = new Netrc.Credential(host, login, password, account);
    return new HeadProber(Netrc.create(null, ImmutableMap.of(host, credential)));
  }

  private static final class RedirectHandler implements HttpHandler {
    private final String target;

    RedirectHandler(String target) {
      this.target = target;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      exchange.getResponseHeaders().add("Location", target);
      exchange.sendResponseHeaders(301, -1);
      exchange.close();
    }
  }

  private static final class FixedStatusHandler implements HttpHandler {
    private final int status;

    FixedStatusHandler(int status) {
      this.status = status;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      exchange.sendResponseHeaders(status, -1);
      exchange.close();
    }
  }
}
