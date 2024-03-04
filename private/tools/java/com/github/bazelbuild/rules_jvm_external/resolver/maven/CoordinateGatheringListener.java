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

package com.github.bazelbuild.rules_jvm_external.resolver.maven;

import static com.github.bazelbuild.rules_jvm_external.resolver.maven.MavenPackagingMappings.mapPackagingToExtension;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class CoordinateGatheringListener extends AbstractRepositoryListener {

  private final Map<Coordinates, Coordinates> knownRewrittenCoordinates = new ConcurrentHashMap<>();

  @Override
  public void artifactResolved(RepositoryEvent event) {

    Artifact artifact = event.getArtifact();
    if (!"pom".equals(artifact.getExtension())) {
      return;
    }

    File file = event.getFile();
    if (file == null) {
      return;
    }

    // The correct thing to do here is to use a `ModelBuildingRequest` from Maven to build and
    // construct a `Model`,
    // which can then query the packaging of. However, since we just want one value from an XML
    // document, we're
    // going to do this the Old Skool way and just parse the document and run a little XPath over
    // it.
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document doc = documentBuilder.parse(file);

      doc.normalizeDocument();

      XPath xPath = XPathFactory.newInstance().newXPath();
      String packaging = xPath.compile("//project/packaging").evaluate(doc);

      if (packaging == null) {
        return;
      }

      packaging = packaging.trim();

      String extension = mapPackagingToExtension(packaging);
      // The default packaging is "jar" anyway
      if (extension.isEmpty() || "jar".equals(extension)) {
        return;
      }

      Coordinates coords =
          new Coordinates(
              artifact.getGroupId(), artifact.getArtifactId(), null, null, artifact.getVersion());

      Coordinates actualCoords =
          new Coordinates(
              artifact.getGroupId(),
              artifact.getArtifactId(),
              extension,
              artifact.getClassifier(),
              artifact.getVersion());

      knownRewrittenCoordinates.put(coords, actualCoords);
    } catch (IOException
        | ParserConfigurationException
        | SAXException
        | XPathExpressionException e) {
      // Bail and hope for the best.
    }
  }

  public Map<Coordinates, Coordinates> getRemappings() {
    return Map.copyOf(knownRewrittenCoordinates);
  }
}
