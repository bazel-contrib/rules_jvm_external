// Copyright 2018 The Bazel Authors. All rights reserved.
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
// limitations under the License.import java.io.FileWriter;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A command line tool for generating Bazel workspace rules for all JARs and AARs on
 * https://maven.google.com.
 */
public class GMavenToBazel {

  private GMavenToBazel() {}

  private static final String MASTER_INDEX_URL =
      "https://dl.google.com/dl/android/maven2/master-index.xml";
  private static final String GROUP_INDEX_TEMPLATE_URL =
      "https://dl.google.com/dl/android/maven2/%s/group-index.xml";
  private static final String POM_TEMPLATE_URL =
      "https://dl.google.com/dl/android/maven2/%s/%s/%s/%s-%s.pom";
  private static final String ARTIFACT_TEMPLATE_URL =
      "https://dl.google.com/dl/android/maven2/%s/%s/%s/%s-%s.%s";
  private static final String OUTPUT_FILE = "gmaven.bzl";
  private static final DocumentBuilderFactory documentBuilderFactory =
      DocumentBuilderFactory.newInstance();
  private static final Map<String, String> PACKAGING_TO_RULE = new HashMap<>();

  static {
    PACKAGING_TO_RULE.put("jar", "java_import_external");
    PACKAGING_TO_RULE.put("aar", "aar_import_external");
  }

