// Copyright 2025 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import static com.github.bazelbuild.rules_jvm_external.resolver.PackagingMappings.mapPackagingToExtension;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependency;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependencyImpl;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependencyModel;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependencyModelImpl;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleResolvedArtifact;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleResolvedArtifactImpl;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleResolvedDependency;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleResolvedDependencyImpl;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleUnresolvedDependency;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleUnresolvedDependencyImpl;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.query.ArtifactResolutionQuery;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ComponentSelectionReason;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.ResolvedVariantResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

/**
 * GradleDependencyModelBuilder implenents the core functionality as part of a plugin to resolve the
 * dependency graph, fetch and associate relevant artifacts, track unresolved dependencies, report
 * any failures and the final tooling model back to the resolver
 */
public class GradleDependencyModelBuilder implements ToolingModelBuilder {

  @Override
  public boolean canBuild(String modelName) {
    return modelName.equals(GradleDependencyModel.class.getName());
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    GradleDependencyModel gradleDependencyModel = new GradleDependencyModelImpl();
    // We only resolve dependencies and fetch artifacts for this configuration
    // similar to how we do in the Maven resolver
    Configuration cfg = project.getConfigurations().getByName("runtimeClasspath");

    List<GradleDependency> declaredDeps = collectDeclaredDependencies(cfg);
    // This stores the mapping between coordinates to the GradleResolvedDependency interface
    // which contains the resolved dependency information from the tooling API, additionally it'll
    // also
    // be used to attach the actual artifacts later
    ConcurrentHashMap<String, GradleResolvedDependency> variantGradleResolvedDependencyMap =
        new ConcurrentHashMap<>();
    // We get the root nodes in the dependency graph (or rather forest here since there can be
    // disjoint trees)
    List<GradleResolvedDependency> resolvedRoots =
        collectResolvedDependencies(cfg, variantGradleResolvedDependencyMap);

    // Collect any unresolved dependencies from the runtimeClasspath configuration
    List<GradleUnresolvedDependency> unresolvedDependenciesRuntimeClasspath =
        getUnresolvedDependencies(cfg);

    List<Dependency> unresolvedDependencies =
        unresolvedDependenciesRuntimeClasspath.stream()
            .map(
                dep -> {
                  String coords = dep.getGroup() + ":" + dep.getName() + ":" + dep.getVersion();
                  return project.getDependencies().create(coords);
                })
            .collect(Collectors.toList());

    // Create a configuration to resolve android dependencies (it can be used for other platforms
    // in the future as well like Kotlin multiplatform).
    List<Dependency> detachedDeps = new ArrayList<>(unresolvedDependencies);

    // Add all declared dependencies to the detached configuration to ensure we have
    // all necessary BOMs/platforms and constraints available for resolution.
    // This is critical for dependencies that rely on BOMs for versioning.
    detachedDeps.addAll(cfg.getAllDependencies());

    Configuration detachedCfg =
        project.getConfigurations().detachedConfiguration(detachedDeps.toArray(new Dependency[0]));
    // Detached configurations are not covered by `configurations.all`, so the forced module
    // versions from the generated build script do not apply to them automatically. Without
    // them, pinned modules can resolve at a different version during the retry.
    Set<String> forcedModules =
        cfg.getResolutionStrategy().getForcedModules().stream()
            .map(m -> m.getGroup() + ":" + m.getName() + ":" + m.getVersion())
            .collect(Collectors.toSet());
    if (!forcedModules.isEmpty()) {
      detachedCfg.getResolutionStrategy().force(forcedModules.toArray());
    }
    // Ensure the detached configuration respects the same exclude rules as the original
    // configuration
    if (!cfg.getExcludeRules().isEmpty()) {
      // Ensure excludes are applied to the detached config
      for (org.gradle.api.artifacts.ExcludeRule rule : cfg.getExcludeRules()) {
        detachedCfg.exclude(
            java.util.Map.of(
                "group", rule.getGroup(),
                "module", rule.getModule()));
      }
    }

    // build the updated dependency graph with the detached configuration for all the
    // dependencies that we couldn't resolve with the default configuration. When everything
    // resolved, skip the retry: the detached configuration resolves without the main
    // configuration's attributes, so it can fail or pick different versions, and merging
    // those results would corrupt an already-complete graph.
    boolean retryUnresolved = !unresolvedDependenciesRuntimeClasspath.isEmpty();
    List<GradleResolvedDependency> resolvedDetachedRoots =
        retryUnresolved
            ? resolveDetachedGraph(detachedCfg, variantGradleResolvedDependencyMap)
            : List.of();

    List<GradleResolvedDependency> roots =
        Streams.concat(resolvedRoots.stream(), resolvedDetachedRoots.stream())
            .collect(Collectors.toList());
    // Use the ArtifactView API to get all the resolved artifacts (jars, aars)
    // The ArtifactView API doesn't download some of the classifiers by default, so we handle that
    // here
    collectAllResolvedArtifacts(
        project, cfg, detachedCfg, variantGradleResolvedDependencyMap, declaredDeps);
    gradleDependencyModel.getResolvedDependencies().addAll(roots);

    // if anything is still unresolved, then add it for reporting
    if (retryUnresolved) {
      gradleDependencyModel
          .getUnresolvedDependencies()
          .addAll(getUnresolvedDependencies(detachedCfg));
    }
    return gradleDependencyModel;
  }

