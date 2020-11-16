package com.jvm.external.jar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rules.jvm.external.jar.AddJarManifestEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AddJarManifestEntryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldAddEntryToManifest() throws IOException {
    Path inputOne = temp.newFile("first.jar").toPath();

    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(new Attributes.Name("Hello-World"), "hello");

    createJar(inputOne, manifest, new ImmutableMap.Builder<String, String>()
           .put("com/example/A.class", "Hello, World!")
           .put("com/example/B.class", "Hello, World Again!")
           .build());

    Path outputJar = temp.newFile("out.jar").toPath();

    AddJarManifestEntry.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--source", inputOne.toAbsolutePath().toString(),
      "--manifest-entry", "Target-Label:@maven//:com_google_guava_guava"});

    Map<String, String> contents = readJar(outputJar);
    assertEquals(3, contents.size());
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Manifest-Version: 1.0"));
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Hello-World: hello"));
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Target-Label: @maven//:com_google_guava_guava"));
  }

  @Test
  public void shouldCreateManifestIfManifestIsMissing() throws IOException {
    Path inputOne = temp.newFile("first.jar").toPath();
    createJar(inputOne, null, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    AddJarManifestEntry.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--source", inputOne.toAbsolutePath().toString(),
      "--manifest-entry", "Target-Label:@maven//:com_google_guava_guava"});

    Map<String, String> contents = readJar(outputJar);
    assertEquals(2, contents.size());
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Manifest-Version: 1.0"));
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Target-Label: @maven//:com_google_guava_guava"));
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Created-By: AddJarManifestEntry"));
  }

  @Test
  public void shouldCreateManifestAsFirstEntryInJarIfManifestIsMissing() throws IOException {
    Path inputOne = temp.newFile("first.jar").toPath();
    createJar(inputOne, null, ImmutableMap.of("com/example/A.class", "Hello, World!"));

    Path outputJar = temp.newFile("out.jar").toPath();

    AddJarManifestEntry.main(new String[]{
            "--output", outputJar.toAbsolutePath().toString(),
            "--source", inputOne.toAbsolutePath().toString(),
            "--manifest-entry", "Target-Label:@maven//:com_google_guava_guava"});

    List<String> entries = readJarEntries(outputJar);
    assertEquals(2, entries.size());
    assertEquals("META-INF/MANIFEST.MF", entries.get(0));
    assertEquals("com/example/A.class", entries.get(1));
  }

  @Test
  public void shouldOverwriteManifestEntryIfItAlreadyExistsInJar() throws IOException {
    Path inputOne = temp.newFile("first.jar").toPath();

    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(new Attributes.Name("Target-Label"), "@maven//:org_hamcrest_hamcrest");

    createJar(inputOne, manifest, new ImmutableMap.Builder<String, String>()
           .put("com/example/A.class", "Hello, World!")
           .build());

    Path outputJar = temp.newFile("out.jar").toPath();

    AddJarManifestEntry.main(new String[]{
      "--output", outputJar.toAbsolutePath().toString(),
      "--source", inputOne.toAbsolutePath().toString(),
      "--manifest-entry", "Target-Label:@maven//:com_google_guava_guava"});

    Map<String, String> contents = readJar(outputJar);
    assertEquals(2, contents.size());
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Manifest-Version: 1.0"));
    assertTrue(contents.get("META-INF/MANIFEST.MF").contains("Target-Label: @maven//:com_google_guava_guava"));
  }

  private void createJar(Path outputTo, Manifest manifest, Map<String, String> pathToContents) throws IOException {
    try (OutputStream os = Files.newOutputStream(outputTo);
         ZipOutputStream zos = new JarOutputStream(os)) {

      if (manifest != null) {
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        ZipEntry manifestEntry = new ZipEntry(JarFile.MANIFEST_NAME);
        zos.putNextEntry(manifestEntry);
        manifest.write(zos);
      }

      for (Map.Entry<String, String> entry : pathToContents.entrySet()) {
        ZipEntry ze = new ZipEntry(entry.getKey());
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

  private List<String> readJarEntries(Path jar) throws IOException {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    try (InputStream is = Files.newInputStream(jar);
         ZipInputStream zis = new ZipInputStream(is)) {
      for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
        if (entry.isDirectory()) {
          continue;
        }
        builder.add(entry.getName());
      }
    }
    return builder.build();
  }
}
