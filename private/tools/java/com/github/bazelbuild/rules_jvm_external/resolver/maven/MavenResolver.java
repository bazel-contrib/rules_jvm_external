package com.github.bazelbuild.rules_jvm_external.resolver.maven;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.ResolutionRequest;
import com.github.bazelbuild.rules_jvm_external.resolver.Resolver;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.collect.ImmutableList;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyCycle;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollectionContext;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DefaultDependencyManager;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.traverser.StaticDependencyTraverser;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;

public class MavenResolver implements Resolver {

  private final RemoteRepositoryFactory remoteRepositoryFactory;
  private final EventListener listener;

  public MavenResolver(Netrc netrc, EventListener listener) {
    this.remoteRepositoryFactory = new RemoteRepositoryFactory(netrc);
    this.listener = listener;
  }

  private Dependency createBom(
      com.github.bazelbuild.rules_jvm_external.resolver.Artifact artifact) {
    Coordinates coordinates = artifact.getCoordinates();

    Dependency bom =
        new Dependency(
            new DefaultArtifact(
                coordinates.getGroupId(),
                coordinates.getArtifactId(),
                "pom",
                "",
                coordinates.getVersion()),
            JavaScopes.RUNTIME);

    Set<Exclusion> excluded =
        artifact.getExclusions().stream().map(this::createExclusion).collect(Collectors.toSet());

    return bom.setScope("import").setExclusions(excluded);
  }

  private Dependency createDependency(
      com.github.bazelbuild.rules_jvm_external.resolver.Artifact source) {
    Artifact artifact;
    Coordinates coords = source.getCoordinates();
    artifact =
        new DefaultArtifact(
            coords.getGroupId(),
            coords.getArtifactId(),
            coords.getClassifier(),
            coords.getExtension(),
            coords.getVersion());

    Set<Exclusion> excluded =
        source.getExclusions().stream().map(this::createExclusion).collect(Collectors.toSet());

    return new Dependency(artifact, JavaScopes.RUNTIME).setExclusions(excluded);
  }

  private Exclusion createExclusion(Coordinates coordinates) {
    return new Exclusion(coordinates.getGroupId(), coordinates.getArtifactId(), "*", "*");
  }

  @Override
  public Graph<Coordinates> resolve(ResolutionRequest request) {
    List<RemoteRepository> repos =
        request.getRepositories().stream()
            .map(remoteRepositoryFactory::createFor)
            .collect(Collectors.toList());

    List<Dependency> boms =
        request.getBoms().stream().map(this::createBom).collect(Collectors.toList());
    List<Dependency> dependencies =
        request.getDependencies().stream().map(this::createDependency).collect(Collectors.toList());
    Set<Exclusion> globalExclusions =
        request.getGlobalExclusions().stream()
            .map(this::createExclusion)
            .collect(Collectors.toSet());

    RepositorySystem system = createRepositorySystem();
    ConsoleRepositoryListener consoleLog = new ConsoleRepositoryListener(listener);
    ErrorReportingListener errors = new ErrorReportingListener();
    CoordinateGatheringListener coordsListener = new CoordinateGatheringListener();
    RepositorySystemSession session =
        prepareSession(
            system,
            new DefaultDependencyManager(),
            new CompoundListener(consoleLog, errors, coordsListener),
            request.getLocalCache());

    List<RemoteRepository> repositories = new ArrayList<>(repos.size());
    repositories.add(createRemoteRepoFromLocalM2Cache(request.getLocalCache()));
    repositories.addAll(repos);

    List<Dependency> amendedDeps = addGlobalExclusions(globalExclusions, dependencies);
    List<Dependency> amendedBoms = addGlobalExclusions(globalExclusions, boms);

    consoleLog.setPhase("Resolving BOM artifacts");

    List<Dependency> managedDependencies =
        resolveArtifactsFromBoms(system, session, repositories, amendedBoms);

    managedDependencies = overrideDependenciesWithUserChoices(managedDependencies, dependencies);

    Artifact fakeRoot = new DefaultArtifact("com.example:bazel-dep-resolver:1.0.0");

    Dependency dep = new Dependency(fakeRoot, JavaScopes.COMPILE);
    DependencyCollectionContext depCollectionContext =
        new DefaultDependencyCollectionContext(session, null, dep, managedDependencies);
    DependencyManager derived =
        new DefaultDependencyManager().deriveChildManager(depCollectionContext);
    session =
        prepareSession(
            system,
            derived,
            new CompoundListener(consoleLog, errors, coordsListener),
            request.getLocalCache());

    consoleLog.setPhase("Gathering direct dependency coordinates");
    List<DependencyNode> directDependencies =
        resolveBaseDependencies(
            system, session, repositories, fakeRoot, managedDependencies, amendedDeps);

    List<Exception> exceptions = errors.getExceptions();
    if (!exceptions.isEmpty()) {
      Exception exception = exceptions.get(0);
      if (exception instanceof ModelBuildingException) {
        System.out.println("\n[WARNING] " + exception);
      } else if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else {
        throw new RuntimeException(exception);
      }
    }

    return buildGraph(coordsListener.getRemappings(), directDependencies);
  }

