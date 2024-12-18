// Copyright 2024 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.bazelbuild.rules_jvm_external;

import java.util.Objects;

/**
 * Represents a Maven coordinate using Maven's standard schema of
 * <groupId>:<artifactId>[:<extension>[:<classifier>][:<version>].
 */
public class Coordinates implements Comparable<Coordinates> {
  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classifier;
  private final String extension;

  /**
   * Converts a `String` in one of two formats and extracts the information from it.
   *
   * <p>The two supported formats are:
   *
   * <ol>
   *   <li>group:artifact:version:classifier@extension
   *   <li>group:artifact:extension:classifier:version.
   * </ol>
   *
   * The first of these matches Gradle's <a
   * href="https://docs.gradle.org/8.11.1/dsl/org.gradle.api.artifacts.dsl.DependencyHandler.html#N1739F">
   * external dependencies</a> form, and is the preferred format to use since it matches
   * expectations of users of other tools. The second format is the one used within
   * `rules_jvm_external` since its inception.
   *
   * <p>Note that there is potential confusion when only three segments are given (that is, the
   * value could be one of `group:artifact:version` or `group:artifact:extension`) In this case, we
   * assume the value is `group:artifact:version` as this is far more widely used.
   */
  public Coordinates(String coordinates) {
    Objects.requireNonNull(coordinates, "Coordinates");

    String[] parts = coordinates.split(":");
    if (parts.length > 5 || parts.length < 2) {
      throw new IllegalArgumentException(
          "Bad artifact coordinates "
              + coordinates
              + ", expected format is"
              + " <groupId>:<artifactId>[:<version>][:<classifier>][@<extension>");
    }

    groupId = Objects.requireNonNull(parts[0]);
    artifactId = Objects.requireNonNull(parts[1]);

    boolean isGradle =
        coordinates.contains("@")
            || (parts.length > 2 && !parts[2].isEmpty() && Character.isDigit(parts[2].charAt(0)));

    String version = null;
    String extension = "jar";
    String classifier = "jar";

    if (parts.length == 2) {
      extension = "jar";
      classifier = "";
      version = "";
    } else if (parts.length == 5) { // Unambiguously the original format
      extension = "".equals(parts[2]) ? "jar" : parts[2];
      classifier = "jar".equals(parts[3]) ? "" : parts[3];
      version = parts[4];
    } else if (parts.length == 3) {
      // Could either be g:a:e or g:a:v or g:a:v@e
      if (isGradle) {
        classifier = "";

        if (parts[2].contains("@")) {
          String[] subparts = parts[2].split("@", 2);
          version = subparts[0];
          extension = subparts[1];
        } else {
          extension = "jar";
          version = parts[2];
        }
      }
    } else {
      // Could be either g:a:e:c or g:a:v:c or g:a:v:c@e
      if (isGradle) {
        version = parts[2];
        if (parts[3].contains("@")) {
          String[] subparts = parts[3].split("@", 2);
          classifier = subparts[0];
          extension = subparts[1];
        } else {
          classifier = parts[3];
          extension = "jar";
        }
      } else {
        extension = parts[2];
        classifier = "";
        version = parts[3];
      }
    }

    this.version = version;
    this.classifier = classifier;
    this.extension = extension;
  }

  public Coordinates(
      String groupId, String artifactId, String extension, String classifier, String version) {
    this.groupId = Objects.requireNonNull(groupId, "Group ID");
    this.artifactId = Objects.requireNonNull(artifactId, "Artifact ID");
    this.extension = extension == null || extension.isEmpty() ? "jar" : extension;
    this.classifier =
        classifier == null || classifier.isEmpty() || "jar".equals(classifier) ? "" : classifier;
    this.version = version == null || version.isEmpty() ? "" : version;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

  public Coordinates setClassifier(String classifier) {
    return new Coordinates(getGroupId(), getArtifactId(), getExtension(), classifier, getVersion());
  }

  public Coordinates setExtension(String extension) {
    return new Coordinates(getGroupId(), getArtifactId(), extension, getClassifier(), getVersion());
  }

  public Coordinates setVersion(String version) {
    return new Coordinates(getGroupId(), getArtifactId(), getExtension(), getClassifier(), version);
  }

  public String getExtension() {
    return extension;
  }

  // This method matches `coordinates.bzl#to_key`. Any changes here must be matched there.
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

  public String toRepoPath() {
    StringBuilder path = new StringBuilder();
    path.append(getGroupId().replace('.', '/'))
        .append("/")
        .append(getArtifactId())
        .append("/")
        .append(getVersion())
        .append("/")
        .append(getArtifactId())
        .append("-")
        .append(getVersion());

    String classifier = getClassifier();

    if (!isNullOrEmpty(classifier) && !"jar".equals(classifier)) {
      path.append("-").append(classifier);
    }
    if (!isNullOrEmpty(getExtension())) {
      path.append(".").append(getExtension());
    } else {
      path.append(".jar");
    }

    return path.toString();
  }

  @Override
  public int compareTo(Coordinates o) {
    return this.toString().compareTo(o.toString());
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append(getGroupId()).append(":").append(getArtifactId());

    if (getVersion() != null && !getVersion().isEmpty()) {
      builder.append(":").append(getVersion());
    }

    if (getClassifier() != null && !getClassifier().isEmpty() && !"jar".equals(getClassifier())) {
      builder.append(":").append(getClassifier());
    }

    if (getExtension() != null && !getExtension().isEmpty() && !"jar".equals(getExtension())) {
      builder.append("@").append(getExtension());
    }

    return builder.toString();
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
        && Objects.equals(getVersion(), that.getVersion())
        && Objects.equals(getClassifier(), that.getClassifier())
        && Objects.equals(getExtension(), that.getExtension());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getGroupId(), getArtifactId(), getVersion(), getClassifier(), getExtension());
  }

  private boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