  private List<GradleResolvedDependency> resolveDetachedGraph(
      Configuration detachedCfg,
      ConcurrentHashMap<String, GradleResolvedDependency> variantGradleResolvedDependencyMap) {
    return collectResolvedDependencies(detachedCfg, variantGradleResolvedDependencyMap);
  }

  private List<GradleDependency> collectDeclaredDependencies(Configuration cfg) {
    List<GradleDependency> dependencies = new ArrayList<>();
    for (Dependency dependency : cfg.getAllDependencies()) {
      if (dependency instanceof ModuleDependency) {
        ModuleDependency externalDependency = (ModuleDependency) dependency;
        if (externalDependency.getArtifacts().isEmpty()) {
          dependencies.add(
              new GradleDependencyImpl(
                  externalDependency.getGroup(),
                  externalDependency.getName(),
                  externalDependency.getVersion(),
                  null,
                  null,
                  null));
        } else {
          for (DependencyArtifact artifact : externalDependency.getArtifacts()) {
            GradleDependency dep =
                new GradleDependencyImpl(
                    externalDependency.getGroup(),
                    externalDependency.getName(),
                    externalDependency.getVersion(),
                    null,
                    artifact.getClassifier(),
                    artifact.getExtension());
            dependencies.add(dep);
          }
        }
      }
    }
    return dependencies;
  }

  private boolean isBom(ResolvedDependencyResult result) {
    for (Attribute attribute : result.getRequested().getAttributes().keySet()) {
      // Gradle sets this attributes for BOMs
      if (attribute.getName().equals("org.gradle.category")) {
        return result.getRequested().getAttributes().getAttribute(attribute).equals("platform");
      }
    }
    return false;
  }

  private List<GradleResolvedDependency> collectResolvedDependencies(
      Configuration cfg, ConcurrentMap<String, GradleResolvedDependency> variantDependencyMap) {
    List<GradleResolvedDependency> resolvedRoots = new ArrayList<>();
    ResolutionResult result = cfg.getIncoming().getResolutionResult();
    ResolvedComponentResult root = result.getRoot();

    if (isVerbose()) {
      System.err.println("DEBUG: Resolving configuration: " + cfg.getName());
      System.err.println("Dependency graph: ");
    }

    for (DependencyResult dep : root.getDependencies()) {
      if (dep instanceof ResolvedDependencyResult) {
        ResolvedDependencyResult rdep = (ResolvedDependencyResult) dep;
        if (isVerbose()) {
          System.err.println(
              "  RESOLVED ROOT: "
                  + rdep.getSelected().getId()
                  + " (from "
                  + rdep.getRequested()
                  + ")");
        }
        ResolvedComponentResult selected = rdep.getSelected();
        // we don't want to recurse through a BOM's artifacts
        // as they wil be explicitly specified
        if (isBom(rdep)) {
          continue;
        }

        Set<String> visited = new HashSet<>();
        // walk the resolved component graph in depth-first manner
        // and collect all the resolved dependencies
        GradleResolvedDependency info =
            walkResolvedVariant(
                selected, rdep.getResolvedVariant(), visited, variantDependencyMap, 1);
        if (info == null) {
          continue;
        }
        if (rdep.getRequested() instanceof ModuleComponentSelector) {
          String requested = ((ModuleComponentSelector) rdep.getRequested()).getVersion();
          info.addRequestedVersion(requested);
          info.setConflict(info.getRequestedVersions().size() > 1);
        }

        resolvedRoots.add(info);
      }
    }
    return resolvedRoots;
  }

