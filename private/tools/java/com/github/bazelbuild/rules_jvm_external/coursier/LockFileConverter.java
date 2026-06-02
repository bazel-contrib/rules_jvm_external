// Copyright 2024 The Bazel Authors. All rights reserved.
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

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.Conflict;
import com.github.bazelbuild.rules_jvm_external.resolver.DependencyInfo;
import com.github.bazelbuild.rules_jvm_external.resolver.lockfile.V3LockFile;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Reads the output of a Coursier resolve and produces a v3 lock file. */
public class LockFileConverter {

  private static final URI M2_LOCAL =
      Paths.get(USER_HOME.value()).resolve(".m2/repository").toUri();

  // HEAD probes are I/O-bound: thousands of cheap RTTs against the same few hosts. The
  // per-artifact loop dominates conversion time on large lock files, so size the pool well
  // above CPU count. Cap at 32 to avoid exhausting connection limits on shared CI mirrors and
  // to keep memory bounded; floor at 8 so small machines still benefit. Override via
  // RJE_MAX_THREADS (env) or --max-threads (CLI).
  private static final int DEFAULT_MAX_THREADS =
      Math.max(8, Math.min(32, Runtime.getRuntime().availableProcessors() * 4));

  private final Set<URI> repositories;
  private final Path unsortedJson;
  private final Predicate<URI> mirrorVerifier;
  private final int maxThreads;
  private final Set<String> warnedUnmatchedPrimaryHosts = ConcurrentHashMap.newKeySet();

  private static String requireValue(String[] args, int index, String flag) {
    if (index >= args.length) {
      throw new IllegalArgumentException("Missing value for " + flag);
    }
    return args[index];
  }

