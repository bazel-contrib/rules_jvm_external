package com.github.bazelbuild.rules_jvm_external.resolver;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.cmd.Config;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.DownloadResult;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.Downloader;
import com.github.bazelbuild.rules_jvm_external.resolver.ui.NullListener;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import com.google.gson.Gson;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public abstract class ResolverTestBase {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  protected Resolver resolver;

  protected abstract Resolver getResolver(Netrc netrc, EventListener listener);

  @Before
  public void createResolver() {
    resolver = getResolver(Netrc.create(null, new HashMap<>()), new NullListener());
  }

  @Test
  public void shouldResolveASingleCoordinate() {
    Coordinates coords = new Coordinates("com.example:foo:1.0");

    Path repo = MavenRepo.create().add(coords).getPath();

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(repo.toUri(), coords));

    assertEquals(1, resolved.nodes().size());
    assertEquals(coords, resolved.nodes().iterator().next());
  }

  @Test
  public void shouldResolveASingleCoordinateWithADep() {
    Coordinates main = new Coordinates("com.example:foo:1.0");
    Coordinates dep = new Coordinates("com.example:bar:1.0");

    Path repo = MavenRepo.create().add(dep).add(main, dep).getPath();

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(repo.toUri(), main));

    Set<Coordinates> nodes = resolved.nodes();
    assertEquals(2, nodes.size());
    assertEquals(ImmutableSet.of(dep), resolved.successors(main));
  }

  @Test
  public void shouldFetchTransitiveDependencies() {
    Coordinates grandDep = new Coordinates("com.example:grand-dep:1.0");
    Coordinates dep = new Coordinates("com.example:dep:2.0");
    Coordinates main = new Coordinates("com.example:main:3.0");

    Path repo = MavenRepo.create().add(grandDep).add(dep, grandDep).add(main, dep).getPath();

    Graph<Coordinates> graph = resolver.resolve(prepareRequestFor(repo.toUri(), main));

    Set<Coordinates> nodes = graph.nodes();
    assertTrue(nodes.contains(main));
    assertTrue(nodes.contains(dep));
    assertTrue(nodes.contains(grandDep));
  }

  @Test
  public void shouldAllowExclusionsAtTheCoordinateLevel() {
    Coordinates singleDep = new Coordinates("com.example:single-dep:1.0");
    Coordinates noDeps = new Coordinates("com.example:no-deps:1.0");

    Path repo = MavenRepo.create().add(noDeps).add(singleDep, noDeps).getPath();

    ResolutionRequest request =
        prepareRequestFor(repo.toUri() /* deliberately left blank */)
            .addArtifact(singleDep.toString(), "com.example:no-deps");

    Graph<Coordinates> graph = resolver.resolve(request);

    Set<Coordinates> nodes = graph.nodes();
    assertFalse(nodes.contains(noDeps));
    assertTrue(nodes.contains(singleDep));
  }

  @Test
  public void shouldAllowGlobalExclusions() {
    Coordinates singleDep = new Coordinates("com.example:single-dep:1.0");
    Coordinates noDeps = new Coordinates("com.example:no-deps:1.0");

    Path repo = MavenRepo.create().add(noDeps).add(singleDep, noDeps).getPath();

    ResolutionRequest request =
        prepareRequestFor(repo.toUri(), singleDep).exclude("com.example:no-deps");

    Graph<Coordinates> graph = resolver.resolve(request);

    Set<Coordinates> nodes = graph.nodes();
    assertFalse(nodes.contains(noDeps));
    assertTrue(nodes.contains(singleDep));
  }

  @Test
  public void shouldBeAbleToFetchCoordinatesWhichDifferByClassifier() {
    Coordinates jar = new Coordinates("com.example:thing:1.2.3");
    Coordinates classified = new Coordinates("com.example:thing:jar:sausages:1.2.3");

    Path repo = MavenRepo.create().add(jar).add(classified).getPath();

    Graph<Coordinates> resolved =
        resolver.resolve(prepareRequestFor(repo.toUri(), jar, classified));

    Set<Coordinates> nodes = resolved.nodes();
    assertEquals(2, nodes.size());
    assertTrue(nodes.toString(), nodes.contains(jar));
    assertTrue(nodes.toString(), nodes.contains(classified));
  }

  @Test
  public void shouldDownloadJarsOverHttp() throws IOException {
    Coordinates coords = new Coordinates("com.example:foo:1.0");

    Path repo = MavenRepo.create().add(coords).getPath();

    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", new PathHandler(repo));
    server.start();

    int port = server.getAddress().getPort();
    URI remote = URI.create("http://localhost:" + port);

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(remote, coords));

    assertEquals(1, resolved.nodes().size());
    assertEquals(coords, resolved.nodes().iterator().next());
  }

  @Test
  public void shouldDownloadOverHttpWithAuthenticationPassedInOnRepoUrl() throws IOException {
    Coordinates coords = new Coordinates("com.example:foo:1.0");

    Path repo = MavenRepo.create().add(coords).getPath();

    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    HttpContext context = server.createContext("/", new PathHandler(repo));
    context.setAuthenticator(
        new BasicAuthenticator("maven") {
          @Override
          public boolean checkCredentials(String username, String password) {
            return "cheese".equals(username) && "hunter2".equals(password);
          }
        });
    server.start();

    int port = server.getAddress().getPort();

    URI remote = URI.create("http://cheese:hunter2@localhost:" + port);

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(remote, coords));

    assertEquals(1, resolved.nodes().size());
    assertEquals(coords, resolved.nodes().iterator().next());
  }

  @Test
  public void shouldDownloadOverHttpWithAuthenticationGatheredFromNetrc() throws IOException {
    Netrc netrc =
        new Netrc(new Netrc.Credential("localhost", "cheese", "hunter2", null), new HashMap<>());
    assertAuthenticatedAccessWorks(netrc, "cheese", "hunter2");
  }

  @Test
  public void shouldDownloadOverHttpWithMachineSpecificAuthenticationFromNetrc()
      throws IOException {
    Netrc netrc =
        new Netrc(
            null,
            ImmutableMap.of(
                "localhost", new Netrc.Credential("localhost", "cheese", "hunter2", null)));
    assertAuthenticatedAccessWorks(netrc, "cheese", "hunter2");
  }

  @Test
  public void shouldIncludeConflictInformationInOutputFromResolution() {
    Coordinates older = new Coordinates("com.example:foo:1.0");
    Coordinates newer = new Coordinates("com.example:foo:1.5");

    Path repo = MavenRepo.create().add(older).add(newer).getPath();

    // TODO: write me
  }

  @Test
  public void shouldHandlePackagingPomsInDependencies() throws IOException {
    Coordinates parentCoords = new Coordinates("com.example:packaging:1.0.3");
    Model parent = new Model();
    parent.setGroupId(parentCoords.getGroupId());
    parent.setArtifactId(parentCoords.getArtifactId());
    parent.setVersion(parentCoords.getVersion());
    parent.setPackaging("pom");

    Coordinates first = new Coordinates("com.example:first:1.2.3");
    Coordinates second = new Coordinates("com.example:second:1.2.3");

    Path repo = MavenRepo.create().add(first).add(second).add(parent, first, second).getPath();

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(repo.toUri(), parentCoords));

    // We don't want nodes with a classifier of `pom` to have appeared in the graph
    assertEquals(Set.of(first, second, parentCoords), resolved.nodes());

    Path localRepo = Files.createTempDirectory("local");

    DownloadResult parentDownload =
        new Downloader(
                Netrc.fromUserHome(), localRepo, Set.of(repo.toUri()), new NullListener(), false)
            .download(parentCoords);

    assertTrue(parentDownload.getPath().isEmpty());
  }

  @Test
  public void shouldAssumeAPackagingPomIsAJarWhenDependedOnTransitively() {
    Coordinates grandParent = new Coordinates("com.example:grandparent:3.14.1");

    Coordinates parentCoords = new Coordinates("com.example:packaging:1.0.3");
    Model parent = new Model();
    parent.setGroupId(parentCoords.getGroupId());
    parent.setArtifactId(parentCoords.getArtifactId());
    parent.setVersion(parentCoords.getVersion());
    parent.setPackaging("pom");

    Coordinates first = new Coordinates("com.example:first:1.2.3");
    Coordinates second = new Coordinates("com.example:second:1.2.3");

    Path repo =
        MavenRepo.create()
            .add(first)
            .add(second)
            .add(parent, first, second)
            // We want the "parentCoords" to be referenced with a `type` element of `pom`. Within
            // `MavenRepo` we can do that by setting the parent coordinate's extension.
            .add(
                grandParent,
                new Coordinates(
                    parentCoords.getGroupId(),
                    parentCoords.getArtifactId(),
                    "pom",
                    null,
                    parentCoords.getVersion()))
            .getPath();

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(repo.toUri(), grandParent));

    assertEquals(Set.of(grandParent, parentCoords, first, second), resolved.nodes());
  }

  @Test
  public void packagingAttributeOfPomShouldBeRespected() throws IOException {
    Coordinates coords = new Coordinates("com.example:packaging:1.0.3");
    Model model = new Model();
    model.setGroupId(coords.getGroupId());
    model.setArtifactId(coords.getArtifactId());
    model.setVersion(coords.getVersion());
    model.setPackaging("aar");

    Path repo = MavenRepo.create().add(model).writePomFile(model).getPath();

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(repo.toUri(), coords));
    assertEquals(1, resolved.nodes().size());

    Coordinates resolvedCoords = resolved.nodes().iterator().next();
    assertEquals("aar", resolvedCoords.getExtension());
  }

  @Test
  public void shouldResolveAndDownloadItemIdentifiedByClassifierFromArgsFile() throws IOException {
    Map<String, Object> args =
        Map.of(
            "artifacts",
            List.of(
                Map.of(
                    "artifact", "artifact",
                    "group", "com.example",
                    "version", "7.8.9",
                    "classifier", "jdk15")));
    Path argsFile = tempFolder.newFolder("argsdir").toPath().resolve("config.json");
    Files.write(argsFile, new Gson().toJson(args).getBytes(UTF_8));

    Coordinates coords = new Coordinates("com.example", "artifact", null, "jdk15", "7.8.9");
    Path repo = MavenRepo.create().add(coords).getPath();

    Config config =
        new Config(new NullListener(), "--argsfile", argsFile.toAbsolutePath().toString());
    ResolutionRequest request = config.getResolutionRequest();
    request.addRepository(repo.toUri());

    Graph<Coordinates> resolved = resolver.resolve(request);

    assertEquals(resolved.nodes(), Set.of(coords));
  }

  @Test
  public void shouldNotCrashWhenPomFileIsIncorrect() {
    // This example is derived from org.apache.yetus:audience-annotations:0.11.0
    Coordinates coords = new Coordinates("com.example:bad-dep:123.1");
    Model model = new Model();
    model.setGroupId(coords.getGroupId());
    model.setArtifactId(coords.getArtifactId());
    model.setVersion(coords.getVersion());
    Dependency jdkDep = new Dependency();
    jdkDep.setGroupId("jdk.tools");
    jdkDep.setArtifactId("jdk.tools");
    jdkDep.setScope("system");
    jdkDep.setOptional("true");
    model.addDependency(jdkDep);

    Path repo = MavenRepo.create().add(model).getPath();
    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(repo.toUri(), coords));

    assertEquals(Set.of(coords), resolved.nodes());
  }

  @Test
  public void shouldIncludeFullDependencyGraphWithoutRemovingDuplicateEntries() {
    Coordinates sharedDep = new Coordinates("com.example:shared:7.8.9");
    Coordinates first = new Coordinates("com.example:first:1.2.3");
    Coordinates second = new Coordinates("com.example:second:3.4.5");

    Path repo =
        MavenRepo.create().add(sharedDep).add(first, sharedDep).add(second, sharedDep).getPath();

    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(repo.toUri(), first, second));
    assertEquals(3, resolved.nodes().size());

    Set<Coordinates> firstSuccessors = resolved.successors(first);
    assertEquals(Set.of(sharedDep), firstSuccessors);

    Set<Coordinates> secondSuccessors = resolved.successors(second);
    assertEquals(Set.of(sharedDep), secondSuccessors);
  }

  private void assertAuthenticatedAccessWorks(Netrc netrc, String user, String password)
      throws IOException {
    Coordinates coords = new Coordinates("com.example:foo:3.4.5");

    Path repo = MavenRepo.create().add(coords).getPath();

    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    HttpContext context = server.createContext("/", new PathHandler(repo));
    context.setAuthenticator(
        new BasicAuthenticator("maven") {
          @Override
          public boolean checkCredentials(String username, String pwd) {
            if (user == null) {
              return true;
            }
            return user.equals(username) && password.equals(pwd);
          }
        });
    server.start();

    int port = server.getAddress().getPort();

    URI remote = URI.create("http://localhost:" + port);

    Resolver resolver = getResolver(netrc, new NullListener());
    Graph<Coordinates> resolved = resolver.resolve(prepareRequestFor(remote, coords));

    assertEquals(1, resolved.nodes().size());
    assertEquals(coords, resolved.nodes().iterator().next());
  }

  protected ResolutionRequest prepareRequestFor(URI repo, Coordinates... coordinates) {
    ResolutionRequest request = new ResolutionRequest().addRepository(repo);
    for (Coordinates coordinate : coordinates) {
      request = request.addArtifact(coordinate.toString());
    }
    return request;
  }
}
