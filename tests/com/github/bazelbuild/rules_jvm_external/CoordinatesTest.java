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

package com.github.bazelbuild.rules_jvm_external;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CoordinatesTest {

  @Test
  public void coordinatesAreConsistentWhenDefaultValuesAreUsed() {
    Coordinates plain = new Coordinates("com.example:foo:1.2.3");
    Coordinates fancy = new Coordinates("com.example:foo:jar:jar:1.2.3");

    assertEquals(plain, fancy);
    assertEquals(plain.hashCode(), fancy.hashCode());
  }

  @Test
  public void coordinatesAreConsistentWhenEllidedValuesAreUsed() {
    Coordinates plain = new Coordinates("com.example:foo:1.2.3");
    Coordinates fancy = new Coordinates("com.example:foo:::1.2.3");

    assertEquals(plain, fancy);
    assertEquals(plain.hashCode(), fancy.hashCode());
  }

  @Test
  public void toSnapshotBaseVersionConvertsTimestampedToSnapshot() {
    assertEquals(
        "999.0.0-HEAD-jre-SNAPSHOT",
        Coordinates.toSnapshotBaseVersion("999.0.0-HEAD-jre-20250930.222312-91"));
    assertEquals(
        "1.0-SNAPSHOT",
        Coordinates.toSnapshotBaseVersion("1.0-20260626.231353-374"));
  }

  @Test
  public void toSnapshotBaseVersionReturnsNonTimestampedUnchanged() {
    assertEquals("1.0-SNAPSHOT", Coordinates.toSnapshotBaseVersion("1.0-SNAPSHOT"));
    assertEquals("1.0.0", Coordinates.toSnapshotBaseVersion("1.0.0"));
    assertEquals(null, Coordinates.toSnapshotBaseVersion(null));
  }

  @Test
  public void toRepoPathUsesSnapshotDirForTimestampedSnapshot() {
    Coordinates guava =
        new Coordinates("com.google.guava:guava:999.0.0-HEAD-jre-20250930.222312-91");
    assertEquals(
        "com/google/guava/guava/999.0.0-HEAD-jre-SNAPSHOT/guava-999.0.0-HEAD-jre-20250930.222312-91.jar",
        guava.toRepoPath());
  }

  @Test
  public void toRepoPathUsesSnapshotDirForClassifiedTimestampedSnapshot() {
    Coordinates sources =
        new Coordinates("com.google.guava:guava:jar:sources:999.0.0-HEAD-jre-20250930.222312-91");
    assertEquals(
        "com/google/guava/guava/999.0.0-HEAD-jre-SNAPSHOT/"
            + "guava-999.0.0-HEAD-jre-20250930.222312-91-sources.jar",
        sources.toRepoPath());
  }

  @Test
  public void toRepoPathIsUnchangedForNonTimestampedSnapshot() {
    Coordinates snapshot = new Coordinates("com.example:snapshot:1.0-SNAPSHOT");
    assertEquals(
        "com/example/snapshot/1.0-SNAPSHOT/snapshot-1.0-SNAPSHOT.jar",
        snapshot.toRepoPath());
  }

  @Test
  public void toRepoPathIsUnchangedForRegularVersion() {
    Coordinates release = new Coordinates("com.example:foo:1.2.3");
    assertEquals("com/example/foo/1.2.3/foo-1.2.3.jar", release.toRepoPath());
  }
}
