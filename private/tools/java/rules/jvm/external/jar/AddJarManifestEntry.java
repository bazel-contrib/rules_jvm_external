package rules.jvm.external.jar;

import rules.jvm.external.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

/*
 * A class that will add an entry to the manifest and keep the same modification
 # times of the jar entries.
 */
public class AddJarManifestEntry {

  // From https://sourcegraph.com/github.com/openjdk/jdk/-/blob/src/jdk.zipfs/share/classes/jdk/nio/zipfs/ZipFileAttributeView.java?L45&subtree=true
  public static final String MTIME_ATTR_NAME = "lastModifiedTime";

  public static final FileTime DEFAULT_TIMESTAMP =
      FileTime.fromMillis(
              LocalDateTime.of(2010, 1, 1, 0, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli());

  public static void verboseLog(String logline) {
    // To make this work you need to add 'use_default_shell_env = True' to the
    // rule and specify '--action_env=RJE_VERBOSE=true' to the bazel build command.
    if (System.getenv("RJE_VERBOSE") != null) {
      System.out.println(logline);
    }
  }

  public static void main(String[] args) throws IOException {
    Path out = null;
    Path source = null;
    List<String> toAdd = new ArrayList<>();
    List<String> toRemove = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--output":
          out = Paths.get(args[++i]);
          break;

        case "--manifest-entry":
          toAdd.add(args[++i]);
          break;

        case "--remove-entry":
          toRemove.add(args[++i]);
          break;

        case "--source":
          source = Paths.get(args[++i]);
          break;

        default:
          throw new IllegalArgumentException("Unable to parse command line: " + Arrays.toString(args));
      }
    }

    Objects.requireNonNull(source, "Source jar must be set.");
    Objects.requireNonNull(out, "Output path must be set.");

    if (isJarSigned(source)) {
      verboseLog("Signed jar. Will not modify: " + source);
      Files.createDirectories(out.getParent());
      Files.copy(source, out, REPLACE_EXISTING);
      return;
    }

    addEntryToManifest(out, source, toAdd, toRemove);
  }

  private static boolean isJarSigned(Path source) throws IOException {
    try (InputStream is = Files.newInputStream(source);
         JarInputStream jis = new JarInputStream(is)) {
      for (ZipEntry entry = jis.getNextEntry(); entry != null; entry = jis.getNextJarEntry()) {
        if (entry.isDirectory()) {
          continue;
        }
        if (entry.getName().startsWith("META-INF/") && entry.getName().endsWith(".SF")) {
          return true;
        }
      }
    }
    return false;
  }

  public static void addEntryToManifest(
          Path out,
          Path source,
          List<String> toAdd,
          List<String> toRemove) throws IOException {

    Files.copy(source, out);
    out.toFile().setWritable(true);
    try (FileSystem fs = FileSystems.newFileSystem(out, null)) {
        Manifest manifest;
        Path manifestPath = fs.getPath(JarFile.MANIFEST_NAME);
        if (Files.exists(manifestPath)) {
            manifest = new Manifest(Files.newInputStream(manifestPath));
        } else {
          manifest = new Manifest();
          manifest.getMainAttributes().put(MANIFEST_VERSION, "1.0");
        }
        amendManifest(manifest, toAdd, toRemove);
        if (Files.exists(manifestPath)) {
            Files.delete(manifestPath);
        }
        if (!Files.exists(manifestPath.getParent())) {
            fs.provider().createDirectory(manifestPath.getParent());
        }
        try (OutputStream os = fs.provider().newOutputStream(manifestPath, CREATE_NEW)) {
            manifest.write(os);
        }
        fs.provider().setAttribute(manifestPath, MTIME_ATTR_NAME, DEFAULT_TIMESTAMP);
    }
  }

  private static void amendManifest(Manifest manifest, List<String> toAdd, List<String> toRemove) {
    manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "AddJarManifestEntry");
    toAdd.forEach(manifestEntry -> {
      String[] manifestEntryParts = manifestEntry.split(":", 2);
      manifest.getMainAttributes().put(new Attributes.Name(manifestEntryParts[0]), manifestEntryParts[1]);
    });
    toRemove.forEach(name -> manifest.getMainAttributes().remove(new Attributes.Name(name)));
  }
}
