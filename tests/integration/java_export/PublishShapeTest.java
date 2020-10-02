package com.jvm.external.jvm_export;

import com.google.common.io.ByteStreams;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.StandardSystemProperty.OS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class PublishShapeTest {
    // Jar content is fake, but chosen to produce leading zero in the checksum
    // to be sure leading zeroes aren't omitted
    private static final String JAR_CONTENTS = "magic!";
    private static final String JAR_MD5 = "05427eba78c92912c86d004b9857d6a0";
    private static final String JAR_SHA1 = "cbb0126a346a4dd6694fc48e3a94174fd1c7fa93";

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
        checkJarShape(repoRoot, "com/example/my-lib/1.0.0/my-lib-1.0.0.jar");
        checkJarShape(repoRoot, "com/example/my-lib/1.0.0/my-lib-1.0.0-sources.jar");
        checkJarShape(repoRoot, "com/example/my-lib/1.0.0/my-lib-1.0.0-javadoc.jar");
    }

    private void checkJarShape(Path repoRoot, String path) throws IOException {
        assertTrue(Files.exists(repoRoot.resolve(path)));
        assertTrue(Files.exists(repoRoot.resolve(path + ".md5")));
        assertTrue(Files.exists(repoRoot.resolve(path + ".sha1")));

        // Basic checksum format check
        String md5 = new String(Files.readAllBytes(repoRoot.resolve(path + ".md5")));
        assertEquals(32, md5.length());
        String sha1 = new String(Files.readAllBytes(repoRoot.resolve(path + ".sha1")));
        assertEquals(40, sha1.length());

        // Check checksum values
        assertEquals(JAR_MD5, md5);
        assertEquals(JAR_SHA1, sha1);
    }

    private Path publish(String coordinates) throws IOException, InterruptedException {
        Path deployJar = Paths.get(System.getProperty("deploy.jar"));
        assertTrue("Unable to find publish deploy jar: " + deployJar, Files.exists(deployJar));

        // Run the publisher by hand
        File repoRoot = temp.newFolder("m2repo");

        // The publisher doesn't validate inputs (though remote maven repos do)
        // so we'll stub out the bits we need.
        File stubJar = writeFile("dummy.jar", JAR_CONTENTS);
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

    private File writeFile(String name, String contents) throws IOException {
        File file = temp.newFile(name);

        try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
            out.print(contents);
        }
        return file;
    }
}
