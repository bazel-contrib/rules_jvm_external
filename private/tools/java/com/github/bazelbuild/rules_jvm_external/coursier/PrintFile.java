package com.github.bazelbuild.rules_jvm_external.coursier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PrintFile {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expected exactly one argument: <path-to-file>");
    }

    Path filePath = RunfilesPaths.resolvePath(args[0]);
    List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
    for (String line : lines) {
      if (!line.isEmpty()) {
        System.out.println(line);
      }
    }
  }
}
