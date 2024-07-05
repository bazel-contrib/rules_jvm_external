// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.github.bazelbuild.rules_jvm_external.jar;

import static com.github.bazelbuild.rules_jvm_external.jar.DuplicateEntryStrategy.LAST_IN_WINS;
import static java.util.zip.Deflater.BEST_COMPRESSION;
import static java.util.zip.ZipOutputStream.DEFLATED;

import com.github.bazelbuild.rules_jvm_external.ByteStreams;
import com.github.bazelbuild.rules_jvm_external.zip.StableZipEntry;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MergeJars {

  private enum Packaging {
    JAR, AAR;
  }

  public static void main(String[] args) throws IOException {
    Path out = null;
    // Insertion order may matter
    Set<Path> sources = new LinkedHashSet<>();
    Set<Path> excludes = new HashSet<>();
    DuplicateEntryStrategy onDuplicate = LAST_IN_WINS;
    Packaging packaging = Packaging.JAR;
    PathMatcher aarMatcher = FileSystems.getDefault().getPathMatcher("glob:*.aar");
    // AAR to build from
    Path aarSource = null;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--compression":
        case "--normalize":
          // ignore
          break;

        case "--duplicates":
          onDuplicate = DuplicateEntryStrategy.fromShortName(args[++i]);
          break;

        case "--exclude":
          excludes.add(isValid(Paths.get(args[++i])));
          break;

        case "--output":
          out = Paths.get(args[++i]);
          if (aarMatcher.matches(out.getFileName())) {
            packaging = Packaging.AAR;
          }
          break;

        case "--sources":
          sources.add(isValid(Paths.get(args[++i])));
          break;

        default:
          throw new IllegalArgumentException(
              "Unable to parse command line: " + Arrays.toString(args));
      }
    }

    if (packaging == Packaging.AAR) {
      aarSource = sources.stream()
              .filter(source ->  aarMatcher.matches(source.getFileName()))
              .findFirst() // AAR is explicitly only added for top level distribution target, so we _should_ only ever have 1
              .orElseThrow(() -> new IllegalArgumentException("For AAR packaging, we require a prebuilt AAR that already contains the Android resources that we'll add the transitive source closure to."));

      sources.remove(aarSource);

      // Pull out classes jar and add to source set
      Path aarClassesJar = out.getParent().resolve("aar-classes.jar");
      try (ZipFile aar = new ZipFile(aarSource.toFile())) {
        ZipEntry classes = aar.getEntry("classes.jar");
        try (InputStream is = aar.getInputStream(classes);
             OutputStream fos = Files.newOutputStream(aarClassesJar)) {
          ByteStreams.copy(is, fos);
        }
      }
      sources.add(aarClassesJar);
    }

    Objects.requireNonNull(out, "Output path must be set.");
    if (sources.isEmpty()) {
      // Just write an empty jar and leave
      try (OutputStream fos = Files.newOutputStream(out);
          JarOutputStream jos = new JarOutputStream(fos)) {
        // This space left blank deliberately
      }
      return;
    }

    // Remove any jars from sources that we've been told to exclude
    sources.removeIf(excludes::contains);

    // We would love to keep things simple by expanding all the input jars into
    // a single directory, but this isn't possible since one jar may contain a
    // file with the same name as a directory in another. *sigh* Instead, what
    // we'll do is create a list of contents from each jar, and where we should
    // pull the contents from. Whee!

    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

    Map<String, Set<String>> allServices = new TreeMap<>();
    Set<String> excludedPaths = readExcludedFileNames(excludes);

    // Ultimately, we want the entries in the output zip to be sorted
    // so that we have a deterministic output.
    Map<String, Path> fileToSourceJar = new TreeMap<>();
    Map<String, byte[]> fileHashCodes = new HashMap<>();

    for (Path source : sources) {
      try (InputStream fis = Files.newInputStream(source);
          ZipInputStream zis = new ZipInputStream(fis)) {

        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
            Manifest other = new Manifest(zis);
            manifest = merge(manifest, other);
            continue;
          }

          if ("META-INF/".equals(entry.getName())
              || (!entry.getName().startsWith("META-INF/")
                  && excludedPaths.contains(entry.getName()))) {
            continue;
          }

          if (entry.getName().startsWith("META-INF/services/") && !entry.isDirectory()) {
            String servicesName = entry.getName().substring("META-INF/services/".length());
            Set<String> services =
                allServices.computeIfAbsent(servicesName, key -> new TreeSet<>());
            String content = new String(ByteStreams.toByteArray(zis));
            services.addAll(Arrays.asList(content.split("\n")));
            continue;
          }

          // TODO: Why do we need to do this?? Is there a better way?
          Pattern rClassMatcher = Pattern.compile("^.*\\/R(\\$.*)?\\.(class|java)");
          if (rClassMatcher.asMatchPredicate().test(entry.getName())) {
            continue;
          }

          if (!entry.isDirectory()) {
            // Duplicate files, however may not be. We need the hash to determine
            // whether we should do anything.
            byte[] hash = hash(zis);

            if (!fileToSourceJar.containsKey(entry.getName())) {
              fileToSourceJar.put(entry.getName(), source);
              fileHashCodes.put(entry.getName(), hash);
            } else {
              byte[] originalHashCode = fileHashCodes.get(entry.getName());
              boolean replace =
                  onDuplicate.isReplacingCurrent(entry.getName(), originalHashCode, hash);
              if (replace) {
                fileToSourceJar.put(entry.getName(), source);
                fileHashCodes.put(entry.getName(), hash);
              }
            }
          }
        }
      }
    }

    manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "mergejars");
    // Bazel labels are an internal detail of the project producing the merged
    // jar and not useful for consumers.
    manifest.getMainAttributes().remove(new Attributes.Name("Target-Label"));

    switch (packaging) {
      case JAR:
        writeClassesJar(out, manifest, allServices, sources, fileToSourceJar);
        break;
      case AAR:
        Path classesJar = out.getParent().resolve("classes.jar");
        writeClassesJar(classesJar, manifest, allServices, sources, fileToSourceJar);
        writeAar(out, aarSource, classesJar);
    }
  }

  private static void writeAar(Path out, Path aarSource, Path classesJar) throws IOException {
    try (OutputStream os = Files.newOutputStream(out);
         JarOutputStream jos = new JarOutputStream(os)) {
      ZipEntry je = new StableZipEntry(classesJar.toFile().getName());
      jos.putNextEntry(je);

      try (InputStream is = Files.newInputStream(classesJar)) {
        ByteStreams.copy(is, jos);
      }
      jos.closeEntry();

      try (ZipFile aar = new ZipFile(aarSource.toFile())) {
        Enumeration<? extends ZipEntry> entries = aar.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();

          // transitive class closure is captured in our classes.jar
          if ("classes.jar".equals(entry.getName())) {
            continue;
          }

          jos.putNextEntry(new ZipEntry(entry.getName()));
          try (InputStream is = aar.getInputStream(entry)) {
            ByteStreams.copy(is, jos);
          }
          jos.closeEntry();
        }
      }
    }
  }

  private static void writeClassesJar(Path out,
                                      Manifest manifest,
                                      Map<String, Set<String>> allServices,
                                      Set<Path> sources,
                                      Map<String, Path> fileToSourceJar) throws IOException {
    // Now create the output jar
    Files.createDirectories(out.getParent());

    Set<String> createdDirectories = new HashSet<>();

    try (OutputStream os = Files.newOutputStream(out);
        JarOutputStream jos = new JarOutputStream(os)) {
      jos.setMethod(DEFLATED);
      jos.setLevel(BEST_COMPRESSION);

      // Write the manifest by hand to ensure the date is good
      ZipEntry entry = new StableZipEntry("META-INF/");
      jos.putNextEntry(entry);
      jos.closeEntry();
      createdDirectories.add(entry.getName());

      entry = new StableZipEntry("META-INF/MANIFEST.MF");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      manifest.write(bos);
      entry.setSize(bos.size());
      jos.putNextEntry(entry);
      jos.write(bos.toByteArray());
      jos.closeEntry();

      if (!allServices.isEmpty()) {
        if (!createdDirectories.contains("META-INF/services/")) {
          entry = new StableZipEntry("META-INF/services/");
          jos.putNextEntry(entry);
          jos.closeEntry();
          createdDirectories.add(entry.getName());
        }
        for (Map.Entry<String, Set<String>> kv : allServices.entrySet()) {
          entry = new StableZipEntry("META-INF/services/" + kv.getKey());
          bos = new ByteArrayOutputStream();
          bos.write(String.join("\n", kv.getValue()).getBytes());
          entry.setSize(bos.size());
          jos.putNextEntry(entry);
          jos.write(bos.toByteArray());
          jos.closeEntry();
        }
      }

      Path previousSource = sources.isEmpty() ? null : sources.iterator().next();
      ZipFile source = previousSource == null ? null : new ZipFile(previousSource.toFile());

      // We should never enter this loop without there being any sources
      for (Map.Entry<String, Path> pathAndSource : fileToSourceJar.entrySet()) {
        // Get the original entry
        String name = pathAndSource.getKey();

        // Make sure we're not trying to create root entries.
        if (name.startsWith("/")) {
          if (name.length() == 1) {
            continue;
          }
          name = name.substring(1);
        }

        createDirectories(jos, name, createdDirectories);

        if (createdDirectories.contains(name)) {
          continue;
        }

        if (!Objects.equals(previousSource, pathAndSource.getValue())) {
          if (source != null) {
            source.close();
          }
          source = new ZipFile(pathAndSource.getValue().toFile());
          previousSource = pathAndSource.getValue();
        }

        ZipEntry original = source.getEntry(name);
        if (original == null) {
          continue;
        }

        ZipEntry je = new StableZipEntry(name);
        jos.putNextEntry(je);

        try (InputStream is = source.getInputStream(original)) {
          ByteStreams.copy(is, jos);
        }
        jos.closeEntry();
      }
      if (source != null) {
        source.close();
      }
    }
  }

  private static void createDirectories(JarOutputStream jos, String name, Set<String> createdDirs)
      throws IOException {
    if (!name.endsWith("/")) {
      int slashIndex = name.lastIndexOf('/');
      if (slashIndex != -1) {
        createDirectories(jos, name.substring(0, slashIndex + 1), createdDirs);
      }
      return;
    }

    if (createdDirs.contains(name)) {
      return;
    }

    String[] segments = name.split("/");
    StringBuilder path = new StringBuilder();
    for (String segment : segments) {
      path.append(segment).append('/');

      String newPath = path.toString();
      if (createdDirs.contains(newPath)) {
        continue;
      }

      ZipEntry entry = new StableZipEntry(newPath);
      jos.putNextEntry(entry);
      jos.closeEntry();
      createdDirs.add(newPath);
    }
  }

  private static Set<String> readExcludedFileNames(Set<Path> excludes) throws IOException {
    Set<String> paths = new HashSet<>();

    for (Path exclude : excludes) {
      try (InputStream is = Files.newInputStream(exclude);
          BufferedInputStream bis = new BufferedInputStream(is);
          ZipInputStream jis = new ZipInputStream(bis)) {
        ZipEntry entry;
        while ((entry = jis.getNextEntry()) != null) {
          if (entry.isDirectory()) {
            continue;
          }

          String name = entry.getName();
          paths.add(name);
        }
      }
    }
    return paths;
  }

  private static Path isValid(Path path) {
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("File does not exist: " + path);
    }

    if (!Files.isReadable(path)) {
      throw new IllegalArgumentException("File is not readable: " + path);
    }

    return path;
  }

  private static Manifest merge(Manifest into, Manifest from) {
    Attributes attributes = from.getMainAttributes();
    if (attributes != null) {
      attributes.forEach((key, value) -> into.getMainAttributes().put(key, value));
    }

    from.getEntries()
        .forEach(
            (key, value) -> {
              Attributes attrs = into.getAttributes(key);
              if (attrs == null) {
                attrs = new Attributes();
                into.getEntries().put(key, attrs);
              }
              attrs.putAll(value);
            });

    return into;
  }

  private static byte[] hash(InputStream inputStream) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");

      byte[] buf = new byte[100 * 1024];
      int read;

      while ((read = inputStream.read(buf)) != -1) {
        digest.update(buf, 0, read);
      }
      return digest.digest();
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
