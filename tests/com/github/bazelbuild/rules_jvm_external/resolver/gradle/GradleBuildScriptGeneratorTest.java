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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependency;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependencyImpl;
import com.google.devtools.build.runfiles.AutoBazelRepository;
import com.google.devtools.build.runfiles.Runfiles;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;

@AutoBazelRepository
public class GradleBuildScriptGeneratorTest {

  @Test
  public void forcedBomUsesStrictVersionAndResolutionForce() throws Exception {
    String script =
        renderBuildScript(
            List.of(
                new GradleDependencyImpl(
                    "com.example", "example-bom", "1.0!!", List.of(), "", "pom")));

    assertTrue(script.contains("implementation(platform(\"com.example:example-bom\"))"));
    assertTrue(script.contains("version { strictly(\"1.0\") }"));
    assertTrue(script.contains("force(\"com.example:example-bom:1.0\")"));
    assertFalse(script.contains("1.0!!"));
  }

  @Test
  public void unforcedBomUsesVersionedPlatformNotation() throws Exception {
    String script =
        renderBuildScript(
            List.of(
                new GradleDependencyImpl(
                    "com.example", "example-bom", "1.0", List.of(), "", "pom")));

    assertTrue(script.contains("implementation platform(\"com.example:example-bom:1.0\")"));
    assertFalse(script.contains("force(\"com.example:example-bom:1.0\")"));
  }

  @Test
  public void forcedDuplicateBomWinsOverEarlierUnforcedBom() throws Exception {
    String script =
        renderBuildScript(
            List.of(
                new GradleDependencyImpl("com.example", "example-bom", "1.0", List.of(), "", "pom"),
                new GradleDependencyImpl(
                    "com.example", "example-bom", "2.0!!", List.of(), "", "pom")));

    assertTrue(script.contains("implementation(platform(\"com.example:example-bom\"))"));
    assertTrue(script.contains("version { strictly(\"2.0\") }"));
    assertTrue(script.contains("force(\"com.example:example-bom:2.0\")"));
    assertFalse(script.contains("com.example:example-bom:1.0"));
  }

  @Test
  public void laterForcedDuplicateBomWinsOverEarlierForcedBom() throws Exception {
    String script =
        renderBuildScript(
            List.of(
                new GradleDependencyImpl(
                    "com.example", "example-bom", "1.0!!", List.of(), "", "pom"),
                new GradleDependencyImpl(
                    "com.example", "example-bom", "2.0!!", List.of(), "", "pom")));

    assertTrue(script.contains("implementation(platform(\"com.example:example-bom\"))"));
    assertTrue(script.contains("version { strictly(\"2.0\") }"));
    assertTrue(script.contains("force(\"com.example:example-bom:2.0\")"));
    assertFalse(script.contains("com.example:example-bom:1.0"));
  }

  private String renderBuildScript(List<GradleDependency> boms) throws Exception {
    Runfiles runfiles =
        Runfiles.preload()
            .withSourceRepository(AutoBazelRepository_GradleBuildScriptGeneratorTest.NAME);
    Path template =
        Paths.get(
            runfiles.rlocation(
                "rules_jvm_external/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/data/build.gradle.hbs"));
    Path output = Files.createTempFile("rje-build-script", ".gradle");

    GradleBuildScriptGenerator.generateBuildScript(
        template,
        output,
        List.of(new Repository(URI.create("https://repo1.maven.org/maven2"))),
        List.of(),
        boms,
        List.of(),
        false);

    return Files.readString(output);
  }
}
