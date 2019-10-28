package rules.jvm.external;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.sun.xml.internal.ws.api.ha.StickyFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.NoSuchElementException;

public class MavenJarMigrator {

  public static final String BUILD_WORKING_DIRECTORY = System.getenv("BUILD_WORKING_DIRECTORY");

  public static void main(String[] args) throws IOException, InterruptedException {
    ImmutableList<String> coordinates = getMavenJarArtifactCoordinates();
    System.out.println(coordinates);
  }

  private static ImmutableList<String> getMavenJarArtifactCoordinates() throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(new File(BUILD_WORKING_DIRECTORY));

    ImmutableList.Builder<String> commandBuilder = new ImmutableList.Builder<>();
    commandBuilder.add(findOnPath("bazel"));
    commandBuilder.add("query");
    commandBuilder.add("kind(maven_jar, //external:all)");
    commandBuilder.add("--output=build");
    commandBuilder.add("--noshow_progress");
    ImmutableList<String> command = commandBuilder.build();

    processBuilder.command(command);
    Process p = processBuilder.start();

    return readOutput(p)
            .stream()
            .filter((String line) -> line.trim().startsWith("artifact"))
            // e.g. artifact = "com.google.truth:truth:0.45"
            .map((String line) -> line.split("\"")[1])
            // e.g. com.google.truth:truth:0.45
            .sorted()
            .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<String> readOutput(Process p) throws InterruptedException {
    p.waitFor();
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    return br.lines().collect(ImmutableList.toImmutableList());
  }

  //  bazel query 'kind(maven_jar, //external:all)' --output=build --noshow_progress 2>/dev/null | grep artifact | awk '{print $3}' | sort | sed 's/^/        /;' 2>/dev/null
  private static String findOnPath(String bin) {
    List<String> pathElements = Splitter.on(File.pathSeparator).splitToList(System.getenv("PATH"));
    for (String pathElement : pathElements) {
      File maybePath = new File(pathElement, bin);
      if (maybePath.canExecute()) {
        return maybePath.toString();
      }
    }
    throw new NoSuchElementException("Could not find bazel on your PATH");
  }

}