  private static int parsePositiveInt(String value, String source) {
    int parsed;
    try {
      parsed = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          source + " must be a positive integer, got '" + value + "'", e);
    }
    if (parsed < 1) {
      throw new IllegalArgumentException(source + " must be >= 1, got " + parsed);
    }
    return parsed;
  }

  public static void main(String[] args) throws Exception {
    try {
      runMain(args);
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  static void runMain(String[] args) throws Exception {
    Path unsortedJson = null;
    Path output = null;
    int maxThreads = DEFAULT_MAX_THREADS;
    String envMaxThreads = System.getenv("RJE_MAX_THREADS");
    if (envMaxThreads != null && !envMaxThreads.isEmpty()) {
      maxThreads = parsePositiveInt(envMaxThreads, "RJE_MAX_THREADS");
    }

    // Insertion order matters
    Set<URI> repositories = new LinkedHashSet<>();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--json":
          unsortedJson = Paths.get(requireValue(args, ++i, "--json"));
          break;

        case "--output":
          output = Paths.get(requireValue(args, ++i, "--output"));
          break;

        case "--max-threads":
          maxThreads =
              parsePositiveInt(requireValue(args, ++i, "--max-threads"), "--max-threads");
          break;

        case "--repo":
          String uri = requireValue(args, ++i, "--repo");
          if ("m2local".equals(uri) || "m2Local".equals(uri)) {
            repositories.add(M2_LOCAL);
          } else {
            try {
              repositories.add(URI.create(uri));
            } catch (IllegalArgumentException e) {
              // URI.create wraps URISyntaxException as its own IAE cause; chain the root so
              // the resulting Throwable chain is two hops, not three.
              Throwable root = e.getCause() != null ? e.getCause() : e;
              throw new IllegalArgumentException(
                  "Invalid --repo value '" + uri + "': " + e.getMessage(), root);
            }
          }
          break;

        default:
          throw new IllegalArgumentException(
              "Unknown command line option: " + args[i]
                  + ". Valid options: --json, --output, --max-threads, --repo.");
      }
    }

    if (unsortedJson == null) {
      throw new IllegalArgumentException(
          "Path to coursier-generated lock file is required. Add using the --json flag.");
    }

    HeadProber prober = new HeadProber(Netrc.fromUserHome());
    LockFileConverter converter =
        new LockFileConverter(repositories, unsortedJson, prober, maxThreads);
    Set<DependencyInfo> infos = converter.getDependencies();
    Set<Conflict> conflicts = converter.getConflicts();

    Map<String, Object> rendered = new V3LockFile(repositories, infos, conflicts, true).render();

    String converted =
        new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(rendered);

    if (output == null) {
      System.out.println(converted);
    } else {
      try {
        Files.write(output, converted.getBytes(UTF_8));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  public LockFileConverter(Set<URI> repositories, Path unsortedJson) {
    this(repositories, unsortedJson, null, DEFAULT_MAX_THREADS);
  }

  @VisibleForTesting
  LockFileConverter(
      Set<URI> repositories, Path unsortedJson, Predicate<URI> mirrorVerifier) {
    this(repositories, unsortedJson, mirrorVerifier, DEFAULT_MAX_THREADS);
  }

  /**
   * @param repositories configured remote repositories. Iteration order is preserved and
   *     load-bearing -- the primary-repo prefix match downstream walks this order. Callers
   *     should pass a Set with deterministic iteration order such as a {@link LinkedHashSet}.
   * @param mirrorVerifier optional predicate used to verify whether a non-primary mirror has the
   *     artifact. When {@code null}, only the repo reported in the Coursier dep's {@code url} field
   *     is recorded. When non-null, the converter probes each other configured repo at the
   *     artifact's path and records every repo for which the predicate returns {@code true}.
   * @param maxThreads upper bound on the per-artifact parallelism used for HEAD probes.
   * @throws IllegalArgumentException if {@code maxThreads < 1} or if {@code repositories}
   *     contains a {@code null} entry.
   */
  @VisibleForTesting
  LockFileConverter(
      Set<URI> repositories,
      Path unsortedJson,
      Predicate<URI> mirrorVerifier,
      int maxThreads) {
    // Defensive copy preserving iteration order. Callers that retain the original Set could
    // otherwise mutate it under the worker threads' feet during getDependencies(), and the
    // primary-repo prefix match downstream relies on the configured order. Set.copyOf would
    // not preserve LinkedHashSet order.
    LinkedHashSet<URI> copy =
        new LinkedHashSet<>(Objects.requireNonNull(repositories, "repositories"));
    if (copy.contains(null)) {
      throw new IllegalArgumentException("repositories must not contain null");
    }
    this.repositories = Collections.unmodifiableSet(copy);
    this.unsortedJson = Objects.requireNonNull(unsortedJson, "unsortedJson");
    this.mirrorVerifier = mirrorVerifier;
    if (maxThreads < 1) {
      throw new IllegalArgumentException("maxThreads must be >= 1, got " + maxThreads);
    }
    this.maxThreads = maxThreads;
  }

  private Set<Conflict> getConflicts() {
    Map<String, Object> depTree = readDepTree();

    @SuppressWarnings("unchecked")
    Map<String, Object> rawConflicts =
        (Map<String, Object>) depTree.getOrDefault("conflict_resolution", Collections.EMPTY_MAP);

    HashSet<Conflict> conflicts = new HashSet<>();
    for (Map.Entry<String, Object> entry : rawConflicts.entrySet()) {
      Coordinates resolved = new Coordinates((String) entry.getValue());
      Coordinates requested = new Coordinates(entry.getKey());
      conflicts.add(new Conflict(resolved, requested));
    }

    return Set.copyOf(conflicts);
  }

  public Set<DependencyInfo> getDependencies() {
    Map<String, Object> depTree = readDepTree();
    Map<Coordinates, Coordinates> mappings = deriveCoordinateMappings(depTree);

    @SuppressWarnings("unchecked")
    Collection<Map<String, Object>> coursierDeps =
        (Collection<Map<String, Object>>) depTree.get("dependencies");
    if (coursierDeps == null) {
      throw new IllegalArgumentException(
          "Coursier lockfile " + unsortedJson + " is missing the 'dependencies' field.");
    }

    // HEAD-probing each non-primary configured repo against every artifact is the slow part.
    // Per-artifact work is independent, so run it on a fixed pool sized by maxThreads.
    int parallelism = Math.max(1, Math.min(maxThreads, coursierDeps.size()));
    ExecutorService executor =
        Executors.newFixedThreadPool(
            parallelism,
            r -> {
              Thread t = new Thread(r, "lockfile-converter");
              t.setDaemon(true);
              return t;
            });

    List<CompletableFuture<DependencyInfo>> futures =
        coursierDeps.stream()
            .map(dep -> CompletableFuture.supplyAsync(() -> toDependencyInfo(dep, mappings), executor))
            .collect(Collectors.toList());
    try {
      Set<DependencyInfo> toReturn =
          new TreeSet<>(Comparator.comparing(DependencyInfo::getCoordinates));
      for (CompletableFuture<DependencyInfo> f : futures) {
        try {
          // f.get() responds to thread interrupt; f.join() would not. If the gather loop is
          // interrupted mid-way, in-flight HEAD probes continue on daemon worker threads but
          // do not block JVM exit.
          toReturn.add(f.get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(
              "Lockfile conversion interrupted: " + toReturn.size() + " of " + futures.size()
                  + " artifacts completed",
              e);
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          }
          // Rethrow Error directly so OutOfMemoryError, StackOverflowError, etc. keep their
          // type for the uncaught-exception handler and for any caller that catches Error
          // specifically. Wrapping them in RuntimeException would hide them from
          // try/catch(Error) patterns. Only checked Throwables fall through to the
          // RuntimeException wrap.
          if (cause instanceof Error) {
            throw (Error) cause;
          }
          throw new RuntimeException(cause);
        }
      }
      return toReturn;
    } finally {
      // shutdownNow() drains queued tasks and interrupts active workers, but HttpURLConnection
      // reads are not interruptible -- in-flight probes run to their configured connect/read
      // timeout. Daemon threads let main() return and the JVM exit while those finish.
      List<Runnable> drained = executor.shutdownNow();
      if (!drained.isEmpty()) {
        System.err.println(
            "LockFileConverter: skipped " + drained.size()
                + " queued HEAD probes; any in-flight probes run to their configured timeout.");
      }
    }
  }

  private DependencyInfo toDependencyInfo(
      Map<String, Object> coursierDep, Map<Coordinates, Coordinates> mappings) {
    Objects.requireNonNull(coursierDep, "coursierDep");
    String coord = null;
    String primaryUrl = null;
    try {
      // Cast inside try so a non-String coord becomes part of the wrapped error context.
      coord = (String) coursierDep.get("coord");
      if (coord == null) {
        throw new IllegalArgumentException(
            "Coursier dependency entry is missing required 'coord' key: " + coursierDep);
      }
      primaryUrl = (String) coursierDep.get("url");
      return toDependencyInfoUnchecked(coursierDep, mappings, coord);
    } catch (RuntimeException e) {
      String coordLabel = coord != null ? coord : "<unknown>";
      String urlContext = primaryUrl == null ? "" : " (url=" + primaryUrl + ")";
      throw new RuntimeException(
          "Failed to convert dependency " + coordLabel + urlContext + ": " + e.getMessage(), e);
    }
  }

  private DependencyInfo toDependencyInfoUnchecked(
      Map<String, Object> coursierDep, Map<Coordinates, Coordinates> mappings, String coord) {
    Coordinates coords = mappings.get(new Coordinates(coord));

    Set<URI> repos = new LinkedHashSet<>();
    String primaryUrl = (String) coursierDep.get("url");

    // The lock file contains a "mirror_urls" array alongside "url"; we deliberately ignore
    // it. It is synthesised in coursier.bzl by interpolating the artifact path into every
    // configured repository regardless of whether the artifact lives there, so trusting it
    // would record every artifact as living in every configured repo. Only "url" reflects
    // where Coursier actually resolved the artifact from.
    URI primaryRepo = null;
    if (primaryUrl != null) {
      for (URI repo : repositories) {
        if (M2_LOCAL.equals(repo)) {
          continue;
        }
        if (primaryUrl.startsWith(repo.toString())) {
          primaryRepo = repo;
          repos.add(repo);
          break;
        }
      }
    }

    if (primaryUrl != null && primaryRepo == null) {
      // The lockfile will record this artifact with an empty repository list. http_file will
      // then fail at fetch time with no breadcrumb back to here. Most common cause: a --repo
      // entry has a trailing-slash or path drift versus what Coursier actually resolved.
      String primaryHost = null;
      try {
        primaryHost = URI.create(primaryUrl).getHost();
      } catch (IllegalArgumentException ignored) {
        // primaryUrl is not a valid URI; fall through to using it directly as the dedup key.
      }
      // Fall back to the raw URL when getHost() returns null (file: and opaque URIs), so the
      // warning still fires and dedups instead of being silently dropped.
      String dedupKey = primaryHost != null ? primaryHost : primaryUrl;
      if (warnedUnmatchedPrimaryHosts.add(dedupKey)) {
        System.err.println(
            "WARNING: Coursier-resolved url "
                + primaryUrl
                + " does not prefix-match any configured --repo. This artifact will be"
                + " recorded with an empty repository list and the build will fail at fetch"
                + " time. Check --repo entries for trailing-slash or path drift. Further"
                + " warnings for "
                + dedupKey
                + " are suppressed.");
      }
    }

    if (mirrorVerifier != null && primaryRepo != null) {
      String artifactPath = primaryUrl.substring(primaryRepo.toString().length());
      // Asking the prober about the primary would waste an RTT and risk a buggy prober demoting
      // an already-verified primary; m2local is never a mirror.
      for (URI repo : repositories) {
        if (repo.equals(primaryRepo) || M2_LOCAL.equals(repo)) {
          continue;
        }
        URI probe = URI.create(repo.toString() + artifactPath);
        if (mirrorVerifier.test(probe)) {
          repos.add(repo);
        }
      }
    }

    String file = (String) coursierDep.get("file");

    String classifier = coords.getClassifier();
    if (classifier == null || classifier.isEmpty()) {
      classifier = "jar";
    }

    Set<Coordinates> directDeps = new TreeSet<>();
    if (!"sources".equals(classifier) && !"javadoc".equals(classifier)) {
      @SuppressWarnings("unchecked")
      Collection<String> depCoords =
          (Collection<String>) coursierDep.getOrDefault("directDependencies", new TreeSet<>());
      directDeps =
          depCoords.stream()
              .map(Coordinates::new)
              .map(c -> mappings.getOrDefault(c, c))
              .collect(Collectors.toCollection(TreeSet::new));
    }

    Object rawPackages = coursierDep.get("packages");
    Set<String> packages = new TreeSet<>();
    if (rawPackages != null) {
      @SuppressWarnings("unchecked")
      Collection<String> depPackages = (Collection<String>) rawPackages;
      packages = new TreeSet<>(depPackages);
    }

    SortedMap<String, SortedSet<String>> services = new TreeMap<>();
    Object rawServices = coursierDep.get("services");
    if (rawServices != null) {
      @SuppressWarnings("unchecked")
      Map<String, SortedSet<String>> rawServicesMap = (Map<String, SortedSet<String>>) rawServices;
      services = new TreeMap<>(rawServicesMap);
    }

    return new DependencyInfo(
        coords,
        repos,
        Optional.ofNullable(file).map(Paths::get),
        Optional.ofNullable((String) coursierDep.get("sha256")),
        directDeps,
        packages,
        Set.of(),
        services);
  }

  private Map<String, Object> readDepTree() {
    try (Reader reader = Files.newBufferedReader(unsortedJson)) {
      return new Gson().fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Provide mappings from coordinates that are incorrect in the original lock file.
   *
   * <p>It turns out that coursier will sometimes claim that a regular set of coordinates is, in
   * fact, for a different extension (typically `aar`). The v2 lock file format relies on the
   * coordinates to determine the path to the artifact, so this kind of nonsense <i>will not
   * stand</i>. Go through all the coordinates in the file and make sure that the coordinate matches
   * the output path. If it doesn't, work out the correct coordinate and provide a mapping.
   *
   * @return a mapping of {@link Coordinates} from the dep tree to the correct {
   * @link Coordinates}.
   */
  private Map<Coordinates, Coordinates> deriveCoordinateMappings(Map<String, Object> depTree) {
    Map<Coordinates, Coordinates> toReturn = new HashMap<>();

    @SuppressWarnings("unchecked")
    Collection<Map<String, Object>> coursierDeps =
        (Collection<Map<String, Object>>) depTree.get("dependencies");
    if (coursierDeps == null) {
      throw new IllegalArgumentException(
          "Coursier lockfile " + unsortedJson + " is missing the 'dependencies' field.");
    }
    for (Map<String, Object> coursierDep : coursierDeps) {
      Coordinates coord = new Coordinates((String) coursierDep.get("coord"));
      String expectedPath = coord.toRepoPath();
      String file = (String) coursierDep.get("file");

      if (file == null) {
        toReturn.put(coord, coord);
        continue;
      }

      // Files may be URL encoded. Decode
      file = URLDecoder.decode(file, UTF_8);

      if (file.endsWith(expectedPath)) {
        toReturn.put(coord, coord);
        continue;
      }

      // The path of the output does not match the expected path. Attempt to rewrite.
      // Assume that the group and artifact IDs are correct, otherwise, we have real
      // problems.

      // The expected path looks something like:
      // "[group]/[artifact]/[version]/[artifact]-[version](-[classifier])(.[extension])"
      String prefix = coord.getGroupId().replace(".", "/") + "/" + coord.getArtifactId() + "/";

      int index = file.indexOf(prefix);
      if (index == -1) {
        throw new IllegalArgumentException(
            String.format(
                "Cannot determine actual coordinates for %s. Current coordinates are %s",
                file, coord));
      }
      String pathSubstring = file.substring(index + prefix.length());

      // The next part of the string should be the version number
      index = pathSubstring.indexOf("/");
      if (index == -1) {
        throw new IllegalArgumentException(
            String.format(
                "Cannot determine version number from %s. Current coordinates are %s",
                file, coord));
      }
      String version = pathSubstring.substring(0, index);

      // After the version, there should be nothing left but a file name
      pathSubstring = pathSubstring.substring(version.length() + 1);

      // Now we know the version, we can calculate the expected file name. For now, ignore
      // the fact that there may be a classifier. We're going to derive that if necessary.
      String expectedFileName = coord.getArtifactId() + "-" + version;

      index = pathSubstring.indexOf(expectedFileName);
      if (index == -1) {
        throw new IllegalArgumentException(
            String.format(
                "Expected file name (%s) not found in path (%s). Current coordinates are %s",
                expectedFileName, file, coord));
      }

      String classifier = "";
      String extension = "";
      String remainder = pathSubstring.substring(expectedFileName.length());

      if (remainder.isEmpty()) {
        throw new IllegalArgumentException(
            String.format(
                "File does not appear to have a suffix. %s. Current coordinates are %s",
                file, coord));
      }

      if (remainder.charAt(0) == '-') {
        // We have a classifier
        index = remainder.lastIndexOf('.');
        if (index == -1) {
          throw new IllegalArgumentException(
              String.format(
                  "File does not appear to have a suffix. %s. Current coordinates are %s",
                  file, coord));
        }
        classifier = remainder.substring(1, index);
        extension = remainder.substring(index + 1);
      } else if (remainder.charAt(0) == '.') {
        // We have an extension
        extension = remainder.substring(1);
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Unable to determine classifier and extension from %s. Current coordinates are %s",
                file, coord));
      }

      toReturn.put(
          coord,
          new Coordinates(
              coord.getGroupId(), coord.getArtifactId(), extension, classifier, version));
    }

    return toReturn;
  }
}
