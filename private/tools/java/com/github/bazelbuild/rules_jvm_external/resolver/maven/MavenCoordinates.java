package com.github.bazelbuild.rules_jvm_external.resolver.maven;

import static com.github.bazelbuild.rules_jvm_external.resolver.maven.MavenPackagingMappings.mapPackagingToExtension;
import static com.github.bazelbuild.rules_jvm_external.resolver.maven.MavenPackagingMappings.mapPackingToClassifier;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import org.eclipse.aether.artifact.Artifact;

class MavenCoordinates {

  public static Coordinates asCoordinates(Artifact artifact) {
    // Please excuse the sleight-of-hand here. Turns out that we need to do some finagaling of the
    // `Artifact` to have a string
    // we can convert to a `Coordinate`.

    String classifier = artifact.getClassifier();

    if (classifier == null || classifier.isEmpty()) {
      classifier = mapPackingToClassifier(artifact.getExtension());
    }

    return new Coordinates(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        mapPackagingToExtension(artifact.getExtension()),
        classifier,
        artifact.getVersion());
  }

  private static String construct(
      String groupId, String artifactId, String extension, String classifier, String version) {
    StringBuilder coords = new StringBuilder();
    coords.append(groupId).append(":").append(artifactId);

    if (extension != null && !extension.isEmpty()) {
      coords.append(":").append(extension);
    }

    if (classifier != null && !classifier.isEmpty() && !"jar".equals(classifier)) {
      coords.append(":").append(classifier);
    }

    if (version != null && !version.isEmpty()) {
      coords.append(":").append(version);
    }

    return coords.toString();
  }
}
