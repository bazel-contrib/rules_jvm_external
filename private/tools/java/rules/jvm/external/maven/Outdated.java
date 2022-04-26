package rules.jvm.external.maven;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Outdated {
  public static String getReleaseVersion(String repository, String groupId, String artifactId) {
    String url =
        String.format("%s/%s/%s/maven-metadata.xml",
            repository,
            groupId.replaceAll("\\.", "/"),
            artifactId);

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder;
    try {
      documentBuilder = documentBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      verboseLog(String.format("Caught exception %s", e));
      return null;
    }
    Document document;
    try {
      document = documentBuilder.parse(new URL(url).openStream());
    } catch (IOException | SAXException e) {
      verboseLog(String.format("Caught exception for %s: %s", url, e));
      return null;
    }
    return getReleaseVersion(document, url);
  }

  public static String getReleaseVersion(Document document, String documentUrl) {
    // example maven-metadata.xml
    // <metadata>
    //   <versioning>
    //     <latest>1.14.0-SNAPSHOT</latest>
    //     <release>1.13.0</release>
    //   </versioning>
    // </metadata>
    //
    // or
    //
    // <metadata>
    //   <groupId>javax.inject</groupId>
    //   <artifactId>javax.inject</artifactId>
    //   <version>1</version>
    //   <versioning>
    //     <versions>
    //       <version>1</version>
    //     </versions>
    //     <lastUpdated>20100720032040</lastUpdated>
    //   </versioning>
    // </metadata>
    Element metadataElement = document.getDocumentElement();
    Element versioningElement = getFirstChildElement(metadataElement, "versioning");
    if (versioningElement == null) {
      verboseLog(
          String.format(
              "Could not find <versioning> tag for %s, returning null version", documentUrl));
      return null;
    }

    // Note: we may want to add a flag to allow people to look for updates against
    // "latest" instead of "release"
    NodeList release = versioningElement.getElementsByTagName("release");
    if (release != null && release.getLength() > 0) {
      return release.item(0).getTextContent();
    }

    // No release info, default to the last version in the list.
    Element versionsElement = getFirstChildElement(versioningElement, "versions");
    if (versionsElement == null) {
      verboseLog(
          String.format(
              "Could not find <release> or <versions> tag for %s, returning null version",
              documentUrl));
      return null;
    }

    NodeList versions = versionsElement.getElementsByTagName("version");
    if (versions == null || versions.getLength() == 0) {
      verboseLog(
          String.format(
              "Could not find <release> tag and empty <versions> tag for %s, returning null version",
              documentUrl));
      return null;
    }

    // Grab last version in the list.
    return versions.item(versions.getLength() -1).getTextContent();
  }

  public static Element getFirstChildElement(Element element, String tagName) {
    NodeList nodeList = element.getElementsByTagName(tagName);
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        return (Element) node;
      }
    }
    return null;
  }

  public static void verboseLog(String logline) {
    if (System.getenv("RJE_VERBOSE") != null) {
      System.out.println(logline);
    }
  }

  public static void main(String[] args) throws IOException {
    verboseLog(String.format("Running outdated with args %s", Arrays.toString(args)));

    if (args.length != 2) {
      System.out.println("Usage: outdated <artifact_file_path> <repositories_file_path>");
      System.exit(1);
    }

    String artifactsFilePath = args[0];
    String repositoriesFilePath = args[1];

    List<String> artifacts = Files.readAllLines(Paths.get(artifactsFilePath), StandardCharsets.UTF_8);
    List<String> repositories = Files.readAllLines(Paths.get(repositoriesFilePath), StandardCharsets.UTF_8);

    System.out.println(String.format("Checking for updates of %d artifacts against the following repositories:", artifacts.size()));
    for (String repository : repositories) {
      System.out.println(String.format("\t%s", repository));
    }
    System.out.println();

    boolean foundUpdates = false;

    // Note: This should be straightforward to run in a thread and do multiple
    // update checks at once if we want to improve performance in the future.
    for (String artifact : artifacts) {
      if (artifact.isEmpty()) {
        continue;
      }
      String[] artifactParts = artifact.split(":");
      String groupId = artifactParts[0];
      String artifactId = artifactParts[1];
      String version = artifactParts[2];

      String releaseVersion = null;
      for (String repository : repositories) {
        releaseVersion = getReleaseVersion(repository, groupId, artifactId);
        if (releaseVersion != null) {
          verboseLog(String.format("Found version [%s] for %s:%s in %s", releaseVersion, groupId, artifactId, repository));
          // Should we search all repositories in the list for latest version instead of just the first
          // repository that has a version?
          break;
        }
      }

      if (releaseVersion == null) {
        verboseLog(String.format("Could not find version for %s:%s", groupId, artifactId));
      } else if (new ComparableVersion(releaseVersion).compareTo(new ComparableVersion(version)) > 0) {
        System.out.println(String.format("%s:%s [%s -> %s]", groupId, artifactId, version, releaseVersion));
        foundUpdates = true;
      }
    }

    if (!foundUpdates) {
      System.out.println("No updates found");
    }
  }
}
