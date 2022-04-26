package com.jvm.external.maven;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import rules.jvm.external.maven.Outdated;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class OutdatedTest {
  private final PrintStream originalOut = System.out;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldFindUpdatedVersionForGuava() throws IOException {
    Path artifactsFile = temp.newFile("outdated.artifacts").toPath();
    Files.write(artifactsFile, Arrays.asList("com.google.guava:guava:27.0-jre"), StandardCharsets.UTF_8);

    Path repositoriesFile = temp.newFile("outdated.repositories").toPath();
    Files.write(repositoriesFile, Arrays.asList("https://repo1.maven.org/maven2"), StandardCharsets.UTF_8);

    ByteArrayOutputStream outdatedOutput = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(outdatedOutput));

      Outdated.main(new String[]{
          artifactsFile.toAbsolutePath().toString(),
          repositoriesFile.toAbsolutePath().toString()});
    } finally {
      System.setOut(originalOut);
    }

    assertThat(outdatedOutput.toString(), containsString("Checking for updates of 1 artifacts against the following repositories"));
    assertThat(outdatedOutput.toString(), containsString("https://repo1.maven.org/maven2"));
    assertThat(outdatedOutput.toString(), containsString("com.google.guava:guava [27.0-jre -> "));
    assertThat(outdatedOutput.toString(), not(containsString("No updates found")));
  }

  @Test
  public void shouldPrintNoUpdatesIfInputFileIsEmpty() throws IOException {
    Path artifactsFile = temp.newFile("outdated.artifacts").toPath();
    Files.write(artifactsFile, Arrays.asList(""), StandardCharsets.UTF_8);

    Path repositoriesFile = temp.newFile("outdated.repositories").toPath();
    Files.write(repositoriesFile, Arrays.asList("https://repo1.maven.org/maven2"), StandardCharsets.UTF_8);

    ByteArrayOutputStream outdatedOutput = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(outdatedOutput));

      Outdated.main(new String[]{
          artifactsFile.toAbsolutePath().toString(),
          repositoriesFile.toAbsolutePath().toString()});
    } finally {
      System.setOut(originalOut);
    }

    assertThat(outdatedOutput.toString(), containsString("Checking for updates of 1 artifacts against the following repositories"));
    assertThat(outdatedOutput.toString(), containsString("https://repo1.maven.org/maven2"));
    assertThat(outdatedOutput.toString(), containsString("No updates found"));
  }

  @Test
  public void shouldWorkWithMultipleArtifactsAndRepositories() throws IOException {
    Path artifactsFile = temp.newFile("outdated.artifacts").toPath();
    Files.write(artifactsFile, Arrays.asList("com.google.guava:guava:27.0-jre", "junit:junit:4.12", "com.squareup:javapoet:1.11.1"), StandardCharsets.UTF_8);

    Path repositoriesFile = temp.newFile("outdated.repositories").toPath();
    Files.write(repositoriesFile, Arrays.asList("https://repo1.maven.org/maven2", "https://maven.google.com"), StandardCharsets.UTF_8);

    ByteArrayOutputStream outdatedOutput = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(outdatedOutput));

      Outdated.main(new String[]{
          artifactsFile.toAbsolutePath().toString(),
          repositoriesFile.toAbsolutePath().toString()});
    } finally {
      System.setOut(originalOut);
    }

    assertThat(outdatedOutput.toString(), containsString("Checking for updates of 3 artifacts against the following repositories"));
    assertThat(outdatedOutput.toString(), containsString("https://repo1.maven.org/maven2"));
    assertThat(outdatedOutput.toString(), containsString("https://maven.google.com"));
    assertThat(outdatedOutput.toString(), containsString("com.google.guava:guava [27.0-jre -> "));
    assertThat(outdatedOutput.toString(), containsString("junit:junit [4.12 -> "));
    assertThat(outdatedOutput.toString(), containsString("com.squareup:javapoet [1.11.1 -> "));
  }

  private static Document getTestDocument(String name) {
    String resourceName = "tests/com/jvm/external/maven/" + name;
    try (InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      return documentBuilder.parse(in);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  // https://github.com/bazelbuild/rules_jvm_external/issues/507
  @Test
  public void shouldWorkForAnArtifactMissingReleaseMetadata() throws IOException {
    Document testDocument = getTestDocument("maven-metadata-javax-inject.xml");
    assertThat(Outdated.getReleaseVersion(testDocument, "testDoc"), equalTo("1"));
  }

  // https://github.com/bazelbuild/rules_jvm_external/issues/507
  @Test
  public void grabsLastVersionWhenArtifactMissingReleaseMetadata() throws IOException {
    Document testDocument = getTestDocument("maven-metadata-multiple-versions.xml");
    assertThat(Outdated.getReleaseVersion(testDocument, "testDoc"), equalTo("2"));
  }
}
