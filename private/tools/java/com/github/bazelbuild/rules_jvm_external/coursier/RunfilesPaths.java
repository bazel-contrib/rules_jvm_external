package com.github.bazelbuild.rules_jvm_external.coursier;

import com.google.devtools.build.runfiles.Runfiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

final class RunfilesPaths {
  private RunfilesPaths() {}

  static Path resolvePath(String path) throws IOException {
    Path directPath = Paths.get(path);
    if (Files.exists(directPath)) {
      return directPath;
    }

    Runfiles.Preloaded runfiles = Runfiles.preload();
    for (String runfilePath : runfileCandidates(path)) {
      if (runfilePath == null || runfilePath.isEmpty()) {
        continue;
      }

      String resolvedPathString;
      try {
        resolvedPathString = runfiles.unmapped().rlocation(runfilePath);
      } catch (IllegalArgumentException e) {
        continue;
      }

      if (resolvedPathString != null) {
        Path resolvedPath = Paths.get(resolvedPathString);
        if (Files.exists(resolvedPath)) {
          return resolvedPath;
        }
      }
    }

    return directPath;
  }

  private static Set<String> runfileCandidates(String path) {
    Set<String> candidates = new LinkedHashSet<>();
    candidates.add(path);

    String normalized = path;
    while (normalized.startsWith("./")) {
      normalized = normalized.substring(2);
    }
    candidates.add(normalized);

    if (normalized.startsWith("external/")) {
      normalized = normalized.substring("external/".length());
      candidates.add(normalized);
    }

    while (normalized.startsWith("../")) {
      normalized = normalized.substring(3);
      candidates.add(normalized);
    }

    return candidates;
  }
}
