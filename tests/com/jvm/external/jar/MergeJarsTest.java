package com.jvm.external.jar;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rules.jvm.external.jar.MergeJars;
import rules.jvm.external.zip.StableZipEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MergeJarsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldGenerateAnEmptyJarIfNoSourcesAreGiven() throws IOException {
    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{"--output", outputJar.toAbsolutePath().toString()});

    assertTrue(Files.exists(outputJar));
    assertTrue(Files.size(outputJar) > 0);
  }

  @Test
  public void shouldGenerateAJarContainingAllTheClassesFromASingleSource() throws IOException {
    Path inputOne = temp.newFile("first.jar").toPath();
    createJar(inputOne, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", inputOne.toAbsolutePath().toString()});

    Map<String, String> contents = readJar(outputJar);
    // We expect the manifest and one file
    assertEquals(2, contents.size());
    assertEquals("Hello, World!", contents.get("com/example/A.class"));
  }

  @Test
  public void shouldMergeMultipleSourceJarsIntoASingleOutputJar() throws IOException {
    Path inputOne = temp.newFile("first.jar").toPath();
    createJar(inputOne, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path inputTwo = temp.newFile("second.jar").toPath();
    createJar(inputTwo, ImmutableMap.of("com/example/foo/B.class", "Also hello"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", inputOne.toAbsolutePath().toString(),
      "--sources", inputTwo.toAbsolutePath().toString()});

    Map<String, String> contents = readJar(outputJar);
    // We expect the manifest and one file
    assertEquals(3, contents.size());
    assertEquals("Hello, World!", contents.get("com/example/A.class"));
    assertEquals("Also hello", contents.get("com/example/foo/B.class"));
  }

  @Test
  public void shouldAllowDuplicateClassesByDefaultAndLastOneInWins() throws IOException {
    // Create jars with names such that the first is sorted after the second
    Path inputOne = temp.newFile("beta.jar").toPath();
    createJar(inputOne, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path inputTwo = temp.newFile("alpha.jar").toPath();
    createJar(inputTwo, ImmutableMap.of("com/example/A.class", "Farewell!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", inputOne.toAbsolutePath().toString(),
      "--sources", inputTwo.toAbsolutePath().toString()});

    Map<String, String> contents = readJar(outputJar);
    // We expect the manifest and one file
    assertEquals(2, contents.size());
    assertEquals("Farewell!", contents.get("com/example/A.class"));
  }

  @Test
  public void shouldBeAbleToSpecifyThatFirstSeenClassShouldBeIncludedInMergedJar() throws IOException {
    // Create jars with names such that the first is sorted after the second
    Path inputOne = temp.newFile("beta.jar").toPath();
    createJar(inputOne, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path inputTwo = temp.newFile("alpha.jar").toPath();
    createJar(inputTwo, ImmutableMap.of("com/example/A.class", "Farewell!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", inputOne.toAbsolutePath().toString(),
      "--sources", inputTwo.toAbsolutePath().toString(),
      "--duplicates", "first-wins"});

    Map<String, String> contents = readJar(outputJar);
    // We expect the manifest and one file
    assertEquals(2, contents.size());
    assertEquals("Hello, World!", contents.get("com/example/A.class"));
  }

  @Test(expected = IOException.class)
  public void duplicateClassesCanBeDeclaredAsErrors() throws IOException {
    // Create jars with names such that the first is sorted after the second
    Path inputOne = temp.newFile("beta.jar").toPath();
    createJar(inputOne, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path inputTwo = temp.newFile("alpha.jar").toPath();
    createJar(inputTwo, ImmutableMap.of("com/example/A.class", "Farewell!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", inputOne.toAbsolutePath().toString(),
      "--sources", inputTwo.toAbsolutePath().toString(),
      "--duplicates", "are-errors"});
  }

  @Test
  public void identicalDuplicateClassesAreFine() throws IOException {
    // Create jars with names such that the first is sorted after the second
    Path inputOne = temp.newFile("beta.jar").toPath();
    createJar(inputOne, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path inputTwo = temp.newFile("alpha.jar").toPath();
    createJar(inputTwo, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", inputOne.toAbsolutePath().toString(),
      "--sources", inputTwo.toAbsolutePath().toString(),
      "--duplicates", "are-errors"});

    Map<String, String> contents = readJar(outputJar);
    // We expect the manifest and one file
    assertEquals(2, contents.size());
    assertEquals("Hello, World!", contents.get("com/example/A.class"));
  }

  @Test
  public void shouldUseDifferentTimesForSourceAndClassFiles() throws IOException {
    Path inputOne = temp.newFile("first.jar").toPath();
    createJar(inputOne, new ImmutableMap.Builder<String, String>()
      .put("com/example/A.class", "Hello, Class!")
      .put("com/example/A.java", "Hello, Source!")
      .build());

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", inputOne.toAbsolutePath().toString()});

    Map<String, Long> entryTimestamps = readJarTimeStamps(outputJar);
    assertEquals(3, entryTimestamps.size());
    assertTrue(entryTimestamps.get("com/example/A.class") > entryTimestamps.get("com/example/A.java"));
  }

  @Test
  public void shouldBeAbleToExcludeClassesFromMergedJar() throws IOException {
    // Create jars with names such that the first is sorted after the second
    Path includeFrom = temp.newFile("include.jar").toPath();
    createJar(
      includeFrom,
      ImmutableMap.of(
        "com/example/A.class", "Hello, World!",
        "com/example/B.class", "I like cheese!"));

    Path excludeFrom = temp.newFile("exclude.jar").toPath();
    createJar(excludeFrom, ImmutableMap.of("com/example/A.class", "Something else!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", includeFrom.toAbsolutePath().toString(),
      "--exclude", excludeFrom.toAbsolutePath().toString()});

    Map<String, String> contents = readJar(outputJar);
    // We expect the manifest and one file
    assertEquals(2, contents.size());
    assertEquals("I like cheese!", contents.get("com/example/B.class"));
  }

  @Test
  public void shouldNotIncludeManifestOrMetaInfEntriesFromExclusions() throws IOException {
    // Create jars with names such that the first is sorted after the second
    Path includeFrom = temp.newFile("include.jar").toPath();
    createJar(
      includeFrom,
      ImmutableMap.of(
        "META-INF/foo", "Hello, World!"));

    Path excludeFrom = temp.newFile("exclude.jar").toPath();
    createJar(excludeFrom, ImmutableMap.of("META-INF/foo", "Something else!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    MergeJars.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--sources", includeFrom.toAbsolutePath().toString(),
      "--exclude", excludeFrom.toAbsolutePath().toString()});

    Map<String, String> contents = readJar(outputJar);
    // We expect the manifest and the one meta inf entry
    assertEquals(2, contents.size());
    assertEquals("Hello, World!", contents.get("META-INF/foo"));
  }

  private void createJar(Path outputTo, Map<String, String> pathToContents) throws IOException {
    try (OutputStream os = Files.newOutputStream(outputTo);
         ZipOutputStream zos = new ZipOutputStream(os)) {

      for (Map.Entry<String, String> entry : pathToContents.entrySet()) {
        ZipEntry ze = new StableZipEntry(entry.getKey());
        zos.putNextEntry(ze);
        zos.write(entry.getValue().getBytes(UTF_8));
        zos.closeEntry();
      }
    }
  }

  private Map<String, String> readJar(Path jar) throws IOException {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    try (InputStream is = Files.newInputStream(jar);
         ZipInputStream zis = new ZipInputStream(is)) {

      for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
        if (entry.isDirectory()) {
          continue;
        }

        builder.put(entry.getName(), new String(ByteStreams.toByteArray(zis), UTF_8));
      }
    }

    return builder.build();
  }

  private Map<String, Long> readJarTimeStamps(Path jar) throws IOException {
    ImmutableMap.Builder<String, Long> builder = ImmutableMap.builder();

    try (InputStream is = Files.newInputStream(jar);
         ZipInputStream zis = new ZipInputStream(is)) {

      for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
        if (entry.isDirectory()) {
          continue;
        }

        builder.put(entry.getName(), entry.getTime());
      }
    }

    return builder.build();
  }
}