  private List<GradleUnresolvedDependency> getUnresolvedDependencies(Configuration cfg) {
    List<GradleUnresolvedDependency> unresolvedDependencies = new ArrayList<>();
    ResolutionResult result = cfg.getIncoming().getResolutionResult();
    for (DependencyResult dep : result.getAllDependencies()) {
      if (dep instanceof UnresolvedDependencyResult) {
        UnresolvedDependencyResult rdep = (UnresolvedDependencyResult) dep;
        ModuleComponentSelector selector = (ModuleComponentSelector) rdep.getAttempted();
        Throwable failure = rdep.getFailure();

        if (isVerbose()) {
          System.err.println(
              "UNRESOLVED in "
                  + cfg.getName()
                  + ": "
                  + selector.getGroup()
                  + ":"
                  + selector.getModule()
                  + ":"
                  + selector.getVersion()
                  + " Reason: "
                  + failure);
        }

        GradleUnresolvedDependency.FailureReason reason =
            GradleUnresolvedDependency.FailureReason.INTERNAL;
        // This is an internal API, so we can't check for the type
        // but this maps to artifact not being found and not some other error
        if (failure
            .toString()
            .contains("org.gradle.internal.resolve.ModuleVersionNotFoundException")) {
          reason = GradleUnresolvedDependency.FailureReason.NOT_FOUND;
        }
        GradleUnresolvedDependency unresolved =
            new GradleUnresolvedDependencyImpl(
                selector.getGroup(),
                selector.getModule(),
                selector.getVersion(),
                reason,
                failure.getMessage());
        unresolvedDependencies.add(unresolved);
      }
    }
    return unresolvedDependencies;
  }

  private GradleResolvedDependency walkResolvedVariant(
      ResolvedComponentResult component,
      ResolvedVariantResult variant,
      Set<String> visited,
      Map<String, GradleResolvedDependency> variantDependencyMap,
      int depth) {
    if (variant == null) {
      return null;
    }
    String key = variantKey(component, variant);

    // Handle cycles as they can exist in resolution
    if (visited.contains(key)) {
      return variantDependencyMap.get(key);
    }

    visited.add(key);
    if (isVerbose()) {
      System.err.println(
          "  ".repeat(depth)
              + "→ "
              + componentKey(component)
              + " ["
              + variant.getDisplayName()
              + "]");
    }

    // We might visit the same node multiple times through the graph, so check if we've visited
    // node before
    GradleResolvedDependency info = variantDependencyMap.get(key);
    if (info == null) {
      info = createResolvedVariant(component, variant);
    }

    info.addRequestedVersion(component.getModuleVersion().getVersion());

    List<GradleResolvedDependency> children = new ArrayList<>();

    for (DependencyResult dep : component.getDependenciesForVariant(variant)) {
      if (!(dep instanceof ResolvedDependencyResult)) {
        continue;
      }

      ResolvedDependencyResult resolvedDep = (ResolvedDependencyResult) dep;
      ResolvedComponentResult selected = resolvedDep.getSelected();

      // Skip dependency constraint edges
      // These are not actual dependencies but show up as edges in the graph.
      // If we don't handle this, this can lead to cycles in the graph
      if (resolvedDep.isConstraint()) {
        continue;
      }

      GradleResolvedDependency child =
          walkResolvedVariant(
              selected, resolvedDep.getResolvedVariant(), visited, variantDependencyMap, depth + 1);
      if (child == null) {
        continue;
      }
      if (resolvedDep.getRequested() instanceof ModuleComponentSelector) {
        String requestedVersion =
            ((ModuleComponentSelector) resolvedDep.getRequested()).getVersion();
        child.addRequestedVersion(requestedVersion);
        child.setConflict(child.getRequestedVersions().size() > 1);
      }

      children.add(child);
    }

    ComponentSelectionReason reason = component.getSelectionReason();
    List<String> bomDescriptions =
        reason.getDescriptions().stream()
            .map(descriptor -> descriptor.getDescription().toLowerCase())
            .filter(
                description ->
                    description.contains("platform") || description.contains("constraint"))
            .collect(Collectors.toList());
    info.setFromBom(!bomDescriptions.isEmpty());

    info.setChildren(children);
    info.setConflict(info.getRequestedVersions().size() > 1);
    variantDependencyMap.put(key, info);
    return info;
  }

