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

package com.github.bazelbuild.rules_jvm_external.resolver.bom;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standalone CLI tool that augments an existing v3 lock file with a {@code bom_resolution}
 * section.
 *
 * <p>Invoked from {@code pin.sh} (or {@code pin_dependencies.bzl} for the maven/gradle
 * resolver) after the lock file has been written. All BOMs, repositories, and versionless
 * artifacts are passed in on the command line because they are not stored in the lock file
 * itself.
 *
 * <p>Arguments may be passed directly or via an argsfile prefixed with {@code @}, where each
 * line of the file is treated as one argument.
 *
 * <p>On success: exit 0, lock file edited in place. On any resolution failure: exit non-zero
 * with a descriptive message; the lock file is not modified.
 */
public final class BomResolverMain {

  private BomResolverMain() {}

  public static void main(String[] args) {
    try {
      run(expandArgsFiles(args));
    } catch (UsageException e) {
      System.err.println("BomResolverMain: " + e.getMessage());
      System.exit(2);
    } catch (RuntimeException e) {
      System.err.println("BomResolverMain failed: " + e.getMessage());
      // Print causal chain for diagnosability without dumping a huge stack on success paths.
      Throwable cause = e.getCause();
      while (cause != null) {
        System.err.println("  caused by: " + cause.getMessage());
        cause = cause.getCause();
      }
      System.exit(1);
    }
  }

  static void run(List<String> args) {
    Path lockFile = null;
    Path netrc = null;
    List<String> boms = new ArrayList<>();
    List<URI> repositories = new ArrayList<>();
    List<Coordinates> versionlessArtifacts = new ArrayList<>();

    for (String arg : args) {
      if (arg.startsWith("--lock-file=")) {
        lockFile = Paths.get(value(arg));
      } else if (arg.startsWith("--netrc=")) {
        String v = value(arg);
        if (!v.isEmpty()) {
          netrc = Paths.get(v);
        }
      } else if (arg.startsWith("--boms=")) {
        boms.add(value(arg));
      } else if (arg.startsWith("--repositories=")) {
        repositories.add(URI.create(value(arg)));
      } else if (arg.startsWith("--artifacts=")) {
        versionlessArtifacts.add(new Coordinates(value(arg)));
      } else if (arg.isEmpty()) {
        // Skip blank lines in argsfile.
      } else {
        throw new UsageException("Unknown argument: " + arg);
      }
    }

    if (lockFile == null) {
      throw new UsageException("--lock-file is required");
    }

    if (boms.isEmpty() || versionlessArtifacts.isEmpty()) {
      // Nothing to do; ensure the lock file lacks any stale bom_resolution section.
      removeBomResolutionSection(lockFile);
      return;
    }

    Map<String, List<String>> mapping =
        BomResolver.buildBomResolutionMapping(repositories, boms, versionlessArtifacts, netrc);

    writeBomResolutionSection(lockFile, mapping);
  }

  private static String value(String arg) {
    int eq = arg.indexOf('=');
    return arg.substring(eq + 1);
  }

  private static void writeBomResolutionSection(
      Path lockFile, Map<String, List<String>> mapping) {
    JsonObject root = readLockFile(lockFile);

    if (mapping.isEmpty()) {
      root.remove("bom_resolution");
    } else {
      JsonObject section = new JsonObject();
      // Insert in declaration / iteration order, which is artifact-declaration order.
      for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
        JsonArray boms = new JsonArray();
        for (String bom : entry.getValue()) {
          boms.add(bom);
        }
        section.add(entry.getKey(), boms);
      }
      root.add("bom_resolution", section);
    }

    writeLockFile(lockFile, root);
  }

  private static void removeBomResolutionSection(Path lockFile) {
    JsonObject root = readLockFile(lockFile);
    if (!root.has("bom_resolution")) {
      return;
    }
    root.remove("bom_resolution");
    writeLockFile(lockFile, root);
  }

  private static JsonObject readLockFile(Path lockFile) {
    String content;
    try {
      content = new String(Files.readAllBytes(lockFile), UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    String trimmed = content.trim();
    if (trimmed.isEmpty() || "{}".equals(trimmed)) {
      // Empty / {} lock files are tolerated per Constraint #13 — treat as "no data".
      return new JsonObject();
    }
    JsonElement parsed = new Gson().fromJson(content, JsonElement.class);
    if (parsed == null || !parsed.isJsonObject()) {
      throw new RuntimeException(
          "Lock file " + lockFile + " is not a JSON object; cannot add bom_resolution section");
    }
    return parsed.getAsJsonObject();
  }

  private static void writeLockFile(Path lockFile, JsonObject root) {
    String rendered = new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(root);
    try {
      Files.write(lockFile, rendered.getBytes(UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // Visible for testing.
  static List<String> expandArgsFiles(String[] args) {
    List<String> expanded = new ArrayList<>();
    for (String arg : args) {
      if (arg.startsWith("@")) {
        Path argsFile = Paths.get(arg.substring(1));
        try (BufferedReader reader = Files.newBufferedReader(argsFile)) {
          String line;
          while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
              expanded.add(trimmed);
            }
          }
        } catch (IOException e) {
          throw new RuntimeException("Failed to read argsfile " + argsFile, e);
        }
      } else {
        expanded.add(arg);
      }
    }
    return expanded;
  }

  private static final class UsageException extends RuntimeException {
    UsageException(String message) {
      super(message);
    }
  }
}
