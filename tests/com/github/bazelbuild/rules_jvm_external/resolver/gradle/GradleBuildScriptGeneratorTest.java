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


package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.Exclusion;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependency;
import com.google.devtools.build.runfiles.Runfiles;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GradleBuildScriptGeneratorTest {

    @Test
    public void simple() throws IOException {
        List<Repository> repositories = List.of(
                new Repository(
                        URI.create("https://repo1.maven.org/maven2")
                )
        );
        List<GradleDependency> dependencies = List.of(
                new GradleDependency(
                        GradleDependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "foo",
                        "0.0.1"
                ),
                new GradleDependency(
                        GradleDependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "bar",
                        "0.1.0"
                )
        );

        List<GradleDependency> boms  = List.of();
        runGoldenTemplateTest("simple", repositories, dependencies, boms, new HashSet<>());
    }

    @Test
    public void multipleRepositoresWithCredentials() throws IOException {
        List<Repository> repositories = List.of(
                new Repository(
                        URI.create("https://repo1.maven.org/maven2")
                ),
                new Repository(
                        URI.create("https://com.foo.org/maven2"),
                        true,
                        "fooUsername",
                        "fooPassword"
                )
        );
        List<GradleDependency> dependencies = List.of(
                new GradleDependency(
                        GradleDependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "foo",
                        "0.0.1"
                ),
                new GradleDependency(
                        GradleDependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "bar",
                        "0.1.0"
                )
        );

        List<GradleDependency> boms  = List.of();
        runGoldenTemplateTest("multipleRepositories", repositories, dependencies, boms, new HashSet<>());
    }

    @Test
    public void boms() throws IOException {
        List<Repository> repositories = List.of(
                new Repository(
                        URI.create("https://repo1.maven.org/maven2")
                )
        );
        List<GradleDependency> dependencies = List.of(
                new GradleDependency(
                        GradleDependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "foo",
                        "0.0.1"
                ),
                new GradleDependency(
                        GradleDependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "bar",
                        "0.1.0"
                )
        );

        List<GradleDependency> boms  = List.of(
                new GradleDependency(
                        GradleDependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "bom",
                        "0.1.0"
                )
        );

        Set<Coordinates> exclusions  = new HashSet<>();
        exclusions.add(new Coordinates("com.example:bar"));


        runGoldenTemplateTest("boms", repositories, dependencies, boms, exclusions);
    }

    private Path getPluginJarPath() {
        try {
            Runfiles.Preloaded runfiles = Runfiles.preload();
            // Check for Bazel 8 path
            String pluginJarPath = runfiles.withSourceRepository("rules_jvm_external")
                    .rlocation("rules_jvm_external+/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/plugin/plugin-single-jar.jar");
            if(pluginJarPath == null) {
                // Check for Bazel 7 path
                pluginJarPath = runfiles.withSourceRepository("rules_jvm_external")
                        .rlocation("rules_jvm_external~/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/plugin/plugin-single-jar.jar");
            }
            return Paths.get(pluginJarPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void runGoldenTemplateTest(String testName, List<Repository> repositories, List<GradleDependency> dependencies, List<GradleDependency> boms, Set<Coordinates> globalExclusions) throws IOException {
        // Locate template path from runfiles
        Path templatePath = Paths.get(Runfiles.preload()
                .withSourceRepository("rules_jvm_external")
                .rlocation("_main/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/data/build.gradle.kts.hbs")
        );

        List<Exclusion> exclusions = globalExclusions.stream().map(exclusion -> {
            return new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId());
        }).collect(Collectors.toList());

        // Output path
        Path outputPath = Files.createTempFile("rules_jvm_external", "gradle");

        GradleBuildScriptGenerator.generateBuildScript(
                templatePath,
                outputPath,
                getPluginJarPath(),
                repositories,
                dependencies,
                boms,
                exclusions
        );

        assertTrue(Files.exists(outputPath));

        // Load generated
        String actual = Files.readString(outputPath).trim().replace("\r\n", "\n");

        // Load expected golden file
        Path expectedPath = Paths.get(Runfiles.preload()
                .withSourceRepository("rules_jvm_external")
                .rlocation("_main/tests/com/github/bazelbuild/rules_jvm_external/resolver/gradle/testdata/" + testName + "_expected_build_gradle.kts")
        );
        String expected = Files.readString(expectedPath).trim().replace("\r\n", "\n");

        // Compare
        assertEquals(expected, actual);
    }

}
