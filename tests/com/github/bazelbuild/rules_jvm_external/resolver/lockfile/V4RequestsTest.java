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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class V4RequestsTest {
  @Test
  public void shouldCompactMatchingVersionAndPreserveUnknownRequestFields() {
    V4Artifact selected = artifact("jar", "", "2");
    Map<String, Object> request =
        Map.of(
            "group",
            "g",
            "artifact",
            "a",
            "version",
            "2",
            "testonly",
            true,
            "future",
            Map.of("key", "value"));
    assertEquals(List.of(), V4Requests.attach(List.of(selected), List.of(request, request)));
    assertEquals(
        Map.of("requests", List.of(Map.of("testonly", true, "future", Map.of("key", "value")))),
        selected.metadata);
    assertEquals("2", request.get("version"));
  }

  @Test
  public void shouldTreatTheJarClassifierSentinelAsTheMainArtifact() {
    V4Artifact main = artifact("jar", "", "2");
    Map<String, Object> request =
        Map.of("group", "g", "artifact", "a", "version", "2", "classifier", "jar");
    assertEquals(List.of(), V4Requests.attach(List.of(main), List.of(request)));
    assertEquals(List.of(Map.of("classifier", "jar")), main.metadata.get("requests"));
  }

  @Test
  public void shouldPreserveRequestedSelectorsAndExplicitPackaging() {
    V4Artifact selected = artifact("jar", "", "2");
    V4Requests.attach(
        List.of(selected),
        List.of(Map.of("group", "g", "artifact", "a", "version", "", "packaging", "jar")));
    assertEquals(
        List.of(Map.of("version", "", "packaging", "jar")), selected.metadata.get("requests"));
  }

  @Test
  public void shouldAttachClassifierRequestsOnlyToTheExactClassifier() {
    V4Artifact main = artifact("jar", "", "2");
    V4Artifact sources = artifact("jar", "sources", "2");
    Map<String, Object> request =
        Map.of("group", "g", "artifact", "a", "version", "1", "classifier", "sources");
    V4Requests.attach(List.of(main, sources), List.of(request));
    assertTrue(main.metadata.isEmpty());
    assertEquals(
        List.of(Map.of("version", "1", "classifier", "sources")), sources.metadata.get("requests"));
    assertEquals(List.of(request), V4Requests.attach(List.of(main), List.of(request)));
  }

  @Test
  public void shouldPreferMainJarAndInferUniqueNonJarOnlyWhenPackagingIsOmitted() {
    V4Artifact jar = artifact("jar", "", "2");
    V4Artifact aar = artifact("aar", "", "2");
    Map<String, Object> request = Map.of("group", "g", "artifact", "a", "version", "2");
    V4Requests.attach(List.of(aar, jar), List.of(request));
    assertFalse(jar.metadata.isEmpty());
    assertTrue(aar.metadata.isEmpty());
    V4Requests.attach(List.of(aar), List.of(request));
    assertEquals(List.of(Map.of()), aar.metadata.get("requests"));
    Map<String, Object> explicit =
        Map.of("group", "g", "artifact", "a", "version", "2", "packaging", "jar");
    assertEquals(List.of(explicit), V4Requests.attach(List.of(aar), List.of(explicit)));
  }

  @Test
  public void shouldInferDownloadPackagingWhenPomIsAlsoPresent() {
    V4Artifact json = artifact("json", "", "2");
    V4Artifact pom = artifact("pom", "", "2");
    V4Requests.attach(
        List.of(pom, json), List.of(Map.of("group", "g", "artifact", "a", "version", "2")));
    assertEquals(List.of(Map.of()), json.metadata.get("requests"));
    assertTrue(pom.metadata.isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectAmbiguousInferredPackaging() {
    V4Requests.attach(
        List.of(artifact("aar", "", "2"), artifact("zip", "", "2")),
        List.of(Map.of("group", "g", "artifact", "a", "version", "2")));
  }

  @Test
  public void shouldSortAndRetainUnresolvedRequestsWithoutAddingRows() {
    Map<String, Object> z = Map.of("group", "g", "artifact", "z", "version", "");
    Map<String, Object> a = Map.of("group", "g", "artifact", "a", "version", "1");
    assertEquals(List.of(a, z), V4Requests.attach(List.of(), List.of(z, a, z)));
  }

  @Test
  public void shouldSortMultipleRequestsByTheirCompactCanonicalJson() {
    V4Artifact selected = artifact("jar", "", "2");
    V4Requests.attach(
        List.of(selected),
        List.of(
            Map.of("group", "g", "artifact", "a", "version", "1"),
            Map.of("group", "g", "artifact", "a", "version", "2", "neverlink", true)));
    assertEquals(
        List.of(Map.of("neverlink", true), Map.of("version", "1")),
        selected.metadata.get("requests"));
  }

  private static V4Artifact artifact(String extension, String classifier, String version) {
    return new V4Artifact(new Coordinates("g", "a", extension, classifier, version), "sha");
  }
}
