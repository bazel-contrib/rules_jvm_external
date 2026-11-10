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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Associates the canonical input snapshot with selected rows without inventing downloads. */
final class V4Requests {
  private V4Requests() {}

  static List<Map<String, Object>> attach(
      Collection<V4Artifact> artifacts, Collection<Map<String, Object>> requests) {
    Map<String, Map<String, Object>> unresolved = new TreeMap<>();
    Map<V4Artifact, Map<String, Map<String, Object>>> attached = new java.util.HashMap<>();
    for (Map<String, Object> request : requests) {
      V4Artifact selected = select(artifacts, request);
      if (selected == null) {
        unresolved.put(V4Json.compact(request), new TreeMap<>(request));
        continue;
      }
      Map<String, Object> compact = new TreeMap<>(request);
      compact.remove("group");
      compact.remove("artifact");
      if (selected.coordinates.getVersion().equals(compact.get("version"))) {
        compact.remove("version");
      }
      attached
          .computeIfAbsent(selected, unused -> new TreeMap<>())
          .put(V4Json.compact(compact), compact);
    }
    attached.forEach(
        (artifact, values) -> artifact.metadata.put("requests", new ArrayList<>(values.values())));
    return new ArrayList<>(unresolved.values());
  }

  private static V4Artifact select(Collection<V4Artifact> artifacts, Map<String, Object> request) {
    String classifier = (String) request.getOrDefault("classifier", "");
    if ("jar".equals(classifier)) {
      classifier = "";
    }
    List<V4Artifact> candidates = new ArrayList<>();
    for (V4Artifact artifact : artifacts) {
      Coordinates coords = artifact.coordinates;
      if (coords.getGroupId().equals(request.get("group"))
          && coords.getArtifactId().equals(request.get("artifact"))
          && coords.getClassifier().equals(classifier)
          && (!request.containsKey("packaging")
              || coords.getExtension().equals(request.get("packaging")))) {
        candidates.add(artifact);
      }
    }
    if (!request.containsKey("packaging")) {
      for (V4Artifact candidate : candidates) {
        if ("jar".equals(candidate.coordinates.getExtension())) {
          return candidate;
        }
      }
    }
    if (candidates.stream()
        .anyMatch(artifact -> !"pom".equals(artifact.coordinates.getExtension()))) {
      candidates.removeIf(artifact -> "pom".equals(artifact.coordinates.getExtension()));
    }
    if (candidates.size() > 1) {
      throw new IllegalArgumentException(
          "Ambiguous selected packaging for request " + V4Json.compact(request));
    }
    return candidates.isEmpty() ? null : candidates.get(0);
  }
}
