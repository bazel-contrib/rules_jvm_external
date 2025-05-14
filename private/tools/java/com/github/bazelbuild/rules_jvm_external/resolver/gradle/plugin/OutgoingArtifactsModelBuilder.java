/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import static com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin.Attributes.isPlatform;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.Conflict;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.model.DefaultOutgoingArtifactsModel;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.model.OutgoingArtifactsModel;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.io.Files;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ComponentResult;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.ResolvedVariantResult;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

public class OutgoingArtifactsModelBuilder implements ToolingModelBuilder {
  private static final String MODEL_NAME = OutgoingArtifactsModel.class.getName();

  public OutgoingArtifactsModelBuilder() {}

  @Override
  public boolean canBuild(String modelName) {
    return modelName.equals(MODEL_NAME);
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    ConfigurationContainer configs = project.getConfigurations();
    Configuration defaultConfig = configs.getByName("default");
    ResolvableDependencies resolvableDeps = defaultConfig.getIncoming();

    // Begin by constructing the possible graph of what we want. Gradle will
    // resolve things at the "module" level (`groupId:artifactId`) and will
    // "loose" results that only differ by classifier. Because of this, the
    // graph we have is possibly incomplete, but it's a good place to start
    // from.
    ResolvedComponentResult result = resolvableDeps.getResolutionResult().getRootComponent().get();
    HashSet<Conflict> conflicts = new HashSet<>();
    Graph<ResolvedComponentResult> graph = buildDependencyGraph(result, conflicts);

    // Given the (possibly incomplete) graph of dependencies, and the list of
    // possible coordinates, we can now make a decent attempt at
    // reconstructing something that includes all the dependencies that have
    // been downloaded.
    //
    // We now rely on the downloaded files to be named following the default
    // maven format so that we can calculate the coordinates that they would
    // represent.
    Set<ResolvedArtifact> allResolvedArtifacts =
        defaultConfig.getResolvedConfiguration().getLenientConfiguration().getArtifacts();
    Map<ComponentIdentifier, Set<File>> knownFiles = collectDownloadedFiles(allResolvedArtifacts);

    Set<Coordinates> remaining = removeAlreadyIncludedFiles(graph, knownFiles);

    Map<String, Set<String>> artifacts = reconstructDependencyGraph(project, graph, remaining);

    Map<String, String> convertedConflicts = new HashMap<>();
    for (Conflict conflict : conflicts) {
      convertedConflicts.put(conflict.getRequested().toString(), conflict.getResolved().toString());
    }

    return new DefaultOutgoingArtifactsModel(artifacts, convertedConflicts);
  }

  private Map<ComponentIdentifier, Set<File>> collectDownloadedFiles(Set<ResolvedArtifact> result) {
    Map<ComponentIdentifier, Set<File>> knownFiles = new HashMap<>();

    for (ResolvedArtifact artifactResult : result) {
      if (artifactResult.getFile() == null) {
        continue;
      }
      ComponentIdentifier id = artifactResult.getId().getComponentIdentifier();
      knownFiles.computeIfAbsent(id, ignored -> new HashSet<>()).add(artifactResult.getFile());
    }

    return Map.copyOf(knownFiles);
  }

  private Set<Coordinates> removeAlreadyIncludedFiles(
      Graph<ResolvedComponentResult> graph, Map<ComponentIdentifier, Set<File>> knownFiles) {
    Set<ModuleComponentIdentifier> ids =
        graph.nodes().stream()
            .map(ComponentResult::getId)
            .filter(rcr -> rcr instanceof ModuleComponentIdentifier)
            .map(rcr -> ((ModuleComponentIdentifier) rcr))
            .collect(Collectors.toSet());

    Set<Coordinates> toReturn = new HashSet<>();

    for (ModuleComponentIdentifier id : ids) {
      String fileName = getMavenFileName(id);
      Set<Coordinates> unused =
          knownFiles.getOrDefault(id, Set.of()).stream()
              .filter(f -> !fileName.equals(f.getName()))
              .map(f -> deriveCoordinates(id, f.getName()))
              .collect(Collectors.toSet());
      toReturn.addAll(unused);
    }

    return Set.copyOf(toReturn);
  }

  private Coordinates deriveCoordinates(ModuleComponentIdentifier id, String name) {
    // A maven file name is made of artifactId-version-classifier.extension

    String extension = Files.getFileExtension(name);

    // The classifier is sandwiched between the version and extension (which
    // is prefixed with a `.`)
    String prefix = id.getModule() + "-" + id.getVersion() + "-";
    String classifier = name.substring(prefix.length(), name.length() - extension.length() - 1);

    return new Coordinates(id.getGroup(), id.getModule(), extension, classifier, id.getVersion());
  }

  private String getMavenFileName(ModuleComponentIdentifier id) {
    return id.getModule() + "-" + id.getVersion() + ".jar";
  }

