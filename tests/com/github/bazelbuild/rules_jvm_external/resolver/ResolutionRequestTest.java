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

package com.github.bazelbuild.rules_jvm_external.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ResolutionRequestTest {

  @Test
  public void artifactEqualityIncludesForceVersion() {
    Coordinates coordinates = new Coordinates("com.example:dependency:1.0");

    assertNotEquals(new Artifact(coordinates, Set.of()), new Artifact(coordinates, Set.of(), true));
  }

  @Test
  public void replaceDependenciesPreservesBomMetadata() {
    Coordinates bomCoordinates = new Coordinates("com.example", "example-bom", "pom", "", "1.0");
    Artifact forcedBom =
        new Artifact(bomCoordinates, Set.of(new Coordinates("com.example:excluded")), true);
    Artifact replacement = new Artifact(new Coordinates("com.example:replacement:1.0"), Set.of());

    ResolutionRequest replaced =
        new ResolutionRequest()
            .addRepository("https://repo.example.com")
            .addBom(forcedBom)
            .replaceDependencies(List.of(replacement));

    assertEquals(List.of(forcedBom), replaced.getBoms());
    assertTrue(replaced.getBoms().get(0).isForceVersion());
    assertEquals(
        Set.of(new Coordinates("com.example:excluded")), replaced.getBoms().get(0).getExclusions());
    assertEquals("pom", replaced.getBoms().get(0).getCoordinates().getExtension());
  }
}
