package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependency;
import com.google.devtools.build.runfiles.Runfiles;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GradleBuildScriptTemplateTest {

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
        runGoldenTemplateTest("simple", repositories, dependencies, boms);
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
        runGoldenTemplateTest("multipleRepositories", repositories, dependencies, boms);
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

        runGoldenTemplateTest("boms", repositories, dependencies, boms);
    }

    private void runGoldenTemplateTest(String testName, List<Repository> repositories, List<GradleDependency> dependencies, List<GradleDependency> boms) throws IOException {
        // Locate template path from runfiles
        Path templatePath = Paths.get(Runfiles.preload()
                .withSourceRepository("rules_jvm_external")
                .rlocation("_main/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/data/build.gradle.kts.hbs")
        );

        // Output path
        Path outputPath = Files.createTempFile("rules_jvm_external", "gradle");

        GradleBuildScriptTemplate.generateBuildGradleKts(
                templatePath,
                outputPath,
                repositories,
                boms,
                dependencies
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
