package rules.jvm.external;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/** A migration tool from native.maven_jar to rules_jvm_external's maven_install. */
public class MavenJarMigrator {

  // Defined by Bazel during a `bazel run`.
  private static final String BUILD_WORKING_DIRECTORY = System.getenv("BUILD_WORKING_DIRECTORY");
  // Converts a Maven coordinate to a valid maven_install target label.
  // e.g. com.google.guava:guava:28.0-jre -> @maven//:com_google_guava_guava
  private static final Function<String, String> convertCoordinateToNewLabel =
      (String coordinate) -> {
        coordinate = coordinate.replace("-", "_").replace(".", "_");
        String[] parts = coordinate.split(":");
        return "@maven//:" + Joiner.on("_").join(parts[0], parts[1]);
      };

  public static void main(String[] args) throws IOException, InterruptedException {
    checkPrerequisites();

    ImmutableList<String> coordinates = collectMavenJarAttributeValues("artifact");
    // Phase 1: Run buildozer to convert old maven_jar labels to new maven_install labels.
    convertLabels(coordinates);
    // Phase 2: Print maven_install WORKSPACE snippet on stdout.
    System.out.println(generateWorkspaceSnippet(coordinates));
  }

  private static void checkPrerequisites() throws InterruptedException, IOException {
    // Assert that buildozer exists on PATH.
    if (getProcessBuilder("buildozer", "-version").start().waitFor() != 0) {
      throw new AssertionError("buildozer is not found on your PATH. Download "
          + "buildozer for your platform from https://github.com/bazelbuild/buildtools/releases "
          + "and put the executable in your PATH.");
    }
  }

  /**
   * Converts the old style maven_jar labels to the new style maven_install labels.
   *
   * <p>This implementation assumes that the sortedness output of bazel query --output=build is
   * deterministic.
   *
   * @param coordinates A list of coordinates to derive the old and new labels.
   * @throws IOException
   * @throws InterruptedException
   */
  private static void convertLabels(ImmutableList<String> coordinates)
      throws IOException, InterruptedException {
    ImmutableList<String> oldLabels =
        collectMavenJarAttributeValues("name").stream()
            .map((String name) -> String.format("@%s//jar", name))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<String> newLabels =
        coordinates.stream()
            .map(convertCoordinateToNewLabel)
            .collect(ImmutableList.toImmutableList());

    assert (newLabels.size() == oldLabels.size());
    Iterator<String> oldLabelIterator = oldLabels.iterator();
    Iterator<String> newLabelIterator = newLabels.iterator();
    while (oldLabelIterator.hasNext()) {
      getProcessBuilder(
              "buildozer",
              "substitute * " + oldLabelIterator.next() + " " + newLabelIterator.next(),
              "//...:*")
          .start()
          .waitFor();
    }
  }

  private static String generateWorkspaceSnippet(ImmutableList<String> coordinates)
      throws IOException {
    InputStream stream =
        MavenJarMigrator.class.getResourceAsStream("resources/workspace_template.txt");
    String workspaceTemplate = new String(ByteStreams.toByteArray(stream), UTF_8);
    return workspaceTemplate.replace(
        "%maven_artifact_list%",
        coordinates.stream()
            .sorted()
            .map((String line) -> "        \"" + line + "\",")
            .collect(Collectors.joining("\n")));
  }

  /**
   * Aggregate information about an attribute on all maven_jar targets using bazel query.
   *
   * <p>The maven_jar declaration does not need to be in the local WORKSPACE. It can be a maven_jar
   * declaration loaded from a remote repository's deps.bzl.
   *
   * @return A list of Maven artifact coordinates used by this workspace.
   * @throws IOException
   * @throws InterruptedException
   */
  private static ImmutableList<String> collectMavenJarAttributeValues(String attribute)
      throws IOException, InterruptedException {
    ProcessBuilder processBuilder =
        getProcessBuilder(
            "bazel",
            "query",
            "kind(maven_jar, //external:all)",
            "--output=build",
            "--noshow_progress");

    return readOutput(processBuilder.start()).stream()
        .filter((String line) -> line.trim().startsWith(attribute))
        .map((String line) -> line.split("\"")[1]) // get the value in a string attribute
        .collect(ImmutableList.toImmutableList());
  }

  private static ProcessBuilder getProcessBuilder(String... args) {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(new File(BUILD_WORKING_DIRECTORY));
    processBuilder.command(args);
    return processBuilder;
  }

  private static ImmutableList<String> readOutput(Process p) throws InterruptedException {
    // ensure that the process is completed before reading the standard output.
    p.waitFor();
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    return br.lines().collect(ImmutableList.toImmutableList());
  }
}
