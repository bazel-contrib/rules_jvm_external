package com.github.bazelbuild.rules_jvm_external.coursier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class Pin {
  private static class PinOptions {
    private String unsortedDepsFile;
    private String mavenInstallLocation;
    private String predefinedMavenInstall;
    private String repositoryName;

    private static PinOptions parse(String[] args) {
      PinOptions options = new PinOptions();

      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--unsorted-deps-file":
            options.unsortedDepsFile = args[++i];
            break;
          case "--maven-install-location":
            options.mavenInstallLocation = args[++i];
            break;
          case "--predefined-maven-install":
            options.predefinedMavenInstall = args[++i];
            break;
          case "--repository-name":
            options.repositoryName = args[++i];
            break;
          default:
            throw new IllegalArgumentException(
                "Unable to parse command line: " + Arrays.toString(args));
        }
      }

      if (options.unsortedDepsFile == null) {
        throw new IllegalArgumentException("--unsorted-deps-file is required");
      }
      if (options.mavenInstallLocation == null) {
        throw new IllegalArgumentException("--maven-install-location is required");
      }
      if (options.predefinedMavenInstall == null) {
        throw new IllegalArgumentException("--predefined-maven-install is required");
      }
      if (options.repositoryName == null) {
        throw new IllegalArgumentException("--repository-name is required");
      }

      return options;
    }
  }

  private static Path workspaceRoot() {
    String workspaceRoot = System.getenv("BUILD_WORKSPACE_DIRECTORY");
    if (workspaceRoot == null || workspaceRoot.isEmpty()) {
      workspaceRoot = System.getProperty("user.dir");
    }
    return Paths.get(workspaceRoot);
  }

  private static void printPostPinMessage(
      String repositoryName, Path outputFile, boolean predefinedMavenInstall) {
    if (predefinedMavenInstall) {
      System.out.println(
          String.format(
              "Successfully pinned resolved artifacts for @%s, %s is now up-to-date.",
              repositoryName, outputFile));
    } else {
      System.out.println(
          String.format(
              "Successfully pinned resolved artifacts for @%s in %s."
                  + " This file should be checked into your version control system.",
              repositoryName, outputFile));
      System.out.println();
      System.out.println(
          String.format(
              "Next, please update your WORKSPACE file by adding the maven_install_json attribute"
                  + " and loading pinned_maven_install from @%s//:defs.bzl.",
              repositoryName));
      System.out.println();
      System.out.println("For example:");
      System.out.println();
      System.out.println("=============================================================");
      System.out.println();
      System.out.println("maven_install(");
      System.out.println("    artifacts = # ...,");
      System.out.println("    repositories = # ...,");
      System.out.println(
          String.format("    maven_install_json = \"@//:%s_install.json\",", repositoryName));
      System.out.println(")");
      System.out.println();
      System.out.println(
          String.format(
              "load(\"@%s//:defs.bzl\", \"pinned_maven_install\")", repositoryName));
      System.out.println("pinned_maven_install()");
      System.out.println();
      System.out.println("=============================================================");
      System.out.println();
      System.out.println(
          String.format(
              "To update %s_install.json, run this command to re-pin the unpinned repository:",
              repositoryName));
      System.out.println();
      System.out.println(String.format("    bazel run @unpinned_%s//:pin", repositoryName));
    }
    System.out.println();
  }

  public static void main(String[] args) throws IOException {
    PinOptions options = PinOptions.parse(args);

    Path unsortedDepsPath = RunfilesPaths.resolvePath(options.unsortedDepsFile);
    if (!Files.exists(unsortedDepsPath)) {
      throw new IllegalArgumentException(
          "Failed to locate the unsorted_deps.json file: " + options.unsortedDepsFile);
    }

    Path outputPath = Paths.get(options.mavenInstallLocation);
    if (!outputPath.isAbsolute()) {
      outputPath = workspaceRoot().resolve(outputPath);
    }

    Path outputParent = outputPath.getParent();
    if (outputParent != null) {
      Files.createDirectories(outputParent);
    }

    Files.copy(unsortedDepsPath, outputPath, StandardCopyOption.REPLACE_EXISTING);

    printPostPinMessage(
        options.repositoryName,
        outputPath,
        Boolean.parseBoolean(options.predefinedMavenInstall));
  }
}
