package rules.jvm.external;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/** A migration tool from native.maven_jar to rules_jvm_external's maven_install. */
public class MavenJarMigrator {

  // Defined by Bazel during a `bazel run`.
  private static final String BUILD_WORKING_DIRECTORY = System.getenv("BUILD_WORKING_DIRECTORY");

  // Converts a Maven coordinate to a valid maven_install target label.
  // e.g. com.google.guava:guava:28.0-jre -> @maven//:com_google_guava_guava
  private static final Function<String, String> convertCoordinateToLabel =
      (String coordinate) -> {
        coordinate = coordinate.replace("-", "_").replace(".", "_");
        String[] parts = coordinate.split(":");
        return "@maven//:" + Joiner.on("_").join(parts[0], parts[1]);
      };

  public static void main(String[] args) throws IOException, InterruptedException {
    ImmutableList<String> coordinates = queryMavenJarsOnStringAttribute("artifact");
    buildozerMavenJarToMavenInstall(coordinates);
    System.out.println(generateWorkspaceSnippet(coordinates));
  }

  private static void buildozerMavenJarToMavenInstall(ImmutableList<String> coordinates)
      throws IOException, InterruptedException {
    ImmutableList<String> mavenInstallLabels =
        coordinates.stream().map(convertCoordinateToLabel).collect(ImmutableList.toImmutableList());
    ImmutableList<String> names =
        queryMavenJarsOnStringAttribute("name").stream()
            .map((String name) -> String.format("@%s//jar", name))
            .collect(ImmutableList.toImmutableList());
    assert (mavenInstallLabels.size() == names.size());
    HashMap<String, String> nameToLabel =
        IntStream.range(0, names.size())
            .collect(
                HashMap::new,
                (map, index) -> map.put(names.get(index), mavenInstallLabels.get(index)),
                Map::putAll);

    for (Map.Entry<String, String> nameAndLabel : nameToLabel.entrySet()) {
      String name = nameAndLabel.getKey();
      String label = nameAndLabel.getValue();
      ProcessBuilder p =
          getProcessBuilder("buildozer", "substitute * " + name + " " + label, "//...:*");
      p.start().waitFor();
    }
  }

  private static String generateWorkspaceSnippet(ImmutableList<String> coordinates)
      throws IOException, InterruptedException {
    InputStream stream =
        MavenJarMigrator.class.getResourceAsStream("resources/workspace_template.txt");
    String workspaceTemplate = new String(ByteStreams.toByteArray(stream), UTF_8);
    String workspaceSnippet =
        workspaceTemplate.replace(
            "%maven_artifact_list%",
            coordinates.stream()
                .sorted()
                .map((String line) -> "        \"" + line + "\",")
                .collect(Collectors.joining("\n")));
    return workspaceSnippet;
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
  private static ImmutableList<String> queryMavenJarsOnStringAttribute(String attribute)
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
        .map((String line) -> line.split("\"")[1])
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
