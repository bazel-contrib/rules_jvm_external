package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.Artifact;
import com.github.bazelbuild.rules_jvm_external.resolver.ResolutionRequest;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GradleBuildFile {

  private final Netrc netrc;
  private final ResolutionRequest request;

  public GradleBuildFile(Netrc netrc, ResolutionRequest request) {
    this.netrc = Objects.requireNonNull(netrc);
    this.request = request;
  }

  public String render() {
    StringBuilder contents = new StringBuilder();
    contents.append("repositories {\n");
    for (int i = 0; i < request.getRepositories().size(); i++) {
      URI uri = request.getRepositories().get(0);
      contents.append("  maven").append(" {\n    url = uri(\"").append(uri).append("\")\n");
      if ("http".equals(uri.getScheme())) {
        contents.append("    allowInsecureProtocol = true\n");
      }
      Netrc.Credential credential =
          netrc.credentials().entrySet().stream()
              .filter(e -> e.getKey().equals(uri.getHost()))
              .findFirst()
              .map(Map.Entry::getValue)
              .orElse(netrc.defaultCredential());
      if (credential != null) {
        contents.append("    credentials {\n");
        contents.append("        username \"").append(credential.login()).append("\"\n");
        contents.append("        password \"").append(credential.password()).append("\"\n");
        contents.append("    }\n");
      }

      contents.append("  }\n");
    }
    contents.append("}\n\n");

    contents.append("dependencies {\n");
    for (Artifact bom : request.getBoms()) {
      // We need to remove the `pom` classifier if gradle is going to be happy
      Coordinates defaultCoords = bom.getCoordinates().setClassifier("jar").setExtension(null);
      contents
          .append(
              toGradleDependencyNotation(
                  "implementation platform", new Artifact(defaultCoords, bom.getExclusions())))
          .append("\n");
    }

    for (Artifact dep : request.getDependencies()) {
      contents.append(toGradleDependencyNotation("implementation", dep)).append("\n");
    }
    contents.append("}\n\n");

    // Add any global exclusions
    if (!request.getGlobalExclusions().isEmpty()) {
      contents.append("configurations.all {\n");
      request
          .getGlobalExclusions()
          .forEach(
              e -> {
                contents
                    .append("  exclude group: '")
                    .append(e.getGroupId())
                    .append("', module: '")
                    .append(e.getArtifactId())
                    .append("'\n");
              });
      contents.append("}\n\n");
    }
    return contents.toString();
  }

  private String toGradleDependencyNotation(String type, Artifact artifact) {
    Coordinates coords = artifact.getCoordinates();
    StringBuilder toReturn =
        new StringBuilder(type)
            .append("(")
            .append("'")
            .append(coords.getGroupId())
            .append(":")
            .append(coords.getArtifactId());
    if (!Strings.isNullOrEmpty(coords.getVersion())) {
      toReturn.append(":").append(coords.getVersion());
    }
    if (!Strings.isNullOrEmpty(coords.getClassifier())) {
      toReturn.append(":").append(coords.getClassifier());
    }
    if (!Strings.isNullOrEmpty(coords.getExtension()) && !"jar".equals(coords.getExtension())) {
      toReturn.append("@").append(coords.getExtension());
    }
    toReturn.append("')");

    if (!artifact.getExclusions().isEmpty()) {
      toReturn.append(" {\n");
      for (Coordinates exclusion : artifact.getExclusions()) {
        toReturn
            .append("    exclude group: '")
            .append(exclusion.getGroupId())
            .append("', module: '")
            .append(exclusion.getArtifactId())
            .append("'\n");
      }
      toReturn.append("  }");
    }

    return toReturn.toString();
  }
}
