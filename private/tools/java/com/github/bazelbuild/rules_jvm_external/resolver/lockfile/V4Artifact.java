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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** One independently validated selected artefact. */
final class V4Artifact {
  final Coordinates coordinates;
  final String sha256;
  final Map<String, Object> metadata = new TreeMap<>();

  V4Artifact(Coordinates coordinates, String sha256) {
    this.coordinates = coordinates;
    this.sha256 = sha256;
  }

  String externalForm() {
    String value =
        coordinates.getGroupId()
            + ":"
            + coordinates.getArtifactId()
            + ":"
            + coordinates.getVersion();
    if (!coordinates.getClassifier().isEmpty()) {
      value += ":" + coordinates.getClassifier();
    }
    if (!"jar".equals(coordinates.getExtension())) {
      value += "@" + coordinates.getExtension();
    }
    return value;
  }

  List<Object> row(Collection<String> dependencies, boolean local, boolean skipped) {
    List<String> children = new ArrayList<>(new TreeSet<>(dependencies));
    String coordinate = externalForm();
    int hash =
        V4Json.compact(Arrays.asList(coordinate, sha256, children, metadata, local, skipped))
            .hashCode();
    List<Object> row = new ArrayList<>(Arrays.asList(coordinate, sha256, hash));
    if (!metadata.isEmpty()) {
      row.add(metadata);
    }
    return row;
  }

  static Coordinates fromKey(String key, String version) {
    String[] parts = key.split(":", -1);
    if (parts.length < 2 || parts.length > 4 || parts[0].isEmpty() || parts[1].isEmpty()) {
      throw new IllegalArgumentException("Invalid artifact identity " + key);
    }
    return new Coordinates(
        parts[0],
        parts[1],
        parts.length >= 3 ? parts[2] : "jar",
        parts.length == 4 ? parts[3] : "",
        version);
  }
}
