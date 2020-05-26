package com.jvm.external.jvm_export;

import com.google.common.io.ByteStreams;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.StandardSystemProperty.OS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class PublishShapeTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @BeforeClass
    public static void checkPlatform() {
        assumeFalse(OS_NAME.value().toLowerCase().contains("window"));
    }

    @Test
    public void publishingToAFileEndPointLooksCorrect() throws IOException, InterruptedException {
        // Ideally, we'd use `bazel run` to run this test, but that's not
        //possible. Instead, we'll invoke the publisher directly.

        Path repoRoot = publish("com.example:my-lib:1.0.0");

        // Check that the files are where we expect them to be
        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0.jar")));
        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0.jar.md5")));
        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0.jar.sha1")));

        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0-sources.jar")));
        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0-sources.jar.md5")));
        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0-sources.jar.sha1")));

        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0-javadoc.jar")));
        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0-javadoc.jar.md5")));
        assertTrue(Files.exists(repoRoot.resolve("com/example/my-lib/1.0.0/my-lib-1.0.0-javadoc.jar.sha1")));
    }

    private Path publish(String coordinates) throws IOException, InterruptedException {
        Path deployJar = Paths.get(System.getProperty("deploy.jar"));
        assertTrue("Unable to find publish deploy jar: " + deployJar, Files.exists(deployJar));

        // Run the publisher by hand
        File repoRoot = temp.newFolder("m2repo");

        // The publisher doesn't validate inputs (though remote maven repos do)
        // so we'll stub out the bits we need.
        File stubJar = temp.newFile("dummy.jar");
        File pomXml = temp.newFile("pom.xml");

        // We'd prefer to use `bazel run`, but this is a reasonable proxy for
        // ./uploader {maven_repo} {gpg_sign} {user} {password} {coordinates} pom.xml artifact.jar source.jar doc.jar
        Process process = new ProcessBuilder()
                .command(
                        "java",
                        "-jar",
                        deployJar.toAbsolutePath().toString(),
                        repoRoot.toURI().toASCIIString(),
                        "false", // No gpg signing
                        "", // User name
                        "", // Password,
                        coordinates,
                        pomXml.getAbsolutePath(),
                        stubJar.getAbsolutePath(),
                        stubJar.getAbsolutePath(),
                        stubJar.getAbsolutePath())
                .redirectErrorStream(true)
                .start();

        process.waitFor();

        // Grab the output, just in case
        String output = new String(ByteStreams.toByteArray(process.getInputStream()));

        assertEquals(output, 0, process.exitValue());

        return repoRoot.toPath();
    }
}
