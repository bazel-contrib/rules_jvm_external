package com.github.bazelbuild.rules_jvm_external.resolver.remote;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.hash.Hashing;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Downloader {

  private static final Logger LOG = Logger.getLogger(Downloader.class.getName());
  // Lifted from RealisedMavenModuleResolveMetadata in Gradle's source
  private static final Set<String> JAR_PACKAGINGS =
      Set.of("jar", "ejb", "bundle", "maven-plugin", "eclipse-plugin");
  // Packaging types (extensions) we know don't have fallbacks. I know `jar` is in both sets.
  private static final Set<String> NO_FALLBACK_PACKAGINGS =
      Set.of("jar", "tar.gz", "tar.bz2", "tar", "zip", "exe", "dll", "so");
  private static final Set<String> NO_FALLBACK_CLASSIFIERS = Set.of("sources", "javadoc");
  private final Path localRepository;
  private final Set<URI> repos;
  private final boolean cacheDownloads;
  private final HttpDownloader httpDownloader;

  public Downloader(
      Netrc netrc,
      Path localRepository,
      Collection<URI> repositories,
      EventListener listener,
      boolean cacheDownloads) {
    this.localRepository = localRepository;
    this.repos = Set.copyOf(repositories);
    this.cacheDownloads = cacheDownloads;
    this.httpDownloader = new HttpDownloader(netrc, listener);
  }

  public DownloadResult download(Coordinates coords) {
    DownloadResult result = performDownload(coords);
    if (result != null) {
      return result;
    }

    // There is a fallback we can use.
    // RealisedMavenModuleResolveMetadata.getArtifactsForConfiguration
    // says that if the artifact isn't a "known java packaging", then we should just look for a
    // `jar`
    // variant as well.
    if (isFallbackAvailable(coords)) {
      result = performDownload(coords.setExtension("jar"));
    }

    if (result != null) {
      return result;
    }

    // Are we dealing with a packaging dep? Download the `pom.xml` and check
    String originalTarget = coords.toRepoPath();
    String pomName = String.format("%s-%s.pom", coords.getArtifactId(), coords.getVersion());
    String pom = Paths.get(originalTarget).getParent().resolve(pomName).toString();

    DownloadResult pomResult = performDownload(coords, pom);
    if (pomResult == null) {
      System.out.println(
          "\n[WARNING] The POM for " + coords + " is missing, no dependency information available");
      return null;
    }
    if (pomResult.getPath().isPresent()) {
      // Quick hack to try and get the packaging from the pom file. We
      // _could_ try and build the model properly, but this quick and dirty
      // check should be sufficient

      try (InputStream is = Files.newInputStream(pomResult.getPath().get());
          BufferedInputStream bis = new BufferedInputStream(is)) {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(bis);
        doc.normalizeDocument();

        XPath xPath = XPathFactory.newInstance().newXPath();
        String value = xPath.compile("//project/packaging").evaluate(doc);

        if ("pom".equals(value)) {
          // We have an aggregating result.
          return new DownloadResult(coords, pomResult.getRepositories(), null, null);
        }
      } catch (IOException
          | ParserConfigurationException
          | SAXException
          | XPathExpressionException e) {
        throw new RuntimeException(e);
      }
    }

    throw new UriNotFoundException("Unable to download from any repo: " + coords);
  }

  private DownloadResult performDownload(Coordinates coordsToUse, String path) {
    Set<URI> repos = new LinkedHashSet<>();

    Path pathInRepo = null;

    // Check the local cache for the path first
    Path cachedResult = localRepository.resolve(path);
    if (Files.exists(cachedResult)) {
      pathInRepo = cachedResult;
    }

    String rjeAssumePresent = System.getenv("RJE_ASSUME_PRESENT");
    boolean assumedDownloaded = false;
    if (rjeAssumePresent != null) {
      assumedDownloaded = "1".equals(rjeAssumePresent) || Boolean.parseBoolean(rjeAssumePresent);
    }

    boolean downloaded = false;
    for (URI repo : this.repos) {
      if (pathInRepo == null) {
        LOG.fine(String.format("Downloading %s%n", coordsToUse));
        pathInRepo = httpDownloader.get(buildUri(repo, path));
        if (pathInRepo != null) {
          repos.add(repo);
          downloaded = true;

          if (cacheDownloads && !cachedResult.equals(pathInRepo)) {
            try {
              Files.createDirectories(cachedResult.getParent());
              Files.copy(pathInRepo, cachedResult, REPLACE_EXISTING);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          }
        }
      } else if (assumedDownloaded) { // path is set
        LOG.fine(String.format("Assuming %s is cached%n", coordsToUse));
        downloaded = true;
      } else if (httpDownloader.head(buildUri(repo, path))) { // path is set
        LOG.fine(String.format("Checking head of %s%n", coordsToUse));
        repos.add(repo);
        downloaded = true;
      }
    }

    if (!downloaded) {
      return null;
    }

    String sha256 = calculateSha256(pathInRepo);

    return new DownloadResult(coordsToUse, Set.copyOf(repos), pathInRepo, sha256);
  }

  private URI buildUri(URI baseUri, String pathInRepo) {
    String path = baseUri.getPath();
    if (!path.endsWith("/")) {
      path += "/";
    }
    path += pathInRepo;

    try {
      return new URI(
          baseUri.getScheme(),
          baseUri.getUserInfo(),
          baseUri.getHost(),
          baseUri.getPort(),
          path,
          baseUri.getQuery(),
          baseUri.getFragment());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private DownloadResult performDownload(Coordinates coords) {
    return performDownload(coords, coords.toRepoPath());
  }

  private boolean isFallbackAvailable(Coordinates coords) {
    String extension = coords.getExtension();
    if (extension.isEmpty() || "jar".equals(extension)) {
      return false;
    }

    if (NO_FALLBACK_PACKAGINGS.contains(extension)) {
      return false;
    }

    if (NO_FALLBACK_CLASSIFIERS.contains(coords.getClassifier())) {
      return false;
    }

    return !JAR_PACKAGINGS.contains(extension);
  }

  private String calculateSha256(Path path) {
    try {
      byte[] bytes = Files.readAllBytes(path);
      return Hashing.sha256().hashBytes(bytes).toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
