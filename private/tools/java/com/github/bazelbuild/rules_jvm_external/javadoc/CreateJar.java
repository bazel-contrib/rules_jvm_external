package com.github.bazelbuild.rules_jvm_external.javadoc;

import com.github.bazelbuild.rules_jvm_external.zip.StableZipEntry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CreateJar {

  public static void main(String[] args) throws IOException {
    Path out = Paths.get(args[0]);
    Set<Path> inputs = Stream.of(args).skip(1).map(Paths::get).collect(Collectors.toSet());

    Path tmpDir = Files.createTempDirectory("create-jar-temp");
    tmpDir.toFile().deleteOnExit();

    for (Path input : inputs) {
      if (!Files.isDirectory(input)) {
        Files.copy(input, tmpDir.resolve(input.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        continue;
      }

      Files.walk(input)
          .forEachOrdered(
              source -> {
                try {
                  Path target = tmpDir.resolve(input.relativize(source));
                  if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                  } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target);
                  }
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }

    createJar(out, tmpDir);
  }

  public static void createJar(Path out, Path inputDir) throws IOException {
    try (OutputStream os = Files.newOutputStream(out);
        ZipOutputStream zos = new ZipOutputStream(os);
        Stream<Path> walk = Files.walk(inputDir)) {

      walk.sorted(Comparator.naturalOrder())
          .forEachOrdered(
              path -> {
                if (path.equals(inputDir)) {
                  return;
                }

                try {
                  if (Files.isDirectory(path)) {
                    String name = inputDir.relativize(path) + "/";
                    ZipEntry entry = new StableZipEntry(name);
                    zos.putNextEntry(entry);
                    zos.closeEntry();
                  } else {
                    String name = inputDir.relativize(path).toString();
                    ZipEntry entry = new StableZipEntry(name);
                    zos.putNextEntry(entry);
                    try (InputStream is = Files.newInputStream(path)) {
                      com.github.bazelbuild.rules_jvm_external.ByteStreams.copy(is, zos);
                    }
                    zos.closeEntry();
                  }
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }
  }
}