  private Graph<ResolvedComponentResult> buildDependencyGraph(
      ResolvedComponentResult result, Set<Conflict> conflicts) {
    MutableGraph<ResolvedComponentResult> toReturn = GraphBuilder.directed().build();
    Set<ComponentIdentifier> visited = new HashSet<>();
    amendDependencyGraph(toReturn, visited, conflicts, result);
    return ImmutableGraph.copyOf(toReturn);
  }

  private void amendDependencyGraph(
      MutableGraph<ResolvedComponentResult> toReturn,
      Set<ComponentIdentifier> visited,
      Set<Conflict> conflicts,
      ResolvedComponentResult result) {
    ComponentIdentifier id = result.getId();

    if (!visited.add(id)) {
      return;
    }

    if (id instanceof ModuleComponentIdentifier) {
      toReturn.addNode(result);
    }

    for (ResolvedVariantResult variant : result.getVariants()) {
      List<DependencyResult> depsForVariant = result.getDependenciesForVariant(variant);
      for (DependencyResult dep : depsForVariant) {
        if (dep instanceof ResolvedDependencyResult) {
          ResolvedDependencyResult resolved = (ResolvedDependencyResult) dep;
          ResolvedComponentResult selected = resolved.getSelected();

          if (selected.getId() instanceof ModuleComponentIdentifier) {
            toReturn.addNode(selected);
            toReturn.putEdge(result, selected);

            if (!resolved.getRequested().matchesStrictly(selected.getId())) {
              Conflict conflict =
                  new Conflict(
                      new Coordinates(selected.getId().toString()),
                      new Coordinates(resolved.getRequested().toString()));
              conflicts.add(conflict);
            }
          }

          amendDependencyGraph(toReturn, visited, conflicts, selected);
        } else {
          System.err.println(String.format("Cannot resolve %s (class %s)", dep, dep.getClass()));
        }
      }
    }
  }

  private Map<String, Set<String>> reconstructDependencyGraph(
          Project project,
          Graph<ResolvedComponentResult> graph,
          Set<Coordinates> orphaned) {
    // Get the list of dependencies that the user actually asked for
    Set<ExternalModuleDependency> requestedDeps = new HashSet<>();
    for (Configuration config : project.getConfigurations()) {
      config.getDependencies().stream()
          .filter(d -> d instanceof ExternalModuleDependency)
          .map(d -> (ExternalModuleDependency) d)
          .filter(d -> !isPlatform(d.getAttributes()))
          .forEach(requestedDeps::add);
    }

    // Now get the module identifiers for the requested deps
    Set<ModuleIdentifier> identifiers =
        requestedDeps.stream().map(ModuleVersionSelector::getModule).collect(Collectors.toSet());

    // And then find the results that match the requested artifacts.
    // These will form the root of our graph
    Set<ResolvedComponentResult> roots =
        graph.nodes().stream()
            .filter(rd -> rd.getId() instanceof ModuleComponentIdentifier)
            .filter(
                rd ->
                    identifiers.contains(
                        ((ModuleComponentIdentifier) rd.getId()).getModuleIdentifier()))
            .collect(Collectors.toSet());

    Map<String, Set<String>> toReturn = new HashMap<>();
    for (ResolvedComponentResult root : roots) {
      reconstructDependencyGraph(root, graph, toReturn);
    }

    // We don't know where to put orphaned coordinates, so make them top-level with no deps
    orphaned.forEach(c -> toReturn.put(c.toString(), Set.of()));

    return Map.copyOf(toReturn);
  }

  private void reconstructDependencyGraph(
      ResolvedComponentResult toVisit,
      Graph<ResolvedComponentResult> graph,
      Map<String, Set<String>> visited) {
    ComponentIdentifier tempId = toVisit.getId();

    if (!(tempId instanceof ModuleComponentIdentifier)) {
      return;
    }

    ModuleComponentIdentifier id = (ModuleComponentIdentifier) tempId;
    String key = createKey(id);

    if (visited.containsKey(key)) {
      return;
    }

    Set<ResolvedComponentResult> successors = graph.successors(toVisit);
    Set<ResolvedComponentResult> recurseInto =
        successors.stream()
            .filter(rd -> rd.getId() instanceof ModuleComponentIdentifier)
            .collect(Collectors.toSet());
    visited.put(
        key,
        recurseInto.stream()
            .map(rd -> createKey((ModuleComponentIdentifier) rd.getId()))
            .collect(Collectors.toSet()));
    for (ResolvedComponentResult dep : recurseInto) {
      reconstructDependencyGraph(dep, graph, visited);
    }
  }

  private String createKey(ModuleComponentIdentifier id) {
    StringBuilder coords = new StringBuilder();
    coords.append(id.getGroup()).append(":").append(id.getModule());
    if (id.getVersion() != null) {
      coords.append(":").append(id.getVersion());
    }
    return coords.toString();
  }
}
