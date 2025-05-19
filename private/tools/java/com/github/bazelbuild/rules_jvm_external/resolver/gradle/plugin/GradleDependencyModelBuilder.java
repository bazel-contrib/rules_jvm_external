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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.*;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.*;
import org.gradle.api.attributes.DocsType;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.Category;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class GradleDependencyModelBuilder implements ToolingModelBuilder {
    private final HashMap<String, GradleDependency.Scope> configurationScopes = new LinkedHashMap<>(Map.of(
            "compileClasspath", GradleDependency.Scope.IMPLEMENTATION,
            "runtimeClasspath", GradleDependency.Scope.RUNTIME_ONLY,
            "testCompileClasspath", GradleDependency.Scope.TEST_IMPLEMENTATION,
            "testRuntimeClasspath", GradleDependency.Scope.TEST_IMPLEMENTATION
            ));

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(GradleDependencyModel.class.getName());
    }

    private List<GradleDependency> collectDeclaredDependencies(Configuration cfg) {
        List<GradleDependency> declaredDeps = new ArrayList<>();
        for (Dependency dep : cfg.getAllDependencies()) {
            if (!(dep instanceof ModuleDependency)) continue;
            ModuleDependency modDep = (ModuleDependency) dep;

            GradleDependency.Scope scope = configurationScopes.get(cfg.getName());
            List<Exclusion> exclusions = modDep.getExcludeRules().stream()
                    .map(rule -> new ExclusionImpl(rule.getGroup(), rule.getModule()))
                    .collect(Collectors.toList());

            String classifier = null;
            String extension = null;
            if(dep instanceof ExternalModuleDependency) {
                ExternalModuleDependency externalDep = (ExternalModuleDependency) dep;
                Set<DependencyArtifact> artifacts = externalDep.getArtifacts();
                if(artifacts.iterator().hasNext()) {
                    System.out.println("found classifier");
                    classifier = artifacts.iterator().next().getClassifier();
                    extension = artifacts.iterator().next().getExtension();
                }
            }
            declaredDeps.add(new GradleDependencyImpl(
                    scope,
                    modDep.getGroup(),
                    modDep.getName(),
                    modDep.getVersion(),
                    exclusions,
                    classifier,
                    extension
            ));
        }
        return declaredDeps;
    }

    private List<GradleResolvedDependency> collectResolvedDependencies(Configuration cfg, Map<ComponentIdentifier, List<GradleResolvedArtifact>> componentResolvedArtifacts) {
        List<GradleResolvedDependency> resolvedRoots = new ArrayList<>();
        ResolutionResult result = cfg.getIncoming().getResolutionResult();
        ResolvedComponentResult root = result.getRoot();

        for (DependencyResult dep : root.getDependencies()) {
            if (!(dep instanceof ResolvedDependencyResult)) continue;

            ResolvedDependencyResult rdep = (ResolvedDependencyResult) dep;
            ResolvedComponentResult selected = rdep.getSelected();
            GradleResolvedDependencyImpl info = walkResolvedComponent(selected, componentResolvedArtifacts);
            if (rdep.getRequested() instanceof ModuleComponentSelector) {
                String requested = ((ModuleComponentSelector) rdep.getRequested()).getVersion();
                info.setRequestedVersion(requested);
                info.setConflict(!Objects.equals(info.getVersion(), requested));
            }

            resolvedRoots.add(info);
        }
        return resolvedRoots;
    }

    private List<GradleDependency> collectBoms(Configuration cfg) {
        List<GradleDependency> boms = new ArrayList<>();

        for (DependencyConstraint constraint : cfg.getAllDependencyConstraints()) {
            if (constraint.getGroup() == null || constraint.getVersion() == null) continue;

            GradleDependency.Scope scope = configurationScopes.get(cfg.getName());
            boms.add(new GradleDependencyImpl(
                    scope,
                    constraint.getGroup(),
                    constraint.getName(),
                    constraint.getVersion(),
                    List.of(),
                    null,
                    null
            ));
        }
        return boms;
    }


    @Override
    public Object buildAll(String modelName, Project project) {
        GradleDependencyModelImpl gradleDependencyModel = new GradleDependencyModelImpl();
        Configuration cfg = project.getConfigurations().getByName("compileClasspath");

        // Collect declared dependencies and their versions
        List<GradleDependency> declaredDeps = collectDeclaredDependencies(cfg);
        gradleDependencyModel.getDeclaredDependencies().put(cfg.getName(), declaredDeps);

        // Use the ArtifactView API to get all the resolved artifacts (jars, javadoc, sources)
        // and map it to the component identifier of each dependency component as Artifacts only have information
        // about the file and variant
        Map<ComponentIdentifier, List<GradleResolvedArtifact>> componentArtifacts = collectAllResolvedArtifacts(project, cfg);

        // Now get the dependency graph obtained in resolution and attach the artifacts on the nodes in the graph
        List<GradleResolvedDependency> resolvedRoots = collectResolvedDependencies(cfg, componentArtifacts);
        gradleDependencyModel.getResolvedDependencies().put(cfg.getName(), resolvedRoots);

        // Collect boms from the gradle resolution
        List<GradleDependency> boms = collectBoms(cfg);
        gradleDependencyModel.getBoms().put(cfg.getName(), boms);

        return gradleDependencyModel;
    }

    private GradleResolvedDependencyImpl walkResolvedComponent(ResolvedComponentResult component, Map<ComponentIdentifier, List<GradleResolvedArtifact>> componentResolvedArtifacts) {
        GradleResolvedDependencyImpl info = new GradleResolvedDependencyImpl();
        info.setGroup(component.getModuleVersion().getGroup());
        info.setName(component.getModuleVersion().getName());
        info.setVersion(component.getModuleVersion().getVersion());
        info.setRequestedVersion(info.getVersion()); // default to the requested version
        info.setConflict(false); // will be set later if there's a conflict

        List<GradleResolvedDependency> children = new ArrayList<>();

        for (DependencyResult dep : component.getDependencies()) {
            if (!(dep instanceof ResolvedDependencyResult)) continue;

            ResolvedDependencyResult resolvedDep = (ResolvedDependencyResult) dep;
            ResolvedComponentResult selected = resolvedDep.getSelected();

            GradleResolvedDependency child = walkResolvedComponent(selected, componentResolvedArtifacts);

            if (resolvedDep.getRequested() instanceof ModuleComponentSelector) {
                String requestedVersion = ((ModuleComponentSelector)
                        resolvedDep.getRequested()).getVersion();
                child.setRequestedVersion(requestedVersion);
                child.setConflict(!Objects.equals(child.getVersion(), requestedVersion));
            }

            children.add(child);
        }

        ComponentSelectionReason reason = component.getSelectionReason();
        List<String> bomDescriptions = reason.getDescriptions().stream()
                        .map(descriptor -> descriptor.getDescription().toLowerCase())
                                .filter(description -> description.contains("platform") || description.contains("constraint"))
                                        .collect(Collectors.toList());
        info.setFromBom(!bomDescriptions.isEmpty());

        info.setChildren(children);

        // This is where we attach the artifacts to the resolved dependency (there can be many - like jars, javadoc, sources, poms)
        for(Map.Entry<ComponentIdentifier, List<GradleResolvedArtifact>> entry : componentResolvedArtifacts.entrySet()) {
            ComponentIdentifier identifier = entry.getKey();
            List<GradleResolvedArtifact> resolvedArtifacts = entry.getValue();
            for(GradleResolvedArtifact artifact : resolvedArtifacts) {
                if(identifier.getDisplayName().equals(identifier.getDisplayName())) {
                    GradleResolvedArtifact gradleResolvedArtifact = new GradleResolvedArtifactImpl();
                    gradleResolvedArtifact.setClassifier(artifact.getClassifier());
                    gradleResolvedArtifact.setExtension(artifact.getExtension());
                    gradleResolvedArtifact.setFile(artifact.getFile());
                    info.addArtifact(gradleResolvedArtifact);
                }
            }
        }
        return info;
    }

    // The ArtifactView api doesn't provide a way to obtain the classifier (the legacy API does but it doesn't provide a way to obtain additional artifacts
    // like javadoc/sources from what I can tell
    // so we interpolate the classifier based on the artifact file
    private String extractClassifier(File file, ComponentIdentifier id) {
        if (!(id instanceof ModuleComponentIdentifier)) return null;

        ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
        String version = module.getVersion();
        String artifact = module.getModule();

        String name = file.getName(); // e.g. lib-1.0-sources.jar, lib-1.0.pom

        // Strip extension
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0) return null;
        String nameWithoutExt = name.substring(0, dotIndex);

        String expectedBase = artifact + "-" + version;

        if (!nameWithoutExt.startsWith(expectedBase)) return null;

        String suffix = nameWithoutExt.substring(expectedBase.length());
        return (suffix.startsWith("-")) ? suffix.substring(1) : null;
    }


    private String extractExtension(File file) {
        String name = file.getName();
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(i + 1) : null;
    }

    private Map<ComponentIdentifier, List<GradleResolvedArtifact>> collectAllResolvedArtifacts(Project project, Configuration cfg) {
        Map<ComponentIdentifier, List<GradleResolvedArtifact>> componentResolvedArtifacts = new HashMap<>();
        ArtifactView sourcesView = cfg.getIncoming().artifactView(spec -> {
            spec.withVariantReselection();
            spec.setLenient(true);
            spec.attributes(attrs -> {
                attrs.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.getObjects().named(DocsType.class, DocsType.SOURCES));
            });
        });

        collectArtifactsFromArtifactView(sourcesView, componentResolvedArtifacts);

        ArtifactView javadocView = cfg.getIncoming().artifactView(spec -> {
            spec.setLenient(true);
            spec.attributes(attrs -> {
                spec.withVariantReselection();
                attrs.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.getObjects().named(DocsType.class, DocsType.JAVADOC));
            });
        });

        collectArtifactsFromArtifactView(javadocView, componentResolvedArtifacts);

        ArtifactView jarView = cfg.getIncoming().artifactView(spec -> {
            spec.attributes(attrs -> {
                spec.setLenient(true);
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_API));
            });
        });

        collectArtifactsFromArtifactView(jarView, componentResolvedArtifacts);
        collectPOMsForAllComponents(project, componentResolvedArtifacts);
        return componentResolvedArtifacts;
    }

    private void collectArtifactsFromArtifactView(ArtifactView artifactView, Map<ComponentIdentifier, List<GradleResolvedArtifact>> componentResolvedArtifacts) {
        Set<ResolvedArtifactResult> resolvedArtifactResults = artifactView.getArtifacts().getArtifacts();
        for(ResolvedArtifactResult artifact : resolvedArtifactResults) {
            ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
            GradleResolvedArtifact resolvedArtifact = new GradleResolvedArtifactImpl();
            resolvedArtifact.setFile(artifact.getFile());
            resolvedArtifact.setClassifier(extractClassifier(artifact.getFile(), identifier));
            resolvedArtifact.setExtension(extractExtension(artifact.getFile()));
            if(!componentResolvedArtifacts.containsKey(identifier)) {
                List<GradleResolvedArtifact> resolvedArtifacts = new ArrayList<>();
                resolvedArtifacts.add(resolvedArtifact);
                componentResolvedArtifacts.put(identifier, resolvedArtifacts);
            } else {
                componentResolvedArtifacts.get(identifier).add(resolvedArtifact);
            }
        }
    }

    private void collectPOMsForAllComponents(Project project, Map<ComponentIdentifier, List<GradleResolvedArtifact>> componentResolvedArtifacts) {
        for(ComponentIdentifier identifier : componentResolvedArtifacts.keySet()) {
            if (!(identifier instanceof ModuleComponentIdentifier)) continue;

            System.out.println("Collecting POM for " + identifier);
            ModuleComponentIdentifier m = (ModuleComponentIdentifier) identifier;
            String depNotation = m.getGroup() + ":" + m.getModule() + ":" + m.getVersion() + "@pom";
            System.out.println("Collecting POM for " + depNotation);
            Dependency dep = project.getDependencies().create(depNotation);

            Configuration pomCfg = project.getConfigurations().detachedConfiguration(dep);

            ArtifactView view = pomCfg.getIncoming().artifactView(spec -> {
                spec.setLenient(true); // avoid failure if a POM is missing
            });

            for (ResolvedArtifactResult artifact : view.getArtifacts().getArtifacts()) {
                GradleResolvedArtifact resolvedArtifact = new GradleResolvedArtifactImpl();
                resolvedArtifact.setFile(artifact.getFile());
                resolvedArtifact.setClassifier(extractClassifier(artifact.getFile(), identifier));
                if(componentResolvedArtifacts.containsKey(identifier)) {
                    componentResolvedArtifacts.get(identifier).add(resolvedArtifact);
                } else {
                    List<GradleResolvedArtifact> resolvedArtifacts = new ArrayList<>();
                    resolvedArtifacts.add(resolvedArtifact);
                    componentResolvedArtifacts.put(identifier, resolvedArtifacts);
                }
            }
        }
    }
}