  // The ArtifactView api doesn't provide a way to obtain the classifier (the legacy API does but it
  // doesn't provide a way to obtain additional artifacts like javadoc/sources from what I can tell
  // so we interpolate the classifier based on the artifact file
  private String extractClassifier(File file, ComponentIdentifier id) {
    if (!(id instanceof ModuleComponentIdentifier)) {
      return null;
    }

    ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
    String version = module.getVersion();
    String artifact = module.getModule();

    String name = file.getName(); // e.g. lib-1.0-sources.jar, lib-1.0.pom

    // Strip extension
    int dotIndex = name.lastIndexOf('.');
    if (dotIndex <= 0) {
      return null;
    }
    String nameWithoutExt = name.substring(0, dotIndex);

    String expectedBase = artifact + "-" + version;

    if (!nameWithoutExt.startsWith(expectedBase)) {
      return null;
    }

    String suffix = nameWithoutExt.substring(expectedBase.length());
    return (suffix.startsWith("-")) ? suffix.substring(1) : null;
  }

  private void collectAllResolvedArtifacts(
      Project project,
      Configuration runtimeClassPathCfg,
      Configuration detachedCfg,
      Map<String, GradleResolvedDependency> variantDependencyMap,
      List<GradleDependency> declaredDeps) {
    Map<String, List<GradleResolvedDependency>> dependenciesByComponent =
        indexByComponent(variantDependencyMap.values());

    // Collect JAR artifacts - we need to check both JAVA_API and JAVA_RUNTIME because some
    // libraries (like resilience4j) only publish runtime variants, while others might only
    // have API variants. We'll collect from both and merge them.
    ArtifactView apiJars =
        runtimeClassPathCfg
            .getIncoming()
            .artifactView(
                spec -> {
                  spec.setLenient(true);
                  spec.attributes(
                      attrs ->
                          attrs.attribute(
                              Usage.USAGE_ATTRIBUTE,
                              project.getObjects().named(Usage.class, Usage.JAVA_API)));
                });

    ArtifactView runtimeJars =
        runtimeClassPathCfg
            .getIncoming()
            .artifactView(
                spec -> {
                  spec.setLenient(true);
                  spec.attributes(
                      attrs ->
                          attrs.attribute(
                              Usage.USAGE_ATTRIBUTE,
                              project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME)));
                });

    // collect JAR artifacts from both views
    collectArtifactsFromArtifactView(apiJars, dependenciesByComponent);
    collectArtifactsFromArtifactView(runtimeJars, dependenciesByComponent);

    ArtifactView aarView =
        detachedCfg
            .getIncoming()
            .artifactView(
                spec -> {
                  spec.setLenient(true); // tolerate dependencies without AAR variants
                  spec.attributes(
                      attrs -> {
                        attrs.attribute(
                            Usage.USAGE_ATTRIBUTE,
                            project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
                        attrs.attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            project.getObjects().named(LibraryElements.class, "aar"));
                      });
                  spec.withVariantReselection();
                });

    // Collect Android artifacts  (AARs)
    collectArtifactsFromArtifactView(aarView, dependenciesByComponent);

