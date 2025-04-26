package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import com.google.devtools.build.runfiles.Runfiles;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GradleBuildScriptTemplateTest {
    private Path templatePath;

    @Before
    public void setup() throws IOException {
        templatePath = Paths.get(Runfiles.preload()
                .withSourceRepository("rules_jvm_external")
                .rlocation("_main/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/data/build.gradle.kts.hbs")
        );
    }

    @Test
    public void shouldRenderValidBuildScriptWithDeps() throws IOException {
        List<Repository> repositories = List.of(
                new Repository(
                        "https://repo1.maven.org/maven2"
                )
        );
        List<Dependency> dependencies = List.of(
                new Dependency(
                        Dependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "foo",
                        "0.0.1"
                ),
                new Dependency(
                        Dependency.Scope.IMPLEMENTATION,
                        "com.example",
                        "bar",
                        "0.1.0"
                )
        );

        List<Dependency> boms  = List.of();
        Path outputPath = Files.createTempFile("rules_jvm_external", "gradle");
        runGoldenTemplateTest("simple", repositories, dependencies, boms);
    }

    private void runGoldenTemplateTest(String testName, List<Repository> repositories, List<Dependency> dependencies, List<Dependency> boms) throws IOException {
        // Locate template path dynamically
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
                .rlocation("_main/tests/com/github/bazelbuild/rules_jvm_external/resolver/gradle/testdata/" + testName + "_expected_build.gradle.kts")
        );
        String expected = Files.readString(expectedPath).trim().replace("\r\n", "\n");

        // Compare
        assertEquals(expected, actual);
    }

}
