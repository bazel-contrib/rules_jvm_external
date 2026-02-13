package com.github.bazelbuild.rules_jvm_external.maven;

import com.google.devtools.build.runfiles.Runfiles;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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
  // This list came from
  // https://github.com/apache/maven/blob/master/maven-artifact/src/main/java/org/apache/maven/artifact/versioning/ComparableVersion.java#L307
  // and unfortunately ComparableVerison does not expose this in any public methods.
  private static final List<String> MAVEN_PRE_RELEASE_QUALIFIERS =
      Arrays.asList("alpha", "beta", "milestone", "cr", "rc", "snapshot");
  private static final String MAVEN_CENTRAL_ALIAS_HOST = "repo1.maven.org";
  private static final String MAVEN_CENTRAL_CANONICAL_HOST = "repo.maven.apache.org";
  private static final String PREFER_IPV6_ADDRESSES_TRUE =
      "-Djava.net.preferIPv6Addresses=true";
  private static final String PREFER_IPV6_ADDRESSES_FALSE =
      "-Djava.net.preferIPv6Addresses=false";
  private static final String PREFER_IPV4_STACK_TRUE = "-Djava.net.preferIPv4Stack=true";

  public static class ArtifactReleaseInfo {
    public String releaseVersion;
    public String preReleaseVersion;

    public ArtifactReleaseInfo(String releaseVersion, String preReleaseVersion) {
      this.releaseVersion = releaseVersion;
      this.preReleaseVersion = preReleaseVersion;
    }

    public boolean hasReleaseVersionGreatherThan(String version) {
      if (releaseVersion == null) {
        return false;
      } else {
        return new ComparableVersion(releaseVersion).compareTo(new ComparableVersion(version)) > 0;
      }
    }

    public boolean hasPreReleaseVersionGreatherThan(String version) {
      if (preReleaseVersion == null) {
        return false;
      } else {
        return new ComparableVersion(preReleaseVersion).compareTo(new ComparableVersion(version))
            > 0;
      }
    }
  }

  public static ArtifactReleaseInfo getReleaseVersion(
      String repository, String groupId, String artifactId) {
    String url =
        String.format(
            "%s/%s/%s/maven-metadata.xml", repository, groupId.replaceAll("\\.", "/"), artifactId);

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

  private static boolean isPreRelease(String version) {
    String canonicalVersion = new ComparableVersion(version).getCanonical();
    verboseLog(
        String.format(
            "Checking canonical version: %s of version: %s for pre-release qualifiers",
            canonicalVersion, version));
    return MAVEN_PRE_RELEASE_QUALIFIERS.stream().anyMatch(canonicalVersion::contains);
  }

  public static ArtifactReleaseInfo getReleaseVersion(Document document, String documentUrl) {
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

    String releaseVersion = null;
    String preReleaseVersion = null;
    // Note: we may want to add a flag to allow people to look for updates against
    // "latest" instead of "release"
    NodeList release = versioningElement.getElementsByTagName("release");
    if (release.getLength() > 0) {
      String version = release.item(0).getTextContent();
      if (isPreRelease(version)) {
        preReleaseVersion = version;
        verboseLog(String.format("Found pre-release version: %s", version));
      } else {
        return new ArtifactReleaseInfo(version, null);
      }
    } else {
      verboseLog(
          String.format(
              "Could not find <release> tag for %s, returning null version", documentUrl));
    }

    // If the release xml tag is missing then use the last version in the versions list.
    Element versionsElement = getFirstChildElement(versioningElement, "versions");
    if (versionsElement == null) {
      verboseLog(
          String.format(
              "Could not find <versions> tag for %s, returning null version", documentUrl));
      if (preReleaseVersion != null) {
        return new ArtifactReleaseInfo(null, preReleaseVersion);
      } else {
        return null;
      }
    }

    NodeList versions = versionsElement.getElementsByTagName("version");
    if (versions.getLength() == 0) {
      verboseLog(
          String.format("Found empty <versions> tag for %s, returning null version", documentUrl));
      if (preReleaseVersion != null) {
        return new ArtifactReleaseInfo(null, preReleaseVersion);
      } else {
        return null;
      }
    }

    TreeSet<String> sortedVersions = new TreeSet<>(Comparator.comparing(ComparableVersion::new));
    for (int i = 0; i < versions.getLength(); i++) {
      sortedVersions.add(versions.item(i).getTextContent());
    }
    for (String version : sortedVersions.descendingSet()) {
      if (!isPreRelease(version)) {
        verboseLog(String.format("Found non-pre-release version: %s", version));
        releaseVersion = version;
        break;
      }
    }

    return new ArtifactReleaseInfo(releaseVersion, preReleaseVersion);
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

  static List<String> repositoryCandidates(String repository) {
    List<String> candidates = new ArrayList<>();
    candidates.add(repository);

    URI repositoryUri;
    try {
      repositoryUri = new URI(repository);
    } catch (URISyntaxException e) {
      return candidates;
    }

    if (!MAVEN_CENTRAL_ALIAS_HOST.equalsIgnoreCase(repositoryUri.getHost())) {
      return candidates;
    }

    try {
      URI canonicalUri =
          new URI(
              repositoryUri.getScheme(),
              repositoryUri.getUserInfo(),
              MAVEN_CENTRAL_CANONICAL_HOST,
              repositoryUri.getPort(),
              repositoryUri.getPath(),
              repositoryUri.getQuery(),
              repositoryUri.getFragment());
      candidates.add(canonicalUri.toString());
    } catch (URISyntaxException e) {
      // Keep the original repository only if canonical URI construction fails.
    }

    return candidates;
  }

  static boolean shouldApplyIpv4Fallback(String javaToolOptions, String jdkJavaOptions) {
    String allOptions =
        (javaToolOptions == null ? "" : javaToolOptions)
            + " "
            + (jdkJavaOptions == null ? "" : jdkJavaOptions);

    return allOptions.contains(PREFER_IPV6_ADDRESSES_TRUE)
        && !allOptions.contains(PREFER_IPV6_ADDRESSES_FALSE)
        && !allOptions.contains(PREFER_IPV4_STACK_TRUE);
  }

  static boolean shouldRelaunchWithIpv4Fallback(
      String javaToolOptions,
      String jdkJavaOptions,
      String preferIpv4StackProperty,
      String preferIpv6AddressesProperty) {
    if (!shouldApplyIpv4Fallback(javaToolOptions, jdkJavaOptions)) {
      return false;
    }

    return !"true".equalsIgnoreCase(preferIpv4StackProperty)
        && !"false".equalsIgnoreCase(preferIpv6AddressesProperty);
  }

  private static int relaunchWithIpv4Fallback(String[] args) throws IOException, InterruptedException {
    List<String> command = new ArrayList<>();
    command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
    command.add(PREFER_IPV6_ADDRESSES_FALSE);
    command.add(PREFER_IPV4_STACK_TRUE);
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(Outdated.class.getName());
    command.addAll(Arrays.asList(args));

    Process process = new ProcessBuilder(command).inheritIO().start();
    return process.waitFor();
  }

  public static void printUpdatesFor(
      List<String> artifacts, List<String> repositories, boolean useLegacyOutputFormat) {
    boolean foundUpdates = false;

    // Note: This should be straightforward to run in a thread and do multiple
    // update checks at once if we want to improve performance in the future.
    for (String artifact : artifacts) {
      verboseLog(String.format("Checking artifact [%s]", artifact));
      if (artifact.isEmpty()) {
        continue;
      }
      String[] artifactParts = artifact.split(":");
      String groupId = artifactParts[0];
      String artifactId = artifactParts[1];

      // artifacts might have empty versions if they come from a BOM
      // In this case, skip the artifact.
      if (artifactParts.length < 3) {
        continue;
      }

      String version = artifactParts[2];

      ArtifactReleaseInfo artifactReleaseInfo = null;
      for (String repository : repositories) {
        for (String repositoryCandidate : repositoryCandidates(repository)) {
          artifactReleaseInfo = getReleaseVersion(repositoryCandidate, groupId, artifactId);

          if (artifactReleaseInfo != null) {
            // We return the result from the first repository instead of searching all repositories
            // for the artifact
            verboseLog(
                String.format(
                    "Found release version [%s] and pre-release version [%s] for %s:%s in %s",
                    artifactReleaseInfo.releaseVersion,
                    artifactReleaseInfo.preReleaseVersion,
                    groupId,
                    artifactId,
                    repositoryCandidate));
            break;
          }
        }
        if (artifactReleaseInfo != null) {
          break;
        }
      }

      if (artifactReleaseInfo == null) {
        verboseLog(String.format("Could not find version for %s:%s", groupId, artifactId));
      } else {
        if (artifactReleaseInfo.hasPreReleaseVersionGreatherThan(version)) {
          if (useLegacyOutputFormat) {
            System.out.println(
                String.format(
                    "%s:%s [%s -> %s]",
                    groupId, artifactId, version, artifactReleaseInfo.preReleaseVersion));

          } else {
            if (artifactReleaseInfo.hasReleaseVersionGreatherThan(version)) {
              System.out.println(
                  String.format(
                      "%s:%s [%s -> %s] (pre-release: %s)",
                      groupId,
                      artifactId,
                      version,
                      artifactReleaseInfo.releaseVersion,
                      artifactReleaseInfo.preReleaseVersion));
            } else {
              System.out.println(
                  String.format(
                      "%s:%s [%s] (pre-release: %s)",
                      groupId, artifactId, version, artifactReleaseInfo.preReleaseVersion));
            }
          }
          foundUpdates = true;

        } else if (artifactReleaseInfo.hasReleaseVersionGreatherThan(version)) {
          System.out.println(
              String.format(
                  "%s:%s [%s -> %s]",
                  groupId, artifactId, version, artifactReleaseInfo.releaseVersion));
          foundUpdates = true;
        }
      }
    }

    if (!foundUpdates) {
      System.out.println("No updates found");
    }
  }

  public static void verboseLog(String logline) {
    if (System.getenv("RJE_VERBOSE") != null) {
      System.out.println(logline);
    }
  }

  private static Set<String> runfileCandidates(String path) {
    Set<String> candidates = new LinkedHashSet<>();
    candidates.add(path);

    String normalized = path;
    while (normalized.startsWith("./")) {
      normalized = normalized.substring(2);
    }
    candidates.add(normalized);

    if (normalized.startsWith("external/")) {
      normalized = normalized.substring("external/".length());
      candidates.add(normalized);
    }

    while (normalized.startsWith("../")) {
      normalized = normalized.substring(3);
      candidates.add(normalized);
    }

    return candidates;
  }

  private static Path resolvePath(String path) throws IOException {
    Path directPath = Paths.get(path);
    if (Files.exists(directPath)) {
      return directPath;
    }

    Runfiles.Preloaded runfiles = Runfiles.preload();
    for (String runfilePath : runfileCandidates(path)) {
      if (runfilePath == null || runfilePath.isEmpty()) {
        continue;
      }

      String resolvedPathString;
      try {
        resolvedPathString = runfiles.unmapped().rlocation(runfilePath);
      } catch (IllegalArgumentException e) {
        continue;
      }

      if (resolvedPathString != null) {
        Path resolvedPath = Paths.get(resolvedPathString);
        if (Files.exists(resolvedPath)) {
          return resolvedPath;
        }
      }
    }

    return directPath;
  }

  public static void main(String[] args) throws IOException {
    if (shouldRelaunchWithIpv4Fallback(
        System.getenv("JAVA_TOOL_OPTIONS"),
        System.getenv("JDK_JAVA_OPTIONS"),
        System.getProperty("java.net.preferIPv4Stack"),
        System.getProperty("java.net.preferIPv6Addresses"))) {
      try {
        System.exit(relaunchWithIpv4Fallback(args));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while relaunching outdated with IPv4 fallback", e);
      }
      return;
    }
    verboseLog(String.format("Running outdated with args %s", Arrays.toString(args)));

    Path artifactsFilePath = null;
    Path bomsFilePath = null;
    Path repositoriesFilePath = null;
    boolean useLegacyOutputFormat = false;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--artifacts-file":
          artifactsFilePath = resolvePath(args[++i]);
          break;

        case "--boms-file":
          bomsFilePath = resolvePath(args[++i]);
          break;

        case "--repositories-file":
          repositoriesFilePath = resolvePath(args[++i]);
          break;

        case "--legacy-output":
          useLegacyOutputFormat = true;
          break;

        default:
          throw new IllegalArgumentException(
              "Unable to parse command line: " + Arrays.toString(args));
      }
    }

    Objects.requireNonNull(artifactsFilePath, "Artifacts file must be set.");
    Objects.requireNonNull(repositoriesFilePath, "Repositories file must be set.");

    List<String> artifacts = Files.readAllLines(artifactsFilePath, StandardCharsets.UTF_8);
    List<String> boms = List.of();
    if (bomsFilePath != null) {
      boms = Files.readAllLines(bomsFilePath, StandardCharsets.UTF_8);
      if (boms.size() == 1 && boms.get(0).isBlank()) {
        boms = List.of();
      }
    }
    List<String> repositories = Files.readAllLines(repositoriesFilePath, StandardCharsets.UTF_8);

    if (boms.size() > 0) {
      System.out.println(
          String.format(
              "Checking for updates of %d boms and %d artifacts against the following"
                  + " repositories:",
              boms.size(), artifacts.size()));
    } else {
      System.out.println(
          String.format(
              "Checking for updates of %d artifacts against the following repositories:",
              artifacts.size()));
    }
    for (String repository : repositories) {
      System.out.println(String.format("\t%s", repository));
    }
    System.out.println();

    if (boms.size() > 0) {
      System.out.println("BOMs");
      printUpdatesFor(boms, repositories, useLegacyOutputFormat);
      System.out.println();
      System.out.println("Artifacts");
    }

    printUpdatesFor(artifacts, repositories, useLegacyOutputFormat);
  }
}
