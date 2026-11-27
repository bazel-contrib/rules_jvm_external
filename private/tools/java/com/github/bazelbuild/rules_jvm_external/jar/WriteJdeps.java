package com.github.bazelbuild.rules_jvm_external.jar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Writes a `.jdeps` file (a serialized `blaze_deps.Dependencies` message, see
 * https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/deps.proto) for an imported
 * jar.
 *
 * <p>Bazel uses the `.jdeps` files of a target's direct dependencies to compute the reduced
 * classpath when `--experimental_java_classpath` is `javabuilder` or `bazel`. Without one, the jars
 * an imported jar needs to be compiled against (e.g. the supertypes of its public classes) are
 * pruned from the classpath of its dependents, and compilation falls back to the full classpath.
 *
 * <p>The message is encoded by hand to avoid depending on protobuf at runtime.
 */
public class WriteJdeps {

  // Field numbers and enum values from `deps.proto`.
  private static final int DEPENDENCIES_DEPENDENCY = 1;
  private static final int DEPENDENCIES_RULE_LABEL = 2;
  private static final int DEPENDENCIES_SUCCESS = 3;
  private static final int DEPENDENCY_PATH = 1;
  private static final int DEPENDENCY_KIND = 2;
  private static final int KIND_EXPLICIT = 0;

  private static final int WIRE_TYPE_VARINT = 0;
  private static final int WIRE_TYPE_LENGTH_DELIMITED = 2;

  public static void main(String[] args) throws IOException {
    Path output = null;
    String label = null;
    List<String> paths = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--output":
          output = Paths.get(args[++i]);
          break;

        case "--label":
          label = args[++i];
          break;

        case "--dependency":
          paths.add(args[++i]);
          break;

        default:
          throw new IllegalArgumentException(
              "Unable to parse command line: " + Arrays.toString(args));
      }
    }

    Objects.requireNonNull(output, "Output path must be set.");
    Objects.requireNonNull(label, "Label must be set.");

    try (OutputStream out = Files.newOutputStream(output)) {
      out.write(encode(label, paths));
    }
  }

  // Visible for testing
  static byte[] encode(String label, List<String> paths) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (String path : paths) {
      ByteArrayOutputStream dependency = new ByteArrayOutputStream();
      writeString(dependency, DEPENDENCY_PATH, path);
      writeVarintField(dependency, DEPENDENCY_KIND, KIND_EXPLICIT);
      writeBytes(out, DEPENDENCIES_DEPENDENCY, dependency.toByteArray());
    }
    writeString(out, DEPENDENCIES_RULE_LABEL, label);
    writeVarintField(out, DEPENDENCIES_SUCCESS, 1);
    return out.toByteArray();
  }

  private static void writeString(ByteArrayOutputStream out, int field, String value) {
    writeBytes(out, field, value.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeBytes(ByteArrayOutputStream out, int field, byte[] value) {
    writeVarint(out, field << 3 | WIRE_TYPE_LENGTH_DELIMITED);
    writeVarint(out, value.length);
    out.write(value, 0, value.length);
  }

  private static void writeVarintField(ByteArrayOutputStream out, int field, int value) {
    writeVarint(out, field << 3 | WIRE_TYPE_VARINT);
    writeVarint(out, value);
  }

  private static void writeVarint(ByteArrayOutputStream out, int value) {
    while ((value & ~0x7F) != 0) {
      out.write((value & 0x7F) | 0x80);
      value >>>= 7;
    }
    out.write(value);
  }
}