    // Also collect JARs from the detached configuration, as it contains resolved versions
    // of dependencies that failed in the main runtimeClasspath (e.g. due to missing versions
    // provided by BOMs)
    ArtifactView detachedApiJars =
        detachedCfg
            .getIncoming()
            .artifactView(
                spec -> {
                  spec.setLenient(true);
                  spec.attributes(
                      attrs ->
                          attrs.attribute(
                              Usage.USAGE_ATTRIBUTE,
                              project.getObjects().named(Usage.class, Usage.JAVA_API)));
                });
    ArtifactView detachedRuntimeJars =
        detachedCfg
            .getIncoming()
            .artifactView(
                spec -> {
                  spec.setLenient(true);
                  spec.attributes(
                      attrs ->
                          attrs.attribute(
                              Usage.USAGE_ATTRIBUTE,
                              project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME)));
                });
    collectArtifactsFromArtifactView(detachedApiJars, dependenciesByComponent);
    collectArtifactsFromArtifactView(detachedRuntimeJars, dependenciesByComponent);

    // Collect POM files explicitly as gradle doesn't fetch them unless requested
    collectPOMsForAllComponents(project, dependenciesByComponent, declaredDeps);
    // Don't collect conflicting version artifacts - we only need artifacts for the resolved graph
    // The conflicts are tracked for reporting purposes only, we don't need to download rejected
    // versions
  }

  private void collectArtifactsFromArtifactView(
      ArtifactView artifactView,
      Map<String, List<GradleResolvedDependency>> dependenciesByComponent) {
    Set<ResolvedArtifactResult> resolvedArtifactResults =
        artifactView.getArtifacts().getArtifacts();
    for (ResolvedArtifactResult artifact : resolvedArtifactResults) {
      ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
      if (!(identifier instanceof ModuleComponentIdentifier)) {
        continue;
      }
      ModuleComponentIdentifier module = (ModuleComponentIdentifier) identifier;
      if (artifact.getFile() != null) {
        GradleResolvedArtifact resolvedArtifact = new GradleResolvedArtifactImpl();
        resolvedArtifact.setFile(artifact.getFile());
        resolvedArtifact.setClassifier(extractClassifier(artifact.getFile(), identifier));
        if (artifact.getVariant() != null) {
          resolvedArtifact.setVariantCapabilities(capabilityKeys(artifact.getVariant()));
        }
        String fileExtension = Files.getFileExtension(artifact.getFile().getName());
        resolvedArtifact.setExtension(mapPackagingToExtension(fileExtension));

        GradleResolvedDependency resolvedDependency =
            findOwningDependency(
                dependenciesByComponent,
                componentKey(module.getGroup(), module.getModule(), module.getVersion()),
                resolvedArtifact.getVariantCapabilities());
        if (resolvedDependency != null) {
          synchronized (resolvedDependency) {
            resolvedDependency.addArtifact(resolvedArtifact);
          }
        }
      }
    }
  }

  private void collectPOMsForAllComponents(
      Project project,
      Map<String, List<GradleResolvedDependency>> dependenciesByComponent,
      List<GradleDependency> declaredDeps) {

    ArtifactResolutionQuery query = project.getDependencies().createArtifactResolutionQuery();
    query.withArtifacts(MavenModule.class, MavenPomArtifact.class);

    boolean hasItems = false;
    for (List<GradleResolvedDependency> variants : dependenciesByComponent.values()) {
      GradleResolvedDependency dependency = choosePomCarrier(variants);
      if (dependency == null) {
        continue;
      }
      boolean isDeclaredWithExplicitExtension =
          declaredDeps.stream()
              .anyMatch(
                  declaredDep ->
                      declaredDep.getGroup() != null
                          && declaredDep.getGroup().equals(dependency.getGroup())
                          && declaredDep.getArtifact() != null
                          && declaredDep.getArtifact().equals(dependency.getName())
                          && declaredDep.getVersion() != null
                          && declaredDep.getVersion().equals(dependency.getVersion())
                          && declaredDep.getExtension() != null);

      if (isDeclaredWithExplicitExtension) {
        continue;
      }

      query.forModule(dependency.getGroup(), dependency.getName(), dependency.getVersion());
      hasItems = true;
    }

    if (!hasItems) {
      return;
    }

    ArtifactResolutionResult result = query.execute();

    for (ComponentArtifactsResult component : result.getResolvedComponents()) {
      ComponentIdentifier id = component.getId();
      if (!(id instanceof ModuleComponentIdentifier)) {
        continue;
      }
      ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
      List<GradleResolvedDependency> dependencies =
          dependenciesByComponent.get(
              componentKey(module.getGroup(), module.getModule(), module.getVersion()));
      GradleResolvedDependency resolvedDependency = choosePomCarrier(dependencies);
      if (resolvedDependency == null) {
        continue;
      }

      for (ArtifactResult artifact : component.getArtifacts(MavenPomArtifact.class)) {
        if (artifact instanceof ResolvedArtifactResult) {
          ResolvedArtifactResult resolvedArtifactResult = (ResolvedArtifactResult) artifact;
          if (resolvedArtifactResult.getFile() != null) {
            GradleResolvedArtifact resolvedArtifact = new GradleResolvedArtifactImpl();
            resolvedArtifact.setFile(resolvedArtifactResult.getFile());
            String packaging = PomUtil.extractPackagingFromPom(resolvedArtifactResult.getFile());
            resolvedArtifact.setExtension(mapPackagingToExtension(packaging));
            resolvedArtifact.setVariantCapabilities(resolvedDependency.getVariantCapabilities());
            resolvedDependency.addArtifact(resolvedArtifact);
          }
        }
      }
    }
  }

  private String componentKey(ResolvedComponentResult component) {
    return componentKey(
        component.getModuleVersion().getGroup(),
        component.getModuleVersion().getName(),
        component.getModuleVersion().getVersion());
  }

  private String componentKey(String group, String module, String version) {
    return group + ":" + module + ":" + version;
  }

  private String componentKey(GradleResolvedDependency dependency) {
    return componentKey(dependency.getGroup(), dependency.getName(), dependency.getVersion());
  }

  // The display name distinguishes every selected variant of a component within a single
  // resolution, which is exactly the granularity the graph walk needs.
  private String variantKey(ResolvedComponentResult component, ResolvedVariantResult variant) {
    return componentKey(component) + "::" + variant.getDisplayName();
  }

  private List<String> capabilityKeys(ResolvedVariantResult variant) {
    return variant.getCapabilities().stream()
        .map(capability -> capability.getGroup() + ":" + capability.getName())
        .sorted()
        .collect(Collectors.toList());
  }

  private GradleResolvedDependency createResolvedVariant(
      ResolvedComponentResult component, ResolvedVariantResult variant) {
    GradleResolvedDependency info = new GradleResolvedDependencyImpl();
    info.setGroup(component.getModuleVersion().getGroup());
    info.setName(component.getModuleVersion().getName());

    // For timestamped snapshots, store the resolved timestamped version directly;
    // otherwise store the declared version.
    info.setVersion(
        GradleSnapshotUtil.timestampedSnapshotVersion(component)
            .orElseGet(() -> component.getModuleVersion().getVersion()));
    info.setVariantCapabilities(capabilityKeys(variant));
    return info;
  }

  private Map<String, List<GradleResolvedDependency>> indexByComponent(
      Iterable<GradleResolvedDependency> dependencies) {
    Map<String, List<GradleResolvedDependency>> grouped = new HashMap<>();
    for (GradleResolvedDependency dependency : dependencies) {
      grouped
          .computeIfAbsent(componentKey(dependency), unused -> new ArrayList<>())
          .add(dependency);
    }
    return grouped;
  }

  private GradleResolvedDependency findOwningDependency(
      Map<String, List<GradleResolvedDependency>> dependenciesByComponent,
      String componentKey,
      List<String> artifactCapabilities) {
    List<GradleResolvedDependency> dependencies = dependenciesByComponent.get(componentKey);
    if (dependencies == null || dependencies.isEmpty()) {
      return null;
    }

    // Capabilities are variant identity: api and runtime flavours of the same variant share
    // them, so artifacts from either artifact view land on the node the graph walk created.
    if (!artifactCapabilities.isEmpty()) {
      for (GradleResolvedDependency dependency : dependencies) {
        if (artifactCapabilities.equals(dependency.getVariantCapabilities())) {
          return dependency;
        }
      }
    }

    for (GradleResolvedDependency dependency : dependencies) {
      if (!dependency.isFeatureVariant()) {
        return dependency;
      }
    }

    return dependencies.get(0);
  }

  private GradleResolvedDependency choosePomCarrier(List<GradleResolvedDependency> dependencies) {
    if (dependencies == null || dependencies.isEmpty()) {
      return null;
    }

    for (GradleResolvedDependency dependency : dependencies) {
      boolean hasUnclassifiedBinary =
          dependency.getArtifacts().stream()
              .anyMatch(
                  artifact ->
                      artifact.getFile() != null
                          && !artifact.getFile().getName().endsWith(".pom")
                          && (artifact.getClassifier() == null
                              || artifact.getClassifier().isEmpty()));
      if (hasUnclassifiedBinary) {
        return dependency;
      }
    }

    for (GradleResolvedDependency dependency : dependencies) {
      if (!dependency.isFeatureVariant()) {
        return dependency;
      }
    }

    return dependencies.get(0);
  }

  private boolean isVerbose() {
    return System.getenv("RJE_VERBOSE") != null;
  }
}
