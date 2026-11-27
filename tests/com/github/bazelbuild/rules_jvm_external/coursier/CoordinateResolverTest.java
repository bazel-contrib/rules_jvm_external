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

package com.github.bazelbuild.rules_jvm_external.coursier;

import static org.junit.Assert.assertEquals;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CoordinateResolverTest {
  @Rule public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldResolveImplicitTestJarClassifierToSelectedJar() throws Exception {
    Coordinates requested = new Coordinates("g:a:test-jar:1");
    Coordinates selected = new Coordinates("g:a:jar:tests:1");
    CoordinateResolver resolver =
        new CoordinateResolver(
            Set.of(),
            temp.getRoot().toPath(),
            Map.of(new Coordinates("g:a:test-jar:tests:1"), selected));
    assertEquals(selected, resolver.resolve(requested));
  }

  @Test
  public void shouldFollowCachedPomRelocationToSelectedModule() throws Exception {
    Path cache = temp.getRoot().toPath();
    Path pom = cache.resolve("https/repo.example%3A8443/maven/old/a/1%2Bbuild/a-1%2Bbuild.pom");
    Files.createDirectories(pom.getParent());
    Files.writeString(
        pom,
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><distributionManagement>"
            + "<relocation><groupId>new</groupId></relocation></distributionManagement></project>");
    Coordinates selected = new Coordinates("new:a:2");
    CoordinateResolver resolver =
        new CoordinateResolver(
            Set.of(URI.create("https://repo.example:8443/maven")),
            cache,
            Map.of(selected, selected));
    assertEquals(selected, resolver.resolve(new Coordinates("old:a:1+build")));
  }

  @Test
  public void shouldPreserveAnExplicitClassifierWhenMappingPackaging() {
    Coordinates selected = new Coordinates("g:a:jar:custom:1");
    CoordinateResolver resolver =
        new CoordinateResolver(Set.of(), null, Map.of(selected, selected));
    assertEquals(selected, resolver.resolve(new Coordinates("g:a:test-jar:custom:1")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectCyclicPomRelocations() throws Exception {
    Path cache = temp.getRoot().toPath();
    for (String group : Set.of("one", "two")) {
      Path pom = cache.resolve("https/repo.example/" + group + "/a/1/a-1.pom");
      Files.createDirectories(pom.getParent());
      Files.writeString(
          pom,
          "<project><distributionManagement><relocation><groupId>"
              + (group.equals("one") ? "two" : "one")
              + "</groupId></relocation></distributionManagement></project>");
    }
    new CoordinateResolver(Set.of(URI.create("https://repo.example/")), cache, Map.of())
        .resolve(new Coordinates("one:a:1"));
  }

  @Test
  public void shouldIgnorePluginRelocations() throws Exception {
    Path cache = temp.getRoot().toPath();
    Path pom = cache.resolve("https/repo.example/g/a/1/a-1.pom");
    Files.createDirectories(pom.getParent());
    Files.writeString(
        pom,
        "<project><build><plugins><plugin><configuration><relocation>"
            + "<groupId>new</groupId></relocation></configuration></plugin></plugins></build></project>");
    Coordinates requested = new Coordinates("g:a:1");
    CoordinateResolver resolver =
        new CoordinateResolver(Set.of(URI.create("https://repo.example/")), cache, Map.of());
    assertEquals(requested, resolver.resolve(requested));
  }

  @Test
  public void shouldLeaveUnknownDependenciesForGraphValidation() throws Exception {
    Coordinates missing = new Coordinates("g:missing:1");
    CoordinateResolver resolver =
        new CoordinateResolver(
            Set.of(URI.create("https://repo.example/maven")), temp.getRoot().toPath(), Map.of());
    assertEquals(missing, resolver.resolve(missing));
  }
}
