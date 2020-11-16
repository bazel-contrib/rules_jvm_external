package rules.jvm.external.jar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/*
 * A class that will add an entry to the manifest and keep the same modification
 # times of the jar entries.
 */
public class AddJarManifestEntry {

  public static final long DEFAULT_TIMESTAMP =
      LocalDateTime.of(2010, 1, 1, 0, 0, 0)
          .atZone(ZoneId.systemDefault())
          .toInstant()
          .toEpochMilli();

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
    String manifestEntry = null;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--output":
          out = Paths.get(args[++i]);
          break;

        case "--manifest-entry":
          manifestEntry = args[++i];
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

    addEntryToManifest(out, source, manifestEntry, false);
  }

  public static void addEntryToManifest(Path out, Path source, String manifestEntry, boolean addManifest) throws IOException {
    byte[] buffer = new byte[2048];
    int bytesRead;
    boolean manifestUpdated = false;

    try (InputStream fis = Files.newInputStream(source);
         ZipInputStream zis = new ZipInputStream(fis)) {

      try (OutputStream fos = Files.newOutputStream(out);
           ZipOutputStream zos = new JarOutputStream(fos)) {

        if (addManifest) {
          verboseLog("INFO: No jar manifest found in " + source + " Adding new jar manifest.");
          Manifest manifest = new Manifest();
          manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
          manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "AddJarManifestEntry");
          String[] manifestEntryParts = manifestEntry.split(":", 2);
          manifest.getMainAttributes().put(new Attributes.Name(manifestEntryParts[0]), manifestEntryParts[1]);

          ZipEntry newManifestEntry = new ZipEntry(JarFile.MANIFEST_NAME);
          newManifestEntry.setTime(DEFAULT_TIMESTAMP);
          zos.putNextEntry(newManifestEntry);
          manifest.write(zos);
          manifestUpdated = true;
        }

        ZipEntry sourceEntry;
        while ((sourceEntry = zis.getNextEntry()) != null) {
          String name = sourceEntry.getName();

          ZipEntry outEntry = new ZipEntry(name);
          outEntry.setMethod(sourceEntry.getMethod());
          outEntry.setTime(sourceEntry.getTime());
          outEntry.setComment(sourceEntry.getComment());
          outEntry.setExtra(sourceEntry.getExtra());

          if (manifestEntry != null && JarFile.MANIFEST_NAME.equals(name)) {
            Manifest manifest = new Manifest(zis);
            String[] manifestEntryParts = manifestEntry.split(":", 2);
            manifest.getMainAttributes().put(new Attributes.Name(manifestEntryParts[0]), manifestEntryParts[1]);

            if (sourceEntry.getMethod() == ZipEntry.STORED) {
              CRC32OutputStream crc32OutputStream = new CRC32OutputStream();
              manifest.write(crc32OutputStream);
              outEntry.setSize(crc32OutputStream.getSize());
              outEntry.setCrc(crc32OutputStream.getCRC());
            }
            zos.putNextEntry(outEntry);
            manifest.write(zos);
            manifestUpdated = true;

          } else {

            if (sourceEntry.getMethod() == ZipEntry.STORED) {
              outEntry.setSize(sourceEntry.getSize());
              outEntry.setCrc(sourceEntry.getCrc());
            }

            try {
              zos.putNextEntry(outEntry);
            } catch (ZipException e) {
              if (e.getMessage().contains("duplicate entry:")) {
                // If there is a duplicate entry we keep the first one we saw.
                verboseLog("WARN: Skipping duplicate jar entry " + outEntry.getName() + " in " + source);
                continue;
              } else {
                throw e;
              }
            }
            while ((bytesRead = zis.read(buffer)) != -1) {
              zos.write(buffer, 0, bytesRead);
            }
          }
        }
      }
    }

    if (manifestEntry != null && !manifestUpdated) {
      // If no manifest was found then re-run and add the MANIFEST.MF as the first entry in the output jar
      addEntryToManifest(out, source, manifestEntry, true);
    }
  }

  // OutputStream to find the CRC32 of an updated STORED zip entry
  private static class CRC32OutputStream extends java.io.OutputStream {
    private final CRC32 crc = new CRC32();
    private long size = 0;

    CRC32OutputStream() {}

    public void write(int b) throws IOException {
      crc.update(b);
      size++;
    }

    public void write(byte[] b, int off, int len) throws IOException {
      crc.update(b, off, len);
      size += len;
    }

    public long getSize() {
      return size;
    }

    public long getCRC() {
      return crc.getValue();
    }
  }
}
