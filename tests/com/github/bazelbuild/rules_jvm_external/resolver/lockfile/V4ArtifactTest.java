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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class V4ArtifactTest {
  @Test
  public void shouldMatchCanonicalJsonAndHashGolden() {
    V4Artifact artifact = new V4Artifact(new Coordinates("g:a:1"), null);
    artifact.metadata.put("requests", List.of(Map.of("version", "[1,2)", "testonly", true)));
    String canonical =
        "[\"g:a:1\",null,[\"g:b\",\"g:c\"],{\"requests\":[{\"testonly\":true,\"version\":\"[1,2)\"}]},false,true]";
    assertEquals(
        canonical,
        V4Json.compact(
            Arrays.asList("g:a:1", null, List.of("g:b", "g:c"), artifact.metadata, false, true)));
    assertEquals(1788491109, artifact.row(List.of("g:c", "g:b"), false, true).get(2));
  }

  @Test
  public void shouldPreserveNullChecksumsAndExternalCoordinateSyntax() {
    V4Artifact artifact = new V4Artifact(new Coordinates("g", "a", "aar", "sources", "2"), null);
    assertEquals("g:a:2:sources@aar", artifact.externalForm());
    assertEquals("g:a:aar:sources", artifact.coordinates.asKey());
    assertEquals(
        Arrays.asList(
            "g:a:2:sources@aar",
            null,
            V4Json.compact(
                    Arrays.asList("g:a:2:sources@aar", null, List.of(), Map.of(), false, true))
                .hashCode()),
        artifact.row(List.of(), false, true));
  }

  @Test
  public void shouldHashMetadataAndLocalStatusButNotDependencyOrder() {
    V4Artifact artifact = new V4Artifact(new Coordinates("g:a:1"), "sha");
    Object hash = artifact.row(List.of("g:c", "g:b"), false, false).get(2);
    assertEquals(hash, artifact.row(List.of("g:b", "g:c", "g:b"), false, false).get(2));
    assertNotEquals(hash, artifact.row(List.of("g:b", "g:c"), true, false).get(2));
    artifact.metadata.put("variants", List.of("g:a:jar:sources"));
    assertNotEquals(hash, artifact.row(List.of("g:b", "g:c"), false, false).get(2));
  }

  @Test
  public void shouldSortNestedMapsWithoutEscapingHtmlOrReorderingLists() {
    Map<String, Object> nested = new java.util.HashMap<>();
    nested.put("b", true);
    nested.put("a", null);
    assertEquals(
        "{\"a\":[\"β\",\"<>&='\"],\"z\":{\"a\":null,\"b\":true}}",
        V4Json.compact(Map.of("z", nested, "a", List.of("β", "<>&='"))));
  }
}
