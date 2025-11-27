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

package com.github.bazelbuild.rules_jvm_external.resolver.lockfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.DependencyInfo;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.Test;

public class DependencyIndexTest {

  private final URI defaultRepo = URI.create("http://localhost/m2/repository/");
  private final Set<URI> repos = Set.of(defaultRepo);

  @Test
  public void shouldIncludeVersionNumber() {
    DependencyIndex index = new DependencyIndex(Set.of());
    Map<String, Object> rendered = index.render();

    assertEquals(2, rendered.get("version"));
  }

  @Test
  public void shouldRenderEmptyClassesForEmptyInfos() {
    DependencyIndex index = new DependencyIndex(Set.of());
    Map<String, Object> rendered = index.render();

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Set<String>>> classes =
        (Map<String, Map<String, Set<String>>>) rendered.get("classes");
    assertTrue(classes.isEmpty());
  }

  @Test
  public void shouldGroupClassesByPackage() {
    Set<String> artifactClasses = new TreeSet<>();
    artifactClasses.add("com.example.Foo");
    artifactClasses.add("com.example.Bar");
    artifactClasses.add("com.example.sub.Baz");

    DependencyInfo info =
        new DependencyInfo(
            new Coordinates("com.example:item:1.0.0"),
            repos,
            Optional.empty(),
            Optional.of("abc123"),
            Set.of(),
            Set.of("com.example", "com.example.sub"),
            artifactClasses,
            new TreeMap<>());

    DependencyIndex index = new DependencyIndex(Set.of(info));
    Map<String, Object> rendered = index.render();

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Set<String>>> classes =
        (Map<String, Map<String, Set<String>>>) rendered.get("classes");

    assertEquals(1, classes.size());

    Map<String, Set<String>> artifactPackages = classes.get("com.example:item");
    assertEquals(2, artifactPackages.size());

    assertEquals(Set.of("Bar", "Foo"), artifactPackages.get("com.example"));
    assertEquals(Set.of("Baz"), artifactPackages.get("com.example.sub"));
  }

  @Test
  public void shouldHandleDefaultPackage() {
    Set<String> artifactClasses = new TreeSet<>();
    artifactClasses.add("Foo");

    DependencyInfo info =
        new DependencyInfo(
            new Coordinates("com.example:item:1.0.0"),
            repos,
            Optional.empty(),
            Optional.of("abc123"),
            Set.of(),
            Set.of(""),
            artifactClasses,
            new TreeMap<>());

    DependencyIndex index = new DependencyIndex(Set.of(info));
    Map<String, Object> rendered = index.render();

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Set<String>>> classes =
        (Map<String, Map<String, Set<String>>>) rendered.get("classes");

    Map<String, Set<String>> artifactPackages = classes.get("com.example:item");
    assertEquals(Set.of("Foo"), artifactPackages.get(""));
  }

  @Test
  public void shouldSkipSourcesArtifacts() {
    Set<String> artifactClasses = new TreeSet<>();
    artifactClasses.add("com.example.Foo");

    DependencyInfo sources =
        new DependencyInfo(
            new Coordinates("com.example:item:jar:sources:1.0.0"),
            repos,
            Optional.empty(),
            Optional.of("abc123"),
            Set.of(),
            Set.of(),
            artifactClasses,
            new TreeMap<>());

    DependencyIndex index = new DependencyIndex(Set.of(sources));
    Map<String, Object> rendered = index.render();

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Set<String>>> classes =
        (Map<String, Map<String, Set<String>>>) rendered.get("classes");
    assertTrue(classes.isEmpty());
  }

  @Test
  public void shouldSkipJavadocArtifacts() {
    Set<String> artifactClasses = new TreeSet<>();
    artifactClasses.add("com.example.Foo");

    DependencyInfo javadoc =
        new DependencyInfo(
            new Coordinates("com.example:item:jar:javadoc:1.0.0"),
            repos,
            Optional.empty(),
            Optional.of("abc123"),
            Set.of(),
            Set.of(),
            artifactClasses,
            new TreeMap<>());

    DependencyIndex index = new DependencyIndex(Set.of(javadoc));
    Map<String, Object> rendered = index.render();

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Set<String>>> classes =
        (Map<String, Map<String, Set<String>>>) rendered.get("classes");
    assertTrue(classes.isEmpty());
  }

  @Test
  public void shouldSkipArtifactsWithNoClasses() {
    DependencyInfo info =
        new DependencyInfo(
            new Coordinates("com.example:item:1.0.0"),
            repos,
            Optional.empty(),
            Optional.of("abc123"),
            Set.of(),
            Set.of("com.example"),
            Set.of(),
            new TreeMap<>());

    DependencyIndex index = new DependencyIndex(Set.of(info));
    Map<String, Object> rendered = index.render();

    @SuppressWarnings("unchecked")
    Map<String, Map<String, Set<String>>> classes =
        (Map<String, Map<String, Set<String>>>) rendered.get("classes");
    assertTrue(classes.isEmpty());
  }
}
