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

import java.util.regex.Pattern;
import org.gradle.api.artifacts.result.ResolvedComponentResult;

/** Utility class for SNAPSHOT version detection in gradle dependencies. 
 * 
 * This class is inspired by the implementation details in the maven resolver
 * See: https://github.com/apache/maven-resolver/blob/c9ee9e113f424ac41339ea25313ecceff946960b/maven-resolver-api/src/main/java/org/eclipse/aether/artifact/AbstractArtifact.java#L45 
 */
final class GradleSnapshotUtil {
  /* A regex to identify when a gradle ModuleComponentidentifer is a versioned snapshot (with timestamp)
   *
   * See: https://github.com/gradle/gradle/blob/1c1143f9b850f11cb6fef8f7b28405cb5ede45dc/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/repositories/resolver/MavenUniqueSnapshotComponentIdentifier.java#L55
   */
  private static final Pattern GRADLE_SNAPSHOT_WITH_TIMESTAMP = Pattern.compile("^.*SNAPSHOT:([0-9]{8}\\.[0-9]{6}-[0-9]+)$");

  private GradleSnapshotUtil() {
    // Utility class - prevent instantiation
  }

  /**
   * Determines if a component represents a versioned SNAPSHOT dependency
   * that follows the gradle identifier convention. 
   * Example: com.google.guava:guava:999.0.0-HEAD-jre-SNAPSHOT:20250623.150948-114
   */
  static boolean isVersionedSnapshot(ResolvedComponentResult component) {
    return GRADLE_SNAPSHOT_WITH_TIMESTAMP.matcher(component.getId().toString()).matches();
  }

  /**
   * Extracts the timestamped version from a gradle versioned snapshot component.
   * Example: for a component with version "999.0.0-HEAD-jre-SNAPSHOT" and identifier 
   * "com.google.guava:guava:999.0.0-HEAD-jre-SNAPSHOT:20250623.150948-114" 
   * returns "999.0.0-HEAD-jre-20250623.150948-114"
   * See: https://github.com/gradle/gradle/blob/1c1143f9b850f11cb6fef8f7b28405cb5ede45dc/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/repositories/resolver/MavenUniqueSnapshotComponentIdentifier.java#L55
   */
  static String extractSnapshotId(ResolvedComponentResult component) {
    String version = component.getModuleVersion().getVersion();
    String baseVersion = version.substring(0, version.indexOf("-SNAPSHOT"));
    String timestamp = component.getId().toString().split(":")[3];
    return baseVersion + "-" + timestamp;
  }
}