  private List<Dependency> overrideDependenciesWithUserChoices(
      List<Dependency> managedDependencies, List<Dependency> dependencies) {
    // Add artifacts people have requested to the managed dependencies
    // Without this, the versions requested in BOMs will be preferred to the
    // one that the user requested
    Map<String, Set<String>> groupIdsAndArtifactsIds = new HashMap<>();
    dependencies.stream()
        .map(Dependency::getArtifact)
        .filter(artifact -> artifact.getVersion() != null)
        .filter(artifact -> !artifact.getVersion().isEmpty())
        .forEach(
            artifact -> {
              Set<String> group =
                  groupIdsAndArtifactsIds.computeIfAbsent(
                      artifact.getGroupId(), str -> new HashSet<>());
              group.add(artifact.getArtifactId());
            });

    ImmutableList.Builder<Dependency> toReturn = ImmutableList.builder();
    toReturn.addAll(dependencies);

    // Remove items from managedDependencies where the group and artifact ids match first order deps
    managedDependencies.stream()
        .filter(
            dep -> {
              Artifact artifact = dep.getArtifact();
              Set<String> group =
                  groupIdsAndArtifactsIds.getOrDefault(artifact.getGroupId(), Set.of());
              return !group.contains(artifact.getArtifactId());
            })
        .forEach(toReturn::add);

    return toReturn.build();
  }

  private List<Dependency> resolveArtifactsFromBoms(
      RepositorySystem system,
      RepositorySystemSession session,
      List<RemoteRepository> repositories,
      List<Dependency> amendedBoms) {
    Set<Dependency> managedDependencies = new HashSet<>();

    for (Dependency bom : amendedBoms) {
      ArtifactDescriptorRequest request =
          new ArtifactDescriptorRequest(bom.getArtifact(), repositories, JavaScopes.COMPILE);
      try {
        ArtifactDescriptorResult result = system.readArtifactDescriptor(session, request);
        managedDependencies.addAll(result.getManagedDependencies());
      } catch (ArtifactDescriptorException e) {
        throw new RuntimeException(e);
      }
    }

    return ImmutableList.copyOf(managedDependencies);
  }

  private List<Dependency> addGlobalExclusions(
      Set<Exclusion> globalExclusions, List<Dependency> dependencies) {
    return dependencies.stream()
        .map(
            dep -> {
              Set<Exclusion> allExclusions = new HashSet<>(globalExclusions);
              allExclusions.addAll(dep.getExclusions());

              return new Dependency(
                  dep.getArtifact(), dep.getScope(), dep.getOptional(), allExclusions);
            })
        .collect(ImmutableList.toImmutableList());
  }

  private Graph<Coordinates> buildGraph(
      Map<Coordinates, Coordinates> remappings, Collection<DependencyNode> directDependencies) {
    MutableGraph<Coordinates> toReturn = GraphBuilder.directed().allowsSelfLoops(true).build();
    DependencyVisitor collector =
        new TreeDependencyVisitor(
            new DependencyNodeVisitor(
                node -> {
                  final DependencyNode actualNode = getDependencyNode(node);

                  Artifact artifact = amendArtifact(actualNode.getArtifact());
                  Coordinates from = MavenCoordinates.asCoordinates(artifact);
                  Coordinates remapped = remappings.getOrDefault(from, from);
                  toReturn.addNode(remapped);

                  actualNode.getChildren().stream()
                      .map(DependencyNode::getArtifact)
                      .map(this::amendArtifact)
                      .map(MavenCoordinates::asCoordinates)
                      .map(c -> remappings.getOrDefault(c, c))
                      .forEach(
                          to -> {
                            toReturn.addNode(to);
                            toReturn.putEdge(remapped, to);
                          });
                }));
    directDependencies.forEach(node -> node.accept(collector));

    return ImmutableGraph.copyOf(toReturn);
  }

