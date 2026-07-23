// Copyright 2025 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.artifacts.result.ResolvedComponentResult;

/**
 * Utility for detecting timestamped SNAPSHOT versions in Gradle dependencies.
 *
 * <p>Inspired by the Maven resolver's snapshot handling. See <a
 * href="https://github.com/apache/maven-resolver/blob/c9ee9e113f424ac41339ea25313ecceff946960b/maven-resolver-api/src/main/java/org/eclipse/aether/artifact/AbstractArtifact.java#L45">AbstractArtifact</a>.
 */
final class GradleSnapshotUtil {
  // Matches the toString() of a Gradle MavenUniqueSnapshotComponentIdentifier, capturing the
  // timestamp revision: group:name:<base>-SNAPSHOT:<yyyyMMdd.HHmmss-buildNumber>
  // See:
  // https://github.com/gradle/gradle/blob/1c1143f9b850f11cb6fef8f7b28405cb5ede45dc/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/repositories/resolver/MavenUniqueSnapshotComponentIdentifier.java#L55
  private static final Pattern GRADLE_SNAPSHOT_WITH_TIMESTAMP =
      Pattern.compile("^.*SNAPSHOT:([0-9]{8}\\.[0-9]{6}-[0-9]+)$");

  private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

  private GradleSnapshotUtil() {
    // Utility class - prevent instantiation
  }

  /**
   * Returns the resolved, timestamped version of a Gradle component when it is a timestamped
   * (Maven-unique) snapshot, or empty otherwise.
   *
   * <p>Example: for a component whose module version is {@code 999.0.0-HEAD-jre-SNAPSHOT} and whose
   * id is {@code com.google.guava:guava:999.0.0-HEAD-jre-SNAPSHOT:20250623.150948-114}, returns
   * {@code 999.0.0-HEAD-jre-20250623.150948-114}.
   */
  static Optional<String> timestampedSnapshotVersion(ResolvedComponentResult component) {
    Matcher matcher = GRADLE_SNAPSHOT_WITH_TIMESTAMP.matcher(component.getId().toString());
    if (!matcher.matches()) {
      return Optional.empty();
    }

    String timestamp = matcher.group(1);
    String version = component.getModuleVersion().getVersion();
    String baseVersion =
        version.endsWith(SNAPSHOT_SUFFIX)
            ? version.substring(0, version.length() - SNAPSHOT_SUFFIX.length())
            : version;
    return Optional.of(baseVersion + "-" + timestamp);
  }
}
