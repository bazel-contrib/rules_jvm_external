package com.github.bazelbuild.rules_jvm_external.resolver;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ArtifactsHash {

  private ArtifactsHash() {
    // utility class
  }

  public static int calculateArtifactsHash(Collection<DependencyInfo> infos) {
    Set<String> lines = new TreeSet<>();

    for (DependencyInfo info : infos) {
      StringBuilder line = new StringBuilder();
      line.append(info.getCoordinates().toString())
          .append(" | ")
          .append(info.getSha256().orElseGet(() -> ""))
          .append(" | ");

      line.append(
          info.getDependencies().stream()
              .map(Coordinates::asKey)
              .sorted()
              .collect(Collectors.joining(",")));

      lines.add(line.toString());
    }

    return String.join("\n", lines).hashCode();
  }
}
