// Copyright 2026 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.coursier;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.PackagingMappings;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;

/** Repairs references omitted from Coursier's report using Maven metadata it already fetched. */
final class CoordinateResolver {
  private final Set<URI> repositories;
  private final Path cache;
  private final Map<Coordinates, Coordinates> selected;
  private final Map<String, Coordinates> identities = new HashMap<>();

  CoordinateResolver(Set<URI> repositories, Path cache, Map<Coordinates, Coordinates> selected) {
    this.repositories = repositories;
    this.cache = cache;
    this.selected = selected;
    selected.values().forEach(coordinate -> identities.put(coordinate.asKey(), coordinate));
  }

  Coordinates resolve(Coordinates requested) {
    Coordinates current = requested;
    Set<Coordinates> visited = new HashSet<>();
    while (visited.add(current)) {
      if (selected.containsKey(current)) {
        return selected.get(current);
      }
      if (identities.containsKey(current.asKey())) {
        return identities.get(current.asKey());
      }
      // POM is an actual file type here, rather than an implicit jar dependency.
      if (!"pom".equals(current.getExtension())) {
        String classifier =
            current.getClassifier().isEmpty()
                ? PackagingMappings.mapPackingToClassifier(current.getExtension())
                : current.getClassifier();
        Coordinates normalised =
            current
                .setExtension(PackagingMappings.mapPackagingToExtension(current.getExtension()))
                .setClassifier("jar".equals(classifier) ? "" : classifier);
        if (identities.containsKey(normalised.asKey())) {
          return identities.get(normalised.asKey());
        }
      }
      Coordinates relocated = relocation(current);
      if (relocated == null) {
        return requested;
      }
      current = relocated;
    }
    throw new IllegalArgumentException("Cyclic POM relocation for " + requested);
  }

  private static String cachePath(URI uri) {
    // Coursier escapes cache paths independently of URL encoding, including percent signs.
    String path = uri.getScheme() + "/" + uri.getRawAuthority() + uri.getRawPath();
    StringBuilder escaped = new StringBuilder();
    for (char ch : path.toCharArray()) {
      if (ch > 128 || " %$&+,:;=?@<>#".indexOf(ch) >= 0) {
        escaped.append('%').append(hex(ch / 16)).append(hex(ch % 16));
      } else {
        escaped.append(ch);
      }
    }
    return escaped.toString();
  }

  private static char hex(int value) {
    return (char) (value < 10 ? '0' + value : 'A' + value - 10);
  }

  private Coordinates relocation(Coordinates requested) {
    if (cache == null) {
      return null;
    }
    for (URI repository : repositories) {
      String base = repository.toString();
      URI uri =
          URI.create(base.endsWith("/") ? base : base + "/")
              .resolve(requested.setExtension("pom").setClassifier("").toRepoPath());
      Path pom = "file".equals(uri.getScheme()) ? Path.of(uri) : cache.resolve(cachePath(uri));
      if (!Files.isRegularFile(pom)) {
        continue;
      }
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var xpath = XPathFactory.newInstance().newXPath();
        Node relocation =
            (Node)
                xpath.evaluate(
                    "/*[local-name()='project']/*[local-name()='distributionManagement']/*[local-name()='relocation']",
                    factory.newDocumentBuilder().parse(pom.toFile()),
                    XPathConstants.NODE);
        if (relocation == null) {
          continue;
        }
        String group = xpath.evaluate("*[local-name()='groupId']", relocation).trim();
        String artifact = xpath.evaluate("*[local-name()='artifactId']", relocation).trim();
        String version = xpath.evaluate("*[local-name()='version']", relocation).trim();
        return new Coordinates(
            group.isEmpty() ? requested.getGroupId() : group,
            artifact.isEmpty() ? requested.getArtifactId() : artifact,
            requested.getExtension(),
            requested.getClassifier(),
            version.isEmpty() ? requested.getVersion() : version);
      } catch (Exception e) {
        throw new IllegalArgumentException("Cannot read relocation from " + pom, e);
      }
    }
    return null;
  }
}
