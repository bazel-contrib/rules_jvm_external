package com.github.bazelbuild.rules_jvm_external;

import java.util.Objects;

/**
 * Represents a Maven coordinate using Maven's standard schema of
 * <groupId>:<artifactId>[:<extension>[:<classifier>][:<version>].
 *
 * <p>The optional <tt>dirVersion</tt> property is used for snapshotted artifacts.  For those,
 * directory version component in the repository URL is of the <tt>*-SNAPSHOT</tt> * form
 * whereas the version in the artifact itself numeric.</p>
 */
public class Coordinates implements Comparable<Coordinates> {
  private final String groupId;
  private final String artifactId;
  private final String dirVersion;
  private final String version;
  private final String classifier;
  private final String extension;

  public Coordinates(String coordinates) {
    Objects.requireNonNull(coordinates, "Coordinates");

    String[] parts = coordinates.split(":");
    if (parts.length > 5 || parts.length < 2) {
      throw new IllegalArgumentException(
          "Bad artifact coordinates "
              + coordinates
              + ", expected format is"
              + " <groupId>:<artifactId>[:<extension>[:<classifier>][:<version>]");
    }

    groupId = Objects.requireNonNull(parts[0]);
    artifactId = Objects.requireNonNull(parts[1]);
    dirVersion = null;

    if (parts.length == 2) {
      extension = "jar";
      classifier = "";
      version = "";
    } else if (parts.length == 3) {
      extension = "jar";
      classifier = "";
      version = parts[2];
    } else if (parts.length == 4) {
      extension = parts[2];
      classifier = "";
      version = parts[3];
    } else {
      extension = parts[2];
      classifier = parts[3];
      version = parts[4];
    }
  }

  public Coordinates(
      String groupId, String artifactId, String extension, String classifier, String version, String dirVersion) {
    this.groupId = Objects.requireNonNull(groupId, "Group ID");
    this.artifactId = Objects.requireNonNull(artifactId, "Artifact ID");
    this.extension = extension == null || extension.isEmpty() ? "jar" : extension;
    this.classifier = classifier == null || classifier.isEmpty() ? "" : classifier;
    this.version = version == null || version.isEmpty() ? "" : version;
    this.dirVersion = dirVersion;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getDirVersion() {
    return dirVersion;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

  public Coordinates setClassifier(String classifier) {
    return new Coordinates(getGroupId(), getArtifactId(), getExtension(), classifier, getVersion(), dirVersion);
  }

  public Coordinates setExtension(String extension) {
    return new Coordinates(getGroupId(), getArtifactId(), extension, getClassifier(), getVersion(), dirVersion);
  }

  public String getExtension() {
    return extension;
  }

  public String asKey() {
    StringBuilder coords = new StringBuilder();
    coords.append(groupId).append(":").append(artifactId);

    if (getClassifier() != null && !getClassifier().isEmpty() && !"jar".equals(getClassifier())) {
      String extension = getExtension();
      if (extension == null || extension.isEmpty()) {
        extension = "jar";
      }
      coords.append(":").append(extension).append(":").append(getClassifier());
    } else {
      // Otherwise, we just check for the extension
      if (getExtension() != null && !getExtension().isEmpty() && !"jar".equals(getExtension())) {
        coords.append(":").append(getExtension());
      }
    }

    return coords.toString();
  }

  @Override
  public int compareTo(Coordinates o) {
    return this.toString().compareTo(o.toString());
  }

  public String toString() {
    String versionless = asKey();

    if (version != null && !version.isEmpty()) {
      return versionless + ":" + version;
    }

    return versionless;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof Coordinates)) {
      return false;
    }

    Coordinates that = (Coordinates) o;
    return getGroupId().equals(that.getGroupId())
        && getArtifactId().equals(that.getArtifactId())
        && Objects.equals(getDirVersion(), that.getDirVersion())
        && Objects.equals(getVersion(), that.getVersion())
        && Objects.equals(getClassifier(), that.getClassifier())
        && Objects.equals(getExtension(), that.getExtension());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getGroupId(), getArtifactId(), getDirVersion(), getVersion(), getClassifier(),
        getExtension());
  }
}
