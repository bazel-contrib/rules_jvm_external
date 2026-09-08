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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.junit.Test;

public class GradleSnapshotUtilTest {

  private static ComponentIdentifier identifier(String toStringValue) {
    return new ComponentIdentifier() {
      @Override
      public String getDisplayName() {
        return toStringValue;
      }

      @Override
      public String toString() {
        return toStringValue;
      }
    };
  }

  @Test
  public void timestampedSnapshotVersion_resolvesTimestampedVersionFromIdentifierToString() {
    ComponentIdentifier id =
        identifier("com.google.guava:guava:999.0.0-HEAD-jre-SNAPSHOT:20250828.195817-455");

    Optional<String> resolved =
        GradleSnapshotUtil.timestampedSnapshotVersion(id, "999.0.0-HEAD-jre-SNAPSHOT");

    assertTrue(resolved.isPresent());
    assertEquals("999.0.0-HEAD-jre-20250828.195817-455", resolved.get());
  }

  @Test
  public void timestampedSnapshotVersion_emptyForNonTimestampedSnapshot() {
    ComponentIdentifier id = identifier("com.example:lib:1.0-SNAPSHOT");

    Optional<String> resolved = GradleSnapshotUtil.timestampedSnapshotVersion(id, "1.0-SNAPSHOT");

    assertFalse(resolved.isPresent());
  }

  @Test
  public void timestampedSnapshotVersion_emptyForReleaseVersion() {
    ComponentIdentifier id = identifier("com.example:lib:1.0");

    Optional<String> resolved = GradleSnapshotUtil.timestampedSnapshotVersion(id, "1.0");

    assertFalse(resolved.isPresent());
  }

  @Test
  public void timestampedSnapshotVersion_usesVersionAsBaseWhenNotSuffixedWithSnapshot() {
    ComponentIdentifier id =
        identifier("com.example:lib:1.0-SNAPSHOT:20250623.150948-114");

    Optional<String> resolved = GradleSnapshotUtil.timestampedSnapshotVersion(id, "1.0");

    assertTrue(resolved.isPresent());
    assertEquals("1.0-20250623.150948-114", resolved.get());
  }
}
