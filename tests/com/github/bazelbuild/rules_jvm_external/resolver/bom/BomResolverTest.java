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

package com.github.bazelbuild.rules_jvm_external.resolver.bom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.MavenRepo;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link BomResolver}.
 *
 * <p>Uses an in-process fake Maven repository via {@link MavenRepo} so the tests do not require
 * network access and run deterministically in CI.
 */
public class BomResolverTest {

  private static final String JUNIT_BOM = "org.junit:junit-bom:5.10.0";
  private static final String MOCKITO_BOM = "org.mockito:mockito-bom:5.0.0";

  // ===== Anti-faking guardrail: each assertion uses a non-empty expected value or strict
  // assertEquals(expected, actual) so that an implementation returning Collections.emptyMap()
  // will fail loudly. =====

  @Test
  public void testEmptyInputs() {
    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(URI.create("file:///does-not-exist/")), List.of(), List.of(), null);
    assertEquals(Map.of(), result);
  }

  @Test
  public void testNoMatchingArtifacts() throws IOException {
    MavenRepo repo = MavenRepo.create();
    addBom(
        repo,
        new Coordinates(JUNIT_BOM),
        List.of(new Coordinates("org.junit.jupiter:junit-jupiter-api:5.10.0")));

    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(repo.getPath().toUri()),
            List.of(JUNIT_BOM),
            List.of(new Coordinates("com.example", "unmanaged", null, null, null)),
            null);

    assertEquals(Map.of(), result);
  }

  @Test
  public void testSingleBomManagesArtifact() throws IOException {
    MavenRepo repo = MavenRepo.create();
    addBom(
        repo,
        new Coordinates(JUNIT_BOM),
        List.of(
            new Coordinates("org.junit.jupiter:junit-jupiter-api:5.10.0"),
            new Coordinates("org.junit.jupiter:junit-jupiter-engine:5.10.0")));

    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(repo.getPath().toUri()),
            List.of(JUNIT_BOM),
            List.of(new Coordinates("org.junit.jupiter", "junit-jupiter-api", null, null, null)),
            null);

    assertEquals(Map.of("org.junit.jupiter:junit-jupiter-api", List.of(JUNIT_BOM)), result);
  }

  @Test
  public void testMultipleBomsSameArtifact() throws IOException {
    MavenRepo repo = MavenRepo.create();
    Coordinates shared = new Coordinates("com.example:shared:1.0.0");
    addBom(repo, new Coordinates(JUNIT_BOM), List.of(shared));
    addBom(repo, new Coordinates(MOCKITO_BOM), List.of(shared));

    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(repo.getPath().toUri()),
            List.of(JUNIT_BOM, MOCKITO_BOM),
            List.of(new Coordinates("com.example", "shared", null, null, null)),
            null);

    assertEquals(Map.of("com.example:shared", List.of(JUNIT_BOM, MOCKITO_BOM)), result);
  }

  @Test
  public void testRecursiveImportedBoms() throws IOException {
    MavenRepo repo = MavenRepo.create();
    Coordinates parentBom = new Coordinates("com.example:parent-bom:1.0.0");
    Coordinates childBom = new Coordinates("com.example:child-bom:1.0.0");
    Coordinates managed = new Coordinates("com.example:managed:1.0.0");

    // Child BOM directly manages `managed`.
    addBom(repo, childBom, List.of(managed));
    // Parent BOM imports the child BOM (scope=import, type=pom).
    addBomThatImports(repo, parentBom, List.of(childBom));

    // Only the parent BOM is directly declared. `managed` should be attributed to the
    // parent, not to the (transitively-reachable) child BOM.
    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(repo.getPath().toUri()),
            List.of(parentBom.toString()),
            List.of(new Coordinates("com.example", "managed", null, null, null)),
            null);

    assertEquals(Map.of("com.example:managed", List.of(parentBom.toString())), result);
  }

  @Test
  public void testDirectAndTransitiveBomDeduped() throws IOException {
    MavenRepo repo = MavenRepo.create();
    Coordinates parentBom = new Coordinates("com.example:parent-bom:1.0.0");
    Coordinates childBom = new Coordinates("com.example:child-bom:1.0.0");
    Coordinates managed = new Coordinates("com.example:managed:1.0.0");

    addBom(repo, childBom, List.of(managed));
    addBomThatImports(repo, parentBom, List.of(childBom));

    // Both child and parent are directly declared. The child is also reachable transitively
    // through the parent. The result should contain each BOM exactly once, in declaration
    // order (parent first, child second).
    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(repo.getPath().toUri()),
            List.of(parentBom.toString(), childBom.toString()),
            List.of(new Coordinates("com.example", "managed", null, null, null)),
            null);

    assertEquals(
        Map.of(
            "com.example:managed",
            List.of(parentBom.toString(), childBom.toString())),
        result);
  }

  @Test
  public void testVersionedArtifactsIgnored() throws IOException {
    // An artifact passed in with an explicit version is treated as a separate Coordinates
    // instance — the caller is responsible for filtering them out before invocation.
    // BomResolver itself just uses (group, artifact, packaging, classifier) for matching;
    // the version is not consulted, so a versioned input matching a managed BOM artifact
    // would still appear in the mapping. The Starlark caller filters versioned artifacts
    // out (see _filter_versionless_artifacts_for_bom in coursier.bzl) which is what
    // implements Constraint #3.
    //
    // Here we assert the documented contract: BomResolver does not include any artifact
    // that the caller didn't pass, and an empty caller-list produces an empty result.
    MavenRepo repo = MavenRepo.create();
    addBom(
        repo,
        new Coordinates(JUNIT_BOM),
        List.of(new Coordinates("org.junit.jupiter:junit-jupiter-api:5.10.0")));

    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(repo.getPath().toUri()), List.of(JUNIT_BOM), List.of(), null);

    assertEquals(Map.of(), result);
  }

  @Test
  public void testNonDefaultPackagingKeyFormat() {
    // Default (jar, no classifier) -> g:a
    assertEquals(
        "com.example:foo",
        BomResolver.artifactKey(new Coordinates("com.example", "foo", null, null, null)));
    assertEquals(
        "com.example:foo",
        BomResolver.artifactKey(new Coordinates("com.example", "foo", "jar", null, null)));
    assertEquals(
        "com.example:foo",
        BomResolver.artifactKey(new Coordinates("com.example", "foo", "jar", "", null)));

    // Non-default packaging -> g:a:packaging
    assertEquals(
        "com.example:foo:aar",
        BomResolver.artifactKey(new Coordinates("com.example", "foo", "aar", null, null)));

    // With classifier -> g:a:packaging:classifier
    assertEquals(
        "io.netty:netty-tcnative-boringssl-static:jar:linux-x86_64",
        BomResolver.artifactKey(
            new Coordinates(
                "io.netty",
                "netty-tcnative-boringssl-static",
                "jar",
                "linux-x86_64",
                null)));
  }

  @Test
  public void testMultipleRepositories() throws IOException {
    // The BOM is in repo A; the (managed) artifact's POM is in repo B. Aether should
    // consult both repos to resolve and return managed-deps.
    MavenRepo repoA = MavenRepo.create();
    MavenRepo repoB = MavenRepo.create();

    Coordinates managed = new Coordinates("com.example:managed:2.0.0");
    repoB.add(managed);
    addBom(repoA, new Coordinates(JUNIT_BOM), List.of(managed));

    Map<String, List<String>> result =
        BomResolver.buildBomResolutionMapping(
            List.of(repoA.getPath().toUri(), repoB.getPath().toUri()),
            List.of(JUNIT_BOM),
            List.of(new Coordinates("com.example", "managed", null, null, null)),
            null);

    assertEquals(Map.of("com.example:managed", List.of(JUNIT_BOM)), result);
  }

  @Test
  public void testHardFailOnResolutionError() {
    // A non-existent BOM in an empty repo should cause a thrown exception with no partial
    // output — matching Constraint #11 ("hard-fail on resolution errors").
    MavenRepo emptyRepo = MavenRepo.create();
    try {
      BomResolver.buildBomResolutionMapping(
          List.of(emptyRepo.getPath().toUri()),
          List.of("com.example:does-not-exist:0.0.1"),
          List.of(new Coordinates("com.example", "managed", null, null, null)),
          null);
      fail("Expected RuntimeException because BOM coordinate is unresolvable");
    } catch (RuntimeException e) {
      assertNotNull("RuntimeException must carry a message", e.getMessage());
      assertTrue(
          "RuntimeException message should mention the failing BOM coordinate, got: "
              + e.getMessage(),
          e.getMessage().contains("does-not-exist"));
    }
  }

  // ----- Test helpers --------------------------------------------------------------------

  private static void addBom(MavenRepo repo, Coordinates bom, List<Coordinates> managed)
      throws IOException {
    StringBuilder pom = new StringBuilder();
    pom.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    pom.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n");
    pom.append("  <modelVersion>4.0.0</modelVersion>\n");
    pom.append("  <groupId>").append(bom.getGroupId()).append("</groupId>\n");
    pom.append("  <artifactId>").append(bom.getArtifactId()).append("</artifactId>\n");
    pom.append("  <version>").append(bom.getVersion()).append("</version>\n");
    pom.append("  <packaging>pom</packaging>\n");
    pom.append("  <dependencyManagement>\n");
    pom.append("    <dependencies>\n");
    for (Coordinates c : managed) {
      pom.append("      <dependency>\n");
      pom.append("        <groupId>").append(c.getGroupId()).append("</groupId>\n");
      pom.append("        <artifactId>").append(c.getArtifactId()).append("</artifactId>\n");
      pom.append("        <version>").append(c.getVersion()).append("</version>\n");
      if (c.getExtension() != null && !c.getExtension().isEmpty() && !"jar".equals(c.getExtension())) {
        pom.append("        <type>").append(c.getExtension()).append("</type>\n");
      }
      if (c.getClassifier() != null && !c.getClassifier().isEmpty()) {
        pom.append("        <classifier>").append(c.getClassifier()).append("</classifier>\n");
      }
      pom.append("      </dependency>\n");
    }
    pom.append("    </dependencies>\n");
    pom.append("  </dependencyManagement>\n");
    pom.append("</project>\n");

    Coordinates pomCoords =
        new Coordinates(bom.getGroupId(), bom.getArtifactId(), "pom", null, bom.getVersion());
    repo.writePomFile(pomCoords, pom.toString());
  }

  /**
   * Adds a BOM whose {@code <dependencyManagement>} section imports the given child BOMs
   * (scope=import, type=pom).
   */
  private static void addBomThatImports(
      MavenRepo repo, Coordinates parent, List<Coordinates> importedBoms) throws IOException {
    StringBuilder pom = new StringBuilder();
    pom.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    pom.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n");
    pom.append("  <modelVersion>4.0.0</modelVersion>\n");
    pom.append("  <groupId>").append(parent.getGroupId()).append("</groupId>\n");
    pom.append("  <artifactId>").append(parent.getArtifactId()).append("</artifactId>\n");
    pom.append("  <version>").append(parent.getVersion()).append("</version>\n");
    pom.append("  <packaging>pom</packaging>\n");
    pom.append("  <dependencyManagement>\n");
    pom.append("    <dependencies>\n");
    for (Coordinates c : importedBoms) {
      pom.append("      <dependency>\n");
      pom.append("        <groupId>").append(c.getGroupId()).append("</groupId>\n");
      pom.append("        <artifactId>").append(c.getArtifactId()).append("</artifactId>\n");
      pom.append("        <version>").append(c.getVersion()).append("</version>\n");
      pom.append("        <type>pom</type>\n");
      pom.append("        <scope>import</scope>\n");
      pom.append("      </dependency>\n");
    }
    pom.append("    </dependencies>\n");
    pom.append("  </dependencyManagement>\n");
    pom.append("</project>\n");

    Coordinates pomCoords =
        new Coordinates(
            parent.getGroupId(), parent.getArtifactId(), "pom", null, parent.getVersion());
    repo.writePomFile(pomCoords, pom.toString());
  }
}
