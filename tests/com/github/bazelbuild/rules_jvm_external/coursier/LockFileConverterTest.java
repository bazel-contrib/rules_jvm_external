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

import static com.google.common.base.StandardSystemProperty.USER_HOME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.DependencyInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LockFileConverterTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  /**
   * Coursier reports the actual download location of each artifact in the per-dep "url" field, but
   * rules_jvm_external also synthesises a "mirror_urls" entry that interpolates the artifact path
   * into every configured repo. When converting that output into the v3 lock-file format, the
   * converter must record only the repo that actually has the artifact (the one reported in
   * "url"). Recording every repo whose prefix happens to match a synthetic mirror URL makes the v3
   * lockfile claim that every artifact lives in every repo, which causes Bazel's http_file fetcher
   * to try -- and warn on -- repos that don't actually have the artifact.
   */
  @Test
  public void recordsOnlyThePrimaryRepoForEachArtifact() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    URI centralRepo = URI.create("https://central.example.com/maven/");

    // Order matches what a user typed in MODULE.bazel; LinkedHashSet preserves it.
    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);
    configuredRepos.add(centralRepo);

    // Coursier successfully downloaded the artifact from the internal repo only; rules_jvm_external
    // then synthesised mirror_urls pointing at every configured repo, even though the artifact
    // doesn't actually live in centralRepo.
    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:internal-only:1.0.0\","
            + "    \"directDependencies\": [],"
            + "    \"dependencies\": [],"
            + "    \"file\": \"v1/https/internal.example.com/maven/com/example/internal-only/1.0.0/internal-only-1.0.0.jar\","
            + "    \"url\": \"https://internal.example.com/maven/com/example/internal-only/1.0.0/internal-only-1.0.0.jar\","
            + "    \"mirror_urls\": ["
            + "      \"https://internal.example.com/maven/com/example/internal-only/1.0.0/internal-only-1.0.0.jar\","
            + "      \"https://central.example.com/maven/com/example/internal-only/1.0.0/internal-only-1.0.0.jar\""
            + "    ],"
            + "    \"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    LockFileConverter converter = new LockFileConverter(configuredRepos, json);
    Set<DependencyInfo> infos = converter.getDependencies();

    assertEquals("expected exactly one dependency", 1, infos.size());
    DependencyInfo info = infos.iterator().next();
    assertEquals(
        "v3 lock file should record only the repo Coursier actually fetched from",
        Set.of(internalRepo),
        info.getRepositories());
  }

  /**
   * When a mirror verifier is supplied, the converter probes each non-primary configured repo
   * for the artifact. Mirrors that pass the probe join the primary; mirrors that fail are
   * excluded.
   */
  @Test
  public void recordsVerifiedMirrorsAlongsidePrimaryWhenProberSupplied() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    URI mirrorRepo = URI.create("https://mirror.example.com/maven/");
    URI emptyRepo = URI.create("https://empty.example.com/maven/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);
    configuredRepos.add(mirrorRepo);
    configuredRepos.add(emptyRepo);

    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:multi-host:1.0.0\","
            + "    \"directDependencies\": [],"
            + "    \"dependencies\": [],"
            + "    \"file\": \"v1/https/internal.example.com/maven/com/example/multi-host/1.0.0/multi-host-1.0.0.jar\","
            + "    \"url\": \"https://internal.example.com/maven/com/example/multi-host/1.0.0/multi-host-1.0.0.jar\","
            + "    \"mirror_urls\": ["
            + "      \"https://internal.example.com/maven/com/example/multi-host/1.0.0/multi-host-1.0.0.jar\","
            + "      \"https://mirror.example.com/maven/com/example/multi-host/1.0.0/multi-host-1.0.0.jar\","
            + "      \"https://empty.example.com/maven/com/example/multi-host/1.0.0/multi-host-1.0.0.jar\""
            + "    ],"
            + "    \"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    // Recording prober: mirror.example has the artifact, empty.example does not. Captures
    // probe URIs so the test can assert the exact path the converter derived from the
    // primary URL (catches off-by-one regressions in substring path derivation).
    List<URI> probedUris = Collections.synchronizedList(new ArrayList<>());
    Predicate<URI> prober =
        uri -> {
          probedUris.add(uri);
          return uri.toString().startsWith("https://mirror.example.com/maven/");
        };

    LockFileConverter converter = new LockFileConverter(configuredRepos, json, prober);
    Set<DependencyInfo> infos = converter.getDependencies();

    assertEquals("expected exactly one dependency", 1, infos.size());
    DependencyInfo info = infos.iterator().next();
    assertEquals(
        "verified mirror should join primary; unverified mirror should be excluded",
        Set.of(internalRepo, mirrorRepo),
        info.getRepositories());
    assertTrue(
        "prober should have been asked the exact mirror-side path derived from the primary;"
            + " probedUris=" + probedUris,
        probedUris.contains(
            URI.create(
                "https://mirror.example.com/maven/com/example/multi-host/1.0.0/"
                    + "multi-host-1.0.0.jar")));
    assertTrue(
        "prober should have been asked the empty-host probe URI; probedUris=" + probedUris,
        probedUris.contains(
            URI.create(
                "https://empty.example.com/maven/com/example/multi-host/1.0.0/"
                    + "multi-host-1.0.0.jar")));
  }

  /**
   * When two configured repos share a URI prefix (a common Nexus layout: a parent group
   * URL and a more specific proxy under it), the first repo that prefix-matches the
   * primary URL by LinkedHashSet iteration order wins. Pinning current behavior so a
   * future refactor that swaps the data structure to one without insertion-order semantics
   * surfaces visibly.
   */
  @Test
  public void primaryRepoMatchUsesIterationOrderOnPrefixOverlap() throws IOException {
    URI parent = URI.create("https://nexus.example.com/repository/maven/");
    URI child = URI.create("https://nexus.example.com/repository/maven/proxy/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(parent);
    configuredRepos.add(child);

    // Primary URL is under child but parent is a prefix of child; iteration-order wins.
    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:prefix-test:1.0.0\","
            + "    \"directDependencies\": [],"
            + "    \"dependencies\": [],"
            + "    \"url\": \"https://nexus.example.com/repository/maven/proxy/com/example/prefix-test/1.0.0/prefix-test-1.0.0.jar\","
            + "    \"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    LockFileConverter converter = new LockFileConverter(configuredRepos, json);
    DependencyInfo info = converter.getDependencies().iterator().next();
    assertEquals(
        "iteration order is parent then child; parent prefix-matches first",
        Set.of(parent),
        info.getRepositories());
  }

  /**
   * If Coursier reports a {@code url} that doesn't prefix-match any configured repository -- for
   * example because the user removed a previously-configured repo from MODULE.bazel without
   * re-pinning -- the converter must not guess which repo to record. It should leave the
   * artifact's repository list empty so the lockfile reflects only verifiable evidence. The
   * mirror probe loop must also be skipped in this case, since there's no anchor URL to probe
   * sibling repos against. The synthetic {@code mirror_urls} field is intentionally omitted to
   * make explicit that the converter relies only on {@code url}.
   */
  @Test
  public void recordsNothingWhenPrimaryUrlMatchesNoConfiguredRepo() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);

    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:orphan:1.0.0\","
            + "    \"directDependencies\": [],"
            + "    \"dependencies\": [],"
            + "    \"file\": \"v1/https/orphan.example.com/maven/com/example/orphan/1.0.0/orphan-1.0.0.jar\","
            + "    \"url\": \"https://orphan.example.com/maven/com/example/orphan/1.0.0/orphan-1.0.0.jar\","
            + "    \"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    // A prober that says "yes" to anything would normally pull every mirror in. Used here to
    // prove the empty result comes from the missing primary anchor, not from mirror probing
    // being suppressed for some other reason.
    Predicate<URI> alwaysTrue = uri -> true;

    // Capture stderr to verify the no-prefix-match warning fires once per primary host. The
    // warning is the only breadcrumb the operator gets before the downstream http_file fetch
    // failure that comes from an empty repos set; suppressing it would silently re-create
    // the diagnostic gap this change closes.
    ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(capturedErr, true, UTF_8));

    Set<DependencyInfo> infos;
    try {
      LockFileConverter converter = new LockFileConverter(configuredRepos, json, alwaysTrue);
      infos = converter.getDependencies();
    } finally {
      System.setErr(originalErr);
    }

    assertEquals("expected exactly one dependency", 1, infos.size());
    DependencyInfo info = infos.iterator().next();
    assertEquals(
        "no configured repo matches the primary url; lockfile should record no repos",
        Set.of(),
        info.getRepositories());

    String stderr = capturedErr.toString(UTF_8);
    assertTrue(
        "expected no-prefix-match warning naming the resolved url; stderr=\n" + stderr,
        stderr.contains("does not prefix-match any configured --repo")
            && stderr.contains("orphan.example.com"));
  }

  /**
   * The orphan-URL warning must dedup per host. Two orphans sharing a host produce one log
   * line; an orphan from a distinct host produces a second. Symmetric to HeadProberTest's
   * authFailureLogsOncePerDistinctHost; the production code uses a per-instance
   * ConcurrentHashMap.newKeySet() and a regression that switched to a plain HashSet or
   * dropped the host key would surface here.
   */
  @Test
  public void orphanUrlWarningDedupsPerHost() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);

    // Two orphans on orphan.example.com + one on other.example.com. Expect 2 warnings.
    String coursierJson =
        "{\"dependencies\": ["
            + "  {\"coord\": \"com.example:orphan1:1.0.0\","
            + "   \"directDependencies\": [], \"dependencies\": [],"
            + "   \"url\": \"https://orphan.example.com/maven/com/example/orphan1/1.0.0/orphan1-1.0.0.jar\","
            + "   \"packages\": []},"
            + "  {\"coord\": \"com.example:orphan2:1.0.0\","
            + "   \"directDependencies\": [], \"dependencies\": [],"
            + "   \"url\": \"https://orphan.example.com/maven/com/example/orphan2/1.0.0/orphan2-1.0.0.jar\","
            + "   \"packages\": []},"
            + "  {\"coord\": \"com.example:orphan3:1.0.0\","
            + "   \"directDependencies\": [], \"dependencies\": [],"
            + "   \"url\": \"https://other.example.com/maven/com/example/orphan3/1.0.0/orphan3-1.0.0.jar\","
            + "   \"packages\": []}"
            + "]}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(capturedErr, true, UTF_8));
    try {
      new LockFileConverter(configuredRepos, json, uri -> false).getDependencies();
    } finally {
      System.setErr(originalErr);
    }

    String stderr = capturedErr.toString(UTF_8);
    long warningLines =
        stderr.lines().filter(l -> l.contains("does not prefix-match")).count();
    assertEquals(
        "two orphans from orphan.example.com + one from other.example.com should produce two"
            + " warnings; stderr=\n"
            + stderr,
        2,
        warningLines);
    assertTrue(
        "warning should mention orphan.example.com; stderr=\n" + stderr,
        stderr.contains("orphan.example.com"));
    assertTrue(
        "warning should mention other.example.com; stderr=\n" + stderr,
        stderr.contains("other.example.com"));
  }

  /**
   * If Coursier downloaded the artifact from m2local (the local Maven cache), the primary loop
   * must still skip m2local rather than recording it as a repo. The lockfile is a description
   * of remote sources; a local cache is not a remote source. The probe loop also must not call
   * the verifier with an m2local URI: a lying prober that returned true for m2local would
   * otherwise record it.
   */
  @Test
  public void primaryLoopSkipsM2LocalEvenWhenItPrefixMatches() throws IOException {
    URI m2localUri = Paths.get(USER_HOME.value()).resolve(".m2/repository").toUri();
    URI internalRepo = URI.create("https://internal.example.com/maven/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(m2localUri);
    configuredRepos.add(internalRepo);

    // Primary URL is under m2local; only the m2local-prefix would match.
    String primaryUrl =
        m2localUri.toString() + "com/example/m2local-primary/1.0.0/m2local-primary-1.0.0.jar";
    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:m2local-primary:1.0.0\","
            + "    \"directDependencies\": [],"
            + "    \"dependencies\": [],"
            + "    \"url\": \"" + primaryUrl + "\","
            + "    \"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    // Lying prober that says yes to everything; pairs with the primary-under-m2local setup so
    // the only thing that can keep m2local out of the recorded repos is the explicit skip.
    List<URI> probedUris = Collections.synchronizedList(new ArrayList<>());
    Predicate<URI> recordingProber =
        uri -> {
          probedUris.add(uri);
          return true;
        };

    LockFileConverter converter =
        new LockFileConverter(configuredRepos, json, recordingProber);
    Set<DependencyInfo> infos = converter.getDependencies();

    DependencyInfo info = infos.iterator().next();
    assertEquals(
        "m2local must never appear as a recorded repo, even when it's the primary's prefix",
        Set.of(),
        info.getRepositories());
    assertFalse(
        "m2local must not be probed by the mirror verifier; probed=" + probedUris,
        probedUris.stream().anyMatch(u -> u.toString().startsWith(m2localUri.toString())));
  }

  /**
   * Some Coursier output entries (parent POMs, BOMs) lack a {@code url} field. The converter
   * must not NPE and must record no repos for such entries.
   */
  @Test
  public void recordsNoReposWhenUrlFieldIsAbsent() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);

    // No "url" or "file" field -- typical for parent POMs that Coursier resolves but doesn't
    // download as a jar.
    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:parent-pom:1.0.0\","
            + "    \"directDependencies\": [],"
            + "    \"dependencies\": [],"
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    LockFileConverter converter =
        new LockFileConverter(configuredRepos, json, uri -> true);
    Set<DependencyInfo> infos = converter.getDependencies();

    assertEquals("expected exactly one dependency", 1, infos.size());
    DependencyInfo info = infos.iterator().next();
    assertEquals(
        "entry with no url field should record no repos",
        Set.of(),
        info.getRepositories());
  }

  /**
   * The primary repo (verified by Coursier's download) is always recorded, even if a lying
   * prober would return false for it. The probe loop must skip the primary's own URI rather
   * than re-asking the prober. Asserting that the primary URI is never passed to the prober
   * is what catches a skip-self regression: a lying prober would otherwise leave the primary
   * in `repos` only by coincidence (recorded via the primary loop), but the wasted probe
   * would have happened.
   */
  @Test
  public void primaryRepoRecordedEvenWhenProberReturnsFalseForEverything() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    URI mirrorRepo = URI.create("https://mirror.example.com/maven/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);
    configuredRepos.add(mirrorRepo);

    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:liar-test:1.0.0\","
            + "    \"directDependencies\": [],"
            + "    \"dependencies\": [],"
            + "    \"file\": \"v1/https/internal.example.com/maven/com/example/liar-test/1.0.0/liar-test-1.0.0.jar\","
            + "    \"url\": \"https://internal.example.com/maven/com/example/liar-test/1.0.0/liar-test-1.0.0.jar\","
            + "    \"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    List<URI> probedUris = Collections.synchronizedList(new ArrayList<>());
    Predicate<URI> recordingProber =
        uri -> {
          probedUris.add(uri);
          return false;
        };

    LockFileConverter converter =
        new LockFileConverter(configuredRepos, json, recordingProber);
    Set<DependencyInfo> infos = converter.getDependencies();

    DependencyInfo info = infos.iterator().next();
    assertEquals(
        "primary repo from Coursier's url field must always be recorded",
        Set.of(internalRepo),
        info.getRepositories());
    assertFalse(
        "primary URI must not be passed to the prober; probed=" + probedUris,
        probedUris.stream().anyMatch(u -> u.toString().startsWith(internalRepo.toString())));
  }

  /**
   * Output ordering must be deterministic (alphabetical by coordinate) regardless of submission
   * order. Guards against a refactor that swaps the result aggregation to something that
   * preserves submission order instead of sorting.
   */
  @Test
  public void multipleDependenciesAreReturnedInDeterministicOrder() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);

    // Submit in reverse-alphabetical order to make any accidental "preserves submission order"
    // bug visible.
    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + depEntry("com.example:zebra:1.0.0", internalRepo)
            + ","
            + depEntry("com.example:mango:1.0.0", internalRepo)
            + ","
            + depEntry("com.example:apple:1.0.0", internalRepo)
            + "]"
            + "}";

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    AtomicInteger probeCount = new AtomicInteger();
    Predicate<URI> countingProber =
        uri -> {
          probeCount.incrementAndGet();
          return false;
        };

    LockFileConverter converter =
        new LockFileConverter(configuredRepos, json, countingProber);
    Set<DependencyInfo> infos = converter.getDependencies();

    assertEquals(3, infos.size());
    Iterator<DependencyInfo> it = infos.iterator();
    assertEquals(new Coordinates("com.example:apple:1.0.0"), it.next().getCoordinates());
    assertEquals(new Coordinates("com.example:mango:1.0.0"), it.next().getCoordinates());
    assertEquals(new Coordinates("com.example:zebra:1.0.0"), it.next().getCoordinates());
    // Single configured repo, equal to the primary; probe loop should never invoke the verifier.
    assertEquals("verifier should not be invoked when only the primary repo is configured",
        0, probeCount.get());
  }

  /**
   * If the calling thread is interrupted while waiting on a worker, getDependencies must
   * re-throw as a RuntimeException carrying the partial-progress count and restore the
   * interrupt flag for any outer caller. Catches a regression where the gather-loop call is
   * changed to one that ignores interrupts.
   *
   * <p>Holds the prober on a latch so {@code f.get()} actually blocks; otherwise the future
   * may already be complete when the loop reaches it and {@code get()} returns the value
   * instead of throwing InterruptedException. Also logs queued-task count via
   * shutdownNow(), which this test additionally asserts.
   */
  @Test
  public void interruptedThreadAbortsWithProgressMessage() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    URI mirrorRepo = URI.create("https://mirror.example.com/maven/");
    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);
    configuredRepos.add(mirrorRepo);

    // Many deps so the queue is meaningfully populated when shutdownNow drains it.
    StringBuilder depsJson = new StringBuilder("{\"dependencies\": [");
    int depCount = 12;
    for (int i = 0; i < depCount; i++) {
      if (i > 0) depsJson.append(",");
      depsJson.append(depEntry("com.example:foo" + i + ":1.0.0", internalRepo));
    }
    depsJson.append("]}");
    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, depsJson.toString().getBytes(UTF_8));

    java.util.concurrent.CountDownLatch holdProber = new java.util.concurrent.CountDownLatch(1);
    Predicate<URI> blockingProber =
        uri -> {
          try {
            holdProber.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return false;
        };

    // Capture stderr to verify shutdownNow's drained-task log line fires.
    ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(capturedErr, true, UTF_8));

    // maxThreads=2 with 12 deps so 10 tasks sit on the queue when shutdownNow drains them.
    LockFileConverter converter =
        new LockFileConverter(configuredRepos, json, blockingProber, 2);

    Thread.currentThread().interrupt();
    try {
      converter.getDependencies();
      fail("expected RuntimeException from interrupt");
    } catch (RuntimeException e) {
      String msg = e.getMessage();
      assertTrue("message should mention interruption; got: " + msg,
          msg != null && msg.contains("interrupted"));
      assertTrue("message should include partial-progress count; got: " + msg,
          msg.contains(" of "));
      assertTrue("interrupt flag must be restored on the calling thread",
          Thread.currentThread().isInterrupted());
    } finally {
      // Release the held probers so they can drain.
      holdProber.countDown();
      Thread.interrupted();
      System.setErr(originalErr);
    }

    String stderr = capturedErr.toString(UTF_8);
    assertTrue(
        "shutdownNow drained-task diagnostic should fire on interrupted teardown; stderr="
            + stderr,
        stderr.contains("skipped") && stderr.contains("queued HEAD probes"));
  }

  /**
   * Constructs a Coursier entry whose {@code directDependencies} field is a string instead
   * of an array; the worker fails the unchecked cast on that field, and the converter must
   * surface the failing artifact's coord in the wrapped exception message so an operator
   * can locate the offending entry.
   */
  @Test
  public void workerExceptionPropagatesWithCoordContext() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);

    String coursierJson =
        "{"
            + "\"dependencies\": ["
            + "  {"
            + "    \"coord\": \"com.example:bad-deps:1.0.0\","
            + "    \"directDependencies\": \"not-a-list\","
            + "    \"dependencies\": [],"
            + "    \"file\": \"v1/https/internal.example.com/maven/com/example/bad-deps/1.0.0/bad-deps-1.0.0.jar\","
            + "    \"url\": \"https://internal.example.com/maven/com/example/bad-deps/1.0.0/bad-deps-1.0.0.jar\","
            + "    \"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
            + "    \"packages\": []"
            + "  }"
            + "]"
            + "}";
    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    LockFileConverter converter = new LockFileConverter(configuredRepos, json);
    try {
      converter.getDependencies();
      fail("expected RuntimeException from worker");
    } catch (RuntimeException e) {
      String msg = e.getMessage();
      assertTrue(
          "error message should contain the failing coord; got: " + msg,
          msg != null && msg.contains("com.example:bad-deps:1.0.0"));
    }
  }

  /**
   * The defensive copy in the constructor must isolate the converter's view of the configured
   * repositories from caller-side mutation. A regression that drops the copy (or silently
   * swaps to a structure that loses iteration order, e.g. Set.copyOf) would cause workers
   * iterating {@code repositories} to observe later mutations.
   */
  @Test
  public void constructorTakesDefensiveCopyOfRepositories() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    URI otherRepo = URI.create("https://other.example.com/maven/");

    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);

    String coursierJson =
        "{\"dependencies\": [" + depEntry("com.example:foo:1.0.0", internalRepo) + "]}";
    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    LockFileConverter converter = new LockFileConverter(configuredRepos, json);

    // Mutate the caller's reference AFTER construction.
    configuredRepos.clear();
    configuredRepos.add(otherRepo);

    DependencyInfo info = converter.getDependencies().iterator().next();
    assertEquals(
        "converter must use the snapshot at construction time, not the caller's mutated Set",
        Set.of(internalRepo),
        info.getRepositories());
  }

  /**
   * The constructor enforces {@code maxThreads >= 1} as an invariant of the canonical 4-arg
   * constructor. Zero and negative values must throw IAE, not be silently clamped.
   */
  @Test
  public void constructorRejectsZeroAndNegativeMaxThreads() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, "{\"dependencies\": []}".getBytes(UTF_8));

    for (int badValue : new int[] {0, -1, Integer.MIN_VALUE}) {
      try {
        new LockFileConverter(configuredRepos, json, uri -> false, badValue);
        fail("expected IllegalArgumentException for maxThreads=" + badValue);
      } catch (IllegalArgumentException e) {
        assertTrue(
            "message should mention maxThreads; got: " + e.getMessage(),
            e.getMessage() != null && e.getMessage().contains("maxThreads"));
      }
    }
  }

  /**
   * The constructor rejects a configured-repos Set containing a {@code null} entry rather
   * than letting the NPE surface deep inside {@code toDependencyInfo} as a confusing
   * "Failed to convert dependency X (url=Y): null" wrap.
   */
  @Test
  public void constructorRejectsNullRepositoryEntry() throws IOException {
    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(URI.create("https://internal.example.com/maven/"));
    configuredRepos.add(null);

    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, "{\"dependencies\": []}".getBytes(UTF_8));

    try {
      new LockFileConverter(configuredRepos, json);
      fail("expected IllegalArgumentException for null repository entry");
    } catch (IllegalArgumentException e) {
      assertTrue(
          "message should mention null; got: " + e.getMessage(),
          e.getMessage() != null && e.getMessage().toLowerCase().contains("null"));
    }
  }

  /**
   * runMain's CLI-error branches all funnel through {@link IllegalArgumentException}, which
   * main() catches for a clean stderr message + exit(1). Each branch contributes its own
   * regression: missing flag value, unknown flag, missing --json, invalid --max-threads,
   * malformed --repo URI.
   */
  @Test
  public void runMainThrowsIaeOnUnknownFlag() {
    try {
      LockFileConverter.runMain(new String[] {"--no-such-flag"});
      fail("expected IllegalArgumentException");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
      String msg = e.getMessage();
      assertTrue("message should name the bad flag and enumerate valid ones; got: " + msg,
          msg != null && msg.contains("--no-such-flag") && msg.contains("--json"));
    }
  }

  @Test
  public void runMainThrowsIaeOnMissingJsonFlag() {
    try {
      LockFileConverter.runMain(new String[] {"--repo", "https://example.com/"});
      fail("expected IllegalArgumentException");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
      assertTrue("message should mention --json; got: " + e.getMessage(),
          e.getMessage() != null && e.getMessage().contains("--json"));
    }
  }

  @Test
  public void runMainThrowsIaeOnFlagWithoutValue() {
    try {
      LockFileConverter.runMain(new String[] {"--json"});
      fail("expected IllegalArgumentException");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
      assertTrue("message should mention --json; got: " + e.getMessage(),
          e.getMessage() != null && e.getMessage().contains("--json"));
    }
  }

  @Test
  public void runMainThrowsIaeOnMalformedRepoUri() {
    try {
      LockFileConverter.runMain(
          new String[] {
              "--json", "/tmp/nonexistent.json",
              "--repo", "ht tp://has spaces/maven/"
          });
      fail("expected IllegalArgumentException");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
      assertTrue("message should attribute the error to --repo; got: " + e.getMessage(),
          e.getMessage() != null && e.getMessage().contains("--repo"));
    }
  }

  /**
   * A worker-thread {@link Error} (e.g. OutOfMemoryError) must propagate out of
   * getDependencies with its type preserved, NOT wrapped in a RuntimeException. JVM-level
   * crash handlers (e.g. -XX:+CrashOnOutOfMemoryError) rely on the Error type, and a
   * downgrade would silently disable them.
   */
  @Test
  public void workerErrorIsRethrownAsErrorNotWrappedInRuntimeException() throws IOException {
    URI internalRepo = URI.create("https://internal.example.com/maven/");
    URI mirrorRepo = URI.create("https://mirror.example.com/maven/");
    Set<URI> configuredRepos = new LinkedHashSet<>();
    configuredRepos.add(internalRepo);
    configuredRepos.add(mirrorRepo);

    String coursierJson =
        "{\"dependencies\": ["
            + depEntry("com.example:foo:1.0.0", internalRepo)
            + "]}";
    Path json = tempFolder.newFile("coursier_install.json").toPath();
    Files.write(json, coursierJson.getBytes(UTF_8));

    Predicate<URI> erroringProber =
        uri -> {
          throw new OutOfMemoryError("synthetic-not-real-oom");
        };

    LockFileConverter converter =
        new LockFileConverter(configuredRepos, json, erroringProber);

    try {
      converter.getDependencies();
      fail("expected OutOfMemoryError");
    } catch (OutOfMemoryError e) {
      assertEquals("synthetic-not-real-oom", e.getMessage());
    } catch (RuntimeException e) {
      fail("Error must not be wrapped in RuntimeException; got: " + e);
    }
  }

  private static String depEntry(String coord, URI repo) {
    String[] parts = coord.split(":");
    String groupPath = parts[0].replace(".", "/");
    String artifactId = parts[1];
    String version = parts[2];
    String path =
        groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    String url = repo.toString() + path;
    return "{"
        + "\"coord\": \"" + coord + "\","
        + "\"directDependencies\": [],"
        + "\"dependencies\": [],"
        + "\"file\": \"v1/https/" + url.substring("https://".length()) + "\","
        + "\"url\": \"" + url + "\","
        + "\"sha256\": \"0000000000000000000000000000000000000000000000000000000000000000\","
        + "\"packages\": []"
        + "}";
  }
}
