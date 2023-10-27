package com.github.bazelbuild.rules_jvm_external.resolver;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.MavenRepositoryPath;
import com.google.common.hash.Hashing;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

public class MavenRepo {

  private final Path root;

  private MavenRepo(Path root) {
    this.root = root;
  }

  public static MavenRepo create() {
    Path root = null;
    try {
      root = Files.createTempDirectory("maven-repo");
      return new MavenRepo(root);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public MavenRepo add(Model item, Coordinates... deps) {
    // The coordinates we use need not be terribly complicated
    Coordinates coords =
        new Coordinates(
            item.getGroupId(), item.getArtifactId(), item.getPackaging(), null, item.getVersion());

    return add(coords, item, deps);
  }

  public MavenRepo add(Coordinates coords, Coordinates... deps) {
    Model model = new Model();
    model.setGroupId(coords.getGroupId());
    model.setArtifactId(coords.getArtifactId());
    model.setVersion(coords.getVersion());

    return add(coords, model, deps);
  }

  private MavenRepo add(Coordinates coords, Model ofCoordinates, Coordinates... deps) {
    try {
      writePomFile(ofCoordinates, deps);
      if (!"pom".equals(coords.getExtension())) {
        writeFile(coords);
      }

      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public MavenRepo writePomFile(Model model) throws IOException {
    model.setModelVersion("4.0.0");

    Coordinates coords =
        new Coordinates(model.getGroupId(), model.getArtifactId(), null, null, model.getVersion());
    Path dir = root.resolve(new MavenRepositoryPath(coords).getPath()).getParent();
    Files.createDirectories(dir);

    Path pomFile = dir.resolve(model.getArtifactId() + "-" + model.getVersion() + ".pom");
    try (BufferedWriter writer = Files.newBufferedWriter(pomFile)) {
      new MavenXpp3Writer().write(writer, model);
    }
    writeSha1File(pomFile);

    return this;
  }

  private void writePomFile(Model model, Coordinates... deps) throws IOException {
    for (Coordinates dep : deps) {
      Dependency mavenDep = new Dependency();
      mavenDep.setGroupId(dep.getGroupId());
      mavenDep.setArtifactId(dep.getArtifactId());
      mavenDep.setVersion(dep.getVersion());
      if (dep.getClassifier() != null && !dep.getClassifier().isEmpty()) {
        mavenDep.setClassifier(dep.getClassifier());
      }
      mavenDep.setType(dep.getExtension());

      model.addDependency(mavenDep);
    }

    writePomFile(model);
  }

  private void writeFile(Coordinates coords) throws IOException {
    Path output = root.resolve(new MavenRepositoryPath(coords).getPath());
    // We don't read the contents, it just needs to exist
    Files.write(output, "Hello, World!".getBytes(UTF_8));

    writeSha1File(output);
  }

  private void writeSha1File(Path path) throws IOException {
    // Now write the checksum
    byte[] bytes = Files.readAllBytes(path);
    String hashCode = Hashing.sha1().hashBytes(bytes).toString();

    Path shaFile = Paths.get(path.toAbsolutePath() + ".sha1");
    Files.write(shaFile, hashCode.getBytes(UTF_8));
  }

  public Path getPath() {
    return root;
  }
}