  private static DependencyNode getDependencyNode(DependencyNode node) {
    Map<?, ?> data = node.getData();
    if (data != null) {
      Object winner = data.get(ConflictResolver.NODE_DATA_WINNER);
      if (winner instanceof DependencyNode) {
        return (DependencyNode) winner;
      }
    }
    return node;
  }

  private Artifact amendArtifact(Artifact artifact) {
    // If someone has depended on an aggregating pom, the `type` or `extension` will be `pom`.
    // However, in `rules_jvm_external`, we pretend that these are actually `jar` files, and
    // then make things up from there.
    if (!"pom".equals(artifact.getExtension())) {
      return artifact;
    }

    return new DefaultArtifact(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        artifact.getClassifier(),
        null,
        artifact.getVersion());
  }

  private DefaultRepositorySystemSession prepareSession(
      RepositorySystem system,
      DependencyManager dependencyManager,
      RepositoryListener listener,
      Path localCache) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    LocalRepository localRepository = new LocalRepository(localCache.toAbsolutePath().toString());
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));

    session.setDependencyManager(dependencyManager);

    session.setDependencyTraverser(new StaticDependencyTraverser(true));

    session.setRepositoryListener(listener);

    // Only resolve from repos that we have been asked to use.
    session.setIgnoreArtifactDescriptorRepositories(true);

    Map<Object, Object> configProperties = new HashMap<>();
    configProperties.putAll(System.getProperties());

    // Use the breadth-first dependency collector. This allows us to work out the dependency graph
    // in parallel.
    // This only applies to the step where we calculate the dependency graph, not the actual
    // downloading of those
    // dependencies (which is handled by the `Downloader`.
    configProperties.put("aether.dependencyCollector.impl", "bf");
    // And set the number of threads to use when figuring out how many dependencies to download in
    // parallel.
    configProperties.put(
        "maven.artifact.threads", String.valueOf(Runtime.getRuntime().availableProcessors()));
    session.setConfigProperties(Map.copyOf(configProperties));
    session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
    session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

    session.setSystemProperties(System.getProperties());

    return session;
  }

  private static RepositorySystem createRepositorySystem() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    return locator.getService(RepositorySystem.class);
  }

  private List<DependencyNode> resolveBaseDependencies(
      RepositorySystem system,
      RepositorySystemSession session,
      Collection<RemoteRepository> repositories,
      Artifact root,
      List<Dependency> managedDependencies,
      List<Dependency> allDependencies) {
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRootArtifact(root);
    collectRequest.setRequestContext(JavaScopes.RUNTIME);

    collectRequest.setManagedDependencies(managedDependencies);
    collectRequest.setDependencies(allDependencies);

    for (RemoteRepository repository : repositories) {
      collectRequest.addRepository(repository);
    }

    CollectResult collectResult;
    try {
      collectResult = system.collectDependencies(session, collectRequest);
    } catch (DependencyCollectionException e) {
      throw new RuntimeException(e);
    }

    if (!collectResult.getExceptions().isEmpty()) {
      Exception exception = collectResult.getExceptions().get(0);
      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      }
      throw new RuntimeException(exception);
    }

    List<DependencyCycle> cycles = collectResult.getCycles();
    if (!cycles.isEmpty()) {
      Set<DependencyCycle> illegalCycles = new LinkedHashSet<>();
      for (DependencyCycle cycle : cycles) {
        // If the cycle is to the same group and coordinate, it's probably just fine
        List<Dependency> precedingDeps = cycle.getPrecedingDependencies();
        Artifact parent = precedingDeps.get(precedingDeps.size() - 1).getArtifact();

        for (Dependency dep : cycle.getCyclicDependencies()) {
          Artifact artifact = dep.getArtifact();
          if (!parent.getGroupId().equals(artifact.getGroupId())
              || !parent.getArtifactId().equals(artifact.getArtifactId())) {
            illegalCycles.add(cycle);
            break;
          }
        }
      }
      if (!illegalCycles.isEmpty()) {
        throw new RuntimeException(
            "Cycles detected: \n"
                + illegalCycles.stream().map(c -> "  " + c).collect(Collectors.joining("\n")));
      }
    }

    return collectResult.getRoot().getChildren();
  }

  private RemoteRepository createRemoteRepoFromLocalM2Cache(Path localCache) {
    return remoteRepositoryFactory.createFor(localCache.toUri());
  }
}
