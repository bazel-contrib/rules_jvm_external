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

package com.github.bazelbuild.rules_jvm_external.resolver.lockfile;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Expands and validates the selected v3 graph without including repository hints in hashes. */
final class V4Artifacts {
  final Map<String, V4Artifact> byIdentity = new TreeMap<>();
  final Map<String, List<String>> dependencies = new TreeMap<>();
  final Map<String, List<String>> repositories = new LinkedHashMap<>();
  final Set<String> local = new TreeSet<>();
  final Set<String> skipped = new TreeSet<>();

  V4Artifacts(Map<String, Object> v3) {
    Map<String, Map<String, Object>> roots = V4Json.map(v3.get("artifacts"));
    roots.forEach(
        (key, data) -> {
          if (key.split(":", -1).length > 3) {
            throw new IllegalArgumentException("Invalid v3 artifact root " + key);
          }
          String version = (String) data.get("version");
          if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Missing selected version for " + key);
          }
          Coordinates base = V4Artifact.fromKey(key, version);
          Map<String, String> shasums = V4Json.map(data.get("shasums"));
          shasums.forEach(
              (classifier, sha) -> add(new V4Artifact(base.setClassifier(classifier), sha)));
        });
    for (String key : V4Json.<String>items(v3.get("skipped"))) {
      skipped.add(identity(key));
    }
    recoverSkipped();
    attachVariants();
    Map<String, Collection<String>> rawDependencies = V4Json.map(v3.get("dependencies"));
    rawDependencies.forEach(
        (parent, children) -> {
          String key = requireIdentity(parent);
          Set<String> merged = new TreeSet<>(dependencies.getOrDefault(key, List.of()));
          children.forEach(child -> merged.add(requireIdentity(child)));
          dependencies.put(key, new ArrayList<>(merged));
        });
    Map<String, Collection<String>> rawRepositories = V4Json.map(v3.get("repositories"));
    rawRepositories.forEach(
        (url, members) -> {
          Set<String> canonical = new TreeSet<>();
          members.forEach(key -> canonical.add(identity(key)));
          repositories.put(url, new ArrayList<>(canonical));
        });
    if (Boolean.TRUE.equals(v3.get("m2local"))) {
      local.addAll(byIdentity.keySet());
      local.removeAll(skipped);
      repositories.values().forEach(local::removeAll);
    }
  }

  private void add(V4Artifact artifact) {
    String identity = artifact.coordinates.asKey();
    if (artifact.sha256 != null && !artifact.sha256.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Invalid SHA-256 for " + identity);
    }
    if (byIdentity.putIfAbsent(identity, artifact) != null) {
      throw new IllegalArgumentException("Duplicate artifact identity " + identity);
    }
  }

  private void attachVariants() {
    Map<String, List<V4Artifact>> groups = new TreeMap<>();
    for (V4Artifact artifact : byIdentity.values()) {
      Coordinates coords = artifact.coordinates;
      groups
          .computeIfAbsent(
              coords.getGroupId() + ":" + coords.getArtifactId(), unused -> new ArrayList<>())
          .add(artifact);
    }
    for (List<V4Artifact> group : groups.values()) {
      group.sort(Comparator.comparing(V4Artifact::externalForm));
      Set<String> variants = new TreeSet<>();
      V4Artifact anchor = null;
      for (V4Artifact artifact : group) {
        Coordinates coords = artifact.coordinates;
        if ("sources".equals(coords.getClassifier()) || "javadoc".equals(coords.getClassifier())) {
          variants.add(coords.asKey());
        } else if (anchor == null
            || (coords.getClassifier().isEmpty() && "jar".equals(coords.getExtension()))) {
          anchor = artifact;
        }
      }
      if (anchor != null && !variants.isEmpty()) {
        anchor.metadata.put("variants", new ArrayList<>(variants));
      }
    }
  }

  private void recoverSkipped() {
    for (String key : skipped) {
      if (byIdentity.containsKey(key)) {
        continue;
      }
      Coordinates missing = V4Artifact.fromKey(key, "");
      Set<String> versions = new TreeSet<>();
      for (V4Artifact candidate : byIdentity.values()) {
        Coordinates coords = candidate.coordinates;
        if (coords.getGroupId().equals(missing.getGroupId())
            && coords.getArtifactId().equals(missing.getArtifactId())
            && coords.getExtension().equals(missing.getExtension())) {
          versions.add(coords.getVersion());
        }
      }
      if (versions.size() != 1) {
        throw new IllegalArgumentException(
            "Cannot recover unambiguous version for skipped artifact " + key);
      }
      add(new V4Artifact(missing.setVersion(versions.iterator().next()), null));
    }
  }

  private String requireIdentity(String key) {
    String identity = identity(key);
    if (!byIdentity.containsKey(identity)) {
      throw new IllegalArgumentException("Missing artifact row for dependency identity " + key);
    }
    return identity;
  }

  private static String identity(String key) {
    return V4Artifact.fromKey(key, "").asKey();
  }
}