  public static void main(String[] args) throws Exception {
    Document masterIndex = parseUrl(new URL(MASTER_INDEX_URL));
    Map<String, Artifact> artifacts =
        getGroupdIds(masterIndex)
            .parallelStream()
            .flatMap(
                groupId -> {
                  Map<String, Set<String>> artifactsToVersions = getArtifactsToVersions(groupId);
                  return artifactsToVersions
                      .entrySet()
                      .stream()
                      .flatMap(
                          entry -> {
                            String artifactId = entry.getKey();
                            Set<String> versions = entry.getValue();
                            return versions
                                .stream()
                                .map(version -> new String[] {groupId, artifactId, version});
                          });
                })
            .map(
                ids -> {
                  String groupId = ids[0], artifactId = ids[1], version = ids[2];
                  try {
                    Document pom = parseUrl(getPomUrl(groupId, artifactId, version));

                    String packaging = getPackaging(pom);
                    // We don't support APK packaging yet.
                    if (!PACKAGING_TO_RULE.containsKey(packaging)) {
                      return null;
                    }
                    Artifact artifact = new Artifact(groupId, artifactId, version, packaging);
                    artifact.dependencies = getDependencyRepositoryNames(pom);

                    return artifact;
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(artifact -> artifact.repositoryName, artifact -> artifact));

    PrintWriter bzlWriter =
        new PrintWriter(new BufferedWriter(new FileWriter(OUTPUT_FILE), 32 * 1024));
    bzlWriter.println(
        "load('//:import_external.bzl', 'java_import_external', 'aar_import_external')");
    bzlWriter.println("def gmaven_rules():");

    for (String repositoryName : artifacts.keySet()) {
      final Artifact artifact = artifacts.get(repositoryName);
      final String url = artifact.toArtifactUrl();
      final String ruleType = artifact.rule();
      bzlWriter.println();
      bzlWriter.println(String.format("  %s(", ruleType));
      bzlWriter.println(String.format("      name = '%s',", repositoryName));
      bzlWriter.println("      licenses = ['notice'], # apache");
      if ("aar_import_external".equals(ruleType)) {
        bzlWriter.println(String.format("      aar_urls = ['%s'],", url));
        bzlWriter.println("      aar_sha256 = '',");
      } else {
        bzlWriter.println(String.format("      jar_urls = ['%s'],", url));
        bzlWriter.println("      jar_sha256 = '',");
      }
      bzlWriter.println("      deps = [");
      for (String repositoryNameDep : artifact.dependencies) {
        Artifact targetNameDep = artifacts.get(repositoryNameDep);
        if (targetNameDep == null) {
          // our princess is in another castle!
          bzlWriter.println(String.format("        # GMaven does not have %s", repositoryNameDep));
        } else {
          bzlWriter.println(String.format("        '%s',", targetNameDep.targetName()));
        }
      }
      bzlWriter.println("      ],");
      bzlWriter.println("    )");
    }

    bzlWriter.close();
  }

  private static Document parseUrl(URL url) throws Exception {
    try (InputStream in = new BufferedInputStream(url.openStream(), 8096)) {
      return documentBuilderFactory.newDocumentBuilder().parse(in);
    }
  }

  private static List<String> getGroupdIds(Document masterIndex) {
    List<String> groupIds = new ArrayList<>();
    NodeList groupIdNodes = masterIndex.getDocumentElement().getChildNodes();
    for (int index = 1; index < groupIdNodes.getLength(); index += 2) {
      groupIds.add(groupIdNodes.item(index).getNodeName());
    }
    return groupIds;
  }

  private static Map<String, Set<String>> getArtifactsToVersions(String groupId) {
    Document groupIndex;
    try {
      groupIndex = parseUrl(getGroupIndexUrl(groupId));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Map<String, Set<String>> result = new HashMap<>();
    NodeList artifactNodes = groupIndex.getDocumentElement().getChildNodes();
    for (int index = 1; index < artifactNodes.getLength(); index += 2) {
      Node artifactNode = artifactNodes.item(index);
      Node versionsNode = artifactNode.getAttributes().getNamedItem("versions");
      result.put(
          artifactNode.getNodeName(),
          new HashSet<>(Arrays.asList(versionsNode.getNodeValue().split(","))));
    }
    return result;
  }

  private static URL getGroupIndexUrl(String groupId) throws MalformedURLException {
    return new URL(String.format(GROUP_INDEX_TEMPLATE_URL, groupId.replace('.', '/')));
  }

  private static URL getPomUrl(String groupId, String artifactId, String version)
      throws MalformedURLException {
    return new URL(
        String.format(
            POM_TEMPLATE_URL, groupId.replace('.', '/'), artifactId, version, artifactId, version));
  }

  private static String getPackaging(Document pom) throws Exception {
    Element project = (Element) pom.getElementsByTagName("project").item(0);
    NodeList packaging = project.getElementsByTagName("packaging");
    return packaging.getLength() > 0 ? packaging.item(0).getTextContent() : "jar";
  }

  private static Set<String> getDependencyRepositoryNames(Document pom) throws Exception {
    NodeList dependencies = pom.getElementsByTagName("dependency");
    Set<String> result = new HashSet<>();
    for (int index = 0; index < dependencies.getLength(); index++) {
      Element dependency = (Element) dependencies.item(index);
      String groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
      String artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
      String version = getDependencyVersion(dependency);
      if (dependency.getElementsByTagName("scope").item(0) != null) {
        String scope = dependency.getElementsByTagName("scope").item(0).getTextContent();
        if (scope.equals("test")) {
          continue;
        }
      }
      result.add(getRepositoryName(groupId, artifactId, version));
    }
    return result;
  }

  private static String getRepositoryName(String groupId, String artifactId, String version) {
    return escape(String.format("%s_%s_%s", groupId, artifactId, version));
  }

  private static String escape(String string) {
    return string.replaceAll("[.-]", "_");
  }

  // Versions can be of the form [A], [A,B], [A,] or [,A]. We just pick one.
  private static String getDependencyVersion(Element element) {
    String raw = element.getElementsByTagName("version").item(0).getTextContent();
    raw = raw.replaceAll("\\[|]", "");
    return raw.split(",")[0];
  }

  static class Artifact {
    String groupId;
    String artifactId;
    String version;
    String packaging;
    Set<String> dependencies;
    String repositoryName;

    Artifact(String groupId, String artifactId, String version, String packaging) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.packaging = packaging;
      this.repositoryName = getRepositoryName(groupId, artifactId, version);
    }

    String toArtifactUrl() {
      return String.format(
          ARTIFACT_TEMPLATE_URL,
          groupId.replaceAll("\\.", "/"),
          artifactId,
          version,
          artifactId,
          version,
          packaging);
    }

    String targetName() {
      return String.format("@%s//%s", repositoryName, packaging);
    }

    String rule() {
      return PACKAGING_TO_RULE.get(packaging);
    }
  }
}
