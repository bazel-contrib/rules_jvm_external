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

package com.github.bazelbuild.rules_jvm_external.resolver;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Utility class for calculating artifact hashes for lock files. */
public final class ArtifactsHash {

  public static final int NO_HASH = -1;
  private static final String SEPARATOR = " | ";

  private ArtifactsHash() {
    // utility class
  }

  public static int calculateArtifactsHash(Collection<DependencyInfo> infos) {
    // Note: this function is exactly equivalent to the one in `v2_lock_file.bzl`
    //       if you make a change there please make it here, and vice versa.
    Set<String> lines = new TreeSet<>();

    for (DependencyInfo info : infos) {
      StringBuilder line = new StringBuilder();
      line.append(info.getCoordinates().toString())
          .append(SEPARATOR)
          .append(info.getSha256().orElseGet(() -> ""))
          .append(SEPARATOR);

      line.append(
          info.getDependencies().stream()
              .map(Coordinates::asKey)
              .sorted()
              .collect(Collectors.joining(",")));

      lines.add(line.toString());
    }

    return String.join("\n", lines).hashCode();
  }
}
