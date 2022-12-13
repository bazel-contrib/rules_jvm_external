package rules.jvm.external.jar;

import com.google.gson.Gson;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ListPackages {

  private static final Predicate<String> IS_NUMERIC_VERSION =
      Pattern.compile("[1-9][0-9]*").asPredicate();

  public static void main(String[] args) throws IOException {
    if (args.length != 2 || !"--argsfile".equals(args[0])) {
      System.err.printf("Required args: --argsfile /path/to/argsfile%n");
      System.exit(1);
    }

    TreeMap<String, SortedSet<String>> index =
        Files.lines(Paths.get(args[1]))
            .parallel()
            .map(
                path -> {
                  try {
                    SortedSet<String> packages = process(Paths.get(path));
                    return new AbstractMap.SimpleEntry<>(path, packages);
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                })
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey(),
                    entry -> entry.getValue(),
                    (left, right) -> {
                      throw new RuntimeException("Duplicate keys detected but not expected");
                    },
                    TreeMap::new));
    System.out.println(new Gson().toJson(index));
  }

  public static SortedSet<String> process(Path path) throws IOException {
    SortedSet<String> packages = new TreeSet<>();
    try (InputStream fis = new BufferedInputStream(Files.newInputStream(path));
        ZipInputStream zis = new ZipInputStream(fis)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.getName().endsWith(".class")) {
          continue;
        }
        if ("module-info.class".equals(entry.getName())
            || entry.getName().endsWith("/module-info.class")) {
          continue;
        }
        packages.add(extractPackageName(entry.getName()));
      }
    }
    return packages;
  }

  private static String extractPackageName(String zipEntryName) {
    String[] parts = zipEntryName.split("/");
    if (parts.length == 1) {
      return "";
    }
    int skip = 0;
    // As per https://docs.oracle.com/en/java/javase/13/docs/specs/jar/jar.html
    if (parts.length > 3
        && "META-INF".equals(parts[0])
        && "versions".equals(parts[1])
        && isNumericVersion(parts[2])) {
      skip = 3;
    }

    // -1 for the class name, -skip for the skipped META-INF prefix.
    int limit = parts.length - 1 - skip;
    return Arrays.stream(parts).skip(skip).limit(limit).collect(Collectors.joining("."));
  }

  private static boolean isNumericVersion(String part) {
    return IS_NUMERIC_VERSION.test(part);
  }
}
