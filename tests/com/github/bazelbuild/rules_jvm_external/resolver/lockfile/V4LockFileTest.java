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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class V4LockFileTest {
  private static final String SHA = "a".repeat(64);

  @Test
  public void shouldRenderSortedCompactRowsWithBlankSeparatorsAndNoGlobalHashes() {
    Map<String, Object> v3 = v3("1");
    v3.put("__INPUT_ARTIFACTS_HASH", Map.of("old", 1));
    v3.put("__RESOLVED_ARTIFACTS_HASH", Map.of("old", 2));
    String text = V4LockFile.render(v3, Map.of());
    assertTrue(text.contains("    [\"g:a:1\",\"" + SHA + "\","));
    assertTrue(text.contains("],\n\n    [\"g:b:1\""));
    assertTrue(text.indexOf("g:a:1") < text.indexOf("g:b:1"));
    assertFalse(text.contains("__INPUT_ARTIFACTS_HASH"));
    assertFalse(text.contains("__RESOLVED_ARTIFACTS_HASH"));
    assertTrue(text.endsWith("\n"));
    Map<String, Object> result = parse(text);
    assertEquals("4", result.get("version"));
    assertEquals(Map.of("g:a", List.of("g:b")), result.get("dependencies"));
    assertEquals(List.of(), result.get("boms"));
    assertEquals(Map.of(), result.get("settings"));
    assertEquals(List.of(), result.get("unresolved_requests"));
  }

  @Test
  public void shouldPreserveBomOrderAndSettingsInABomOnlyUniverse() {
    Map<String, Object> first = Map.of("group", "g", "artifact", "z-bom", "version", "1");
    Map<String, Object> second = Map.of("group", "g", "artifact", "a-bom", "version", "2");
    Map<String, Object> settings = Map.of("fetch_sources", true, "future", List.of("z", "a"));
    Map<String, Object> result =
        parse(
            V4LockFile.render(
                Map.of(), Map.of("boms", List.of(first, second), "settings", settings)));
    assertEquals(List.of(), result.get("artifacts"));
    assertEquals(List.of(first, second), result.get("boms"));
    assertEquals(settings, result.get("settings"));
  }

  @Test
  public void shouldRetainAncillaryMapsAndIncludeFilesOnlyWhenRequested() {
    Map<String, Object> v3 = v3("1");
    v3.put("files", Map.of("g:a", "/tmp/a.jar"));
    v3.put("packages", Map.of("g:a", List.of("com.example")));
    v3.put("services", Map.of("g:a", Map.of("api", List.of("impl"))));
    v3.put("conflict_resolution", Map.of("g:a:0", "g:a:1"));
    Map<String, Object> result = parse(V4LockFile.render(v3, Map.of()));
    assertFalse(result.containsKey("files"));
    for (String key : List.of("packages", "services", "conflict_resolution")) {
      assertEquals(v3.get(key), result.get(key));
    }
    assertEquals(v3.get("files"), parse(V4LockFile.render(v3, Map.of(), true)).get("files"));
  }

  @Test
  public void shouldKeepRepositoryHintsAndOrderOutOfRowHashes() {
    Map<String, Object> v3 = v3("1");
    Map<String, Object> before = parse(V4LockFile.render(v3, Map.of()));
    Map<String, Object> repositories = new LinkedHashMap<>();
    repositories.put("https://z/", List.of("g:b", "g:a"));
    repositories.put("https://a/", List.of("g:a"));
    v3.put("repositories", repositories);
    String text = V4LockFile.render(v3, Map.of());
    assertEquals(before.get("artifacts"), parse(text).get("artifacts"));
    assertEquals(
        Map.of("https://z/", List.of("g:a", "g:b"), "https://a/", List.of("g:a")),
        parse(text).get("repositories"));
    assertTrue(text.indexOf("https://z/") < text.indexOf("https://a/"));
  }

  @Test
  public void shouldCanonicaliseRepositoryMembersWithoutMarkingRemoteRowsLocal() {
    Map<String, Object> v3 = v3("1");
    v3.put("m2local", true);
    List<String> members = List.of("g:b:jar", "g:a:jar", "g:a");
    Map<String, Object> repositories = new LinkedHashMap<>();
    repositories.put("https://z/", members);
    repositories.put("https://a/", List.of());
    v3.put("repositories", repositories);
    String text = V4LockFile.render(v3, Map.of());
    Map<String, Object> result = parse(text);
    assertEquals(
        Map.of("https://z/", List.of("g:a", "g:b"), "https://a/", List.of()),
        result.get("repositories"));
    assertEquals(List.of(), result.get("m2local_artifacts"));
    assertTrue(text.indexOf("https://z/") < text.indexOf("https://a/"));
    assertEquals(members, repositories.get("https://z/"));
  }

  @Test
  public void shouldChangeOnlyTheChildRowWhenItsResolvedVersionChanges() {
    List<List<Object>> before = rows(V4LockFile.render(v3("1"), Map.of()));
    List<List<Object>> after = rows(V4LockFile.render(v3("2"), Map.of()));
    assertEquals(before.get(0), after.get(0));
    assertFalse(before.get(1).equals(after.get(1)));
  }

  @Test
  public void shouldIncludeRequestMetadataAndUnresolvedInputs() {
    Map<String, Object> selected =
        Map.of("group", "g", "artifact", "a", "version", "0", "packaging", "jar");
    Map<String, Object> unresolved = Map.of("group", "g", "artifact", "missing", "version", "");
    Map<String, Object> result =
        parse(
            V4LockFile.render(
                v3("1"), Map.of("artifacts", List.of(unresolved, selected, selected))));
    assertEquals(List.of(unresolved), result.get("unresolved_requests"));
    assertEquals(
        Map.of("requests", List.of(Map.of("version", "0", "packaging", "jar"))),
        ((List<?>) ((List<?>) result.get("artifacts")).get(0)).get(3));
  }

  @Test
  public void shouldEmitLocalAndSkippedInventoriesAndRetainNullRows() {
    Map<String, Object> sums = new HashMap<>();
    sums.put("jar", null);
    Map<String, Object> v3 = v3("1");
    v3.put(
        "artifacts", Map.of("g:a", artifact("1"), "g:b", Map.of("version", "1", "shasums", sums)));
    v3.put("repositories", Map.of());
    v3.put("m2local", true);
    v3.put("skipped", List.of("g:b"));
    Map<String, Object> result = parse(V4LockFile.render(v3, Map.of()));
    assertEquals(true, result.get("m2local"));
    assertEquals(List.of("g:a"), result.get("m2local_artifacts"));
    assertEquals(List.of("g:b"), result.get("skipped"));
    assertEquals(null, ((List<?>) ((List<?>) result.get("artifacts")).get(1)).get(1));
  }

  private static Map<String, Object> v3(String childVersion) {
    return new HashMap<>(
        Map.of(
            "artifacts",
            Map.of("g:b", artifact(childVersion), "g:a", artifact("1")),
            "dependencies",
            Map.of("g:a", List.of("g:b")),
            "repositories",
            Map.of("https://original/", List.of("g:a", "g:b"))));
  }

  private static Map<String, Object> artifact(String version) {
    return Map.of("version", version, "shasums", Map.of("jar", SHA));
  }

  private static Map<String, Object> parse(String text) {
    return new Gson().fromJson(text, new TypeToken<Map<String, Object>>() {}.getType());
  }

  @SuppressWarnings("unchecked")
  private static List<List<Object>> rows(String text) {
    return (List<List<Object>>) parse(text).get("artifacts");
  }
}
