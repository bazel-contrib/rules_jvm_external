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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.*;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependency;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleResolvedDependencyInfo;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.devtools.build.runfiles.Runfiles;
import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GradleResolver implements Resolver {

    private final EventListener eventListener;
    private final Netrc netrc;
    private final int maxThreads;

    public GradleResolver(Netrc netrc, int maxThreads, EventListener eventListener) {
        this.netrc = netrc;
        this.eventListener = eventListener;
        this.maxThreads = maxThreads;
    }

    @Override
    public ResolutionResult resolve(ResolutionRequest request) {
        List<Repository> repositories = request.getRepositories().stream().map(this::createRepository).collect(Collectors.toList());
        List<GradleDependency> dependencies = request.getDependencies().stream().map(this::createDependency).collect(Collectors.toList());
        List<GradleDependency> boms = request.getBoms().stream().map(this::createDependency).collect(Collectors.toList());

        try (GradleProject project = setupFakeGradleProject(
                repositories,
                boms,
                dependencies,
                request.isUseUnsafeSharedCache()
        )) {
            project.setupProject();
            project.resolveDependencies(getGradleTaskProperties(repositories, project.getProjectDir()));
            Path dependenciesJsonFile = getDependenciesJsonFile(project.getProjectDir());
            if(!Files.exists(dependenciesJsonFile)) {
                throw new IllegalStateException("Failed resolving dependencies with gradle");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Map<String, String> getGradleTaskProperties(List<Repository> repositories, Path projectDir) {
        Map<String, String> properties = new HashMap<>();
        for (Repository repository : repositories) {
            if(repository.requiresAuth) {
                properties.put(repository.usernameProperty, repository.getUsername());
                properties.put(repository.passwordProperty, repository.getPassword());
            }
        }

        properties.put("outputJsonFile", getDependenciesJsonFile(projectDir).toString());
        properties.put("org.gradle.workers.max", String.valueOf(maxThreads));
        properties.put("org.gradle.parallel", "true");
        return properties;
    }

    private Path getDependenciesJsonFile(Path projectDir) {
        return projectDir.resolve("resolved-dependencies.json");
    }

    private ResolutionResult parseDependencies(Path dependenciesJsonFile) throws IOException {
        Gson gson = new Gson();

        MutableGraph<Coordinates> graph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .build();

        Set<Conflict> conflicts = null;
        try (FileReader reader = new FileReader(dependenciesJsonFile.toString())) {
            List<GradleResolvedDependencyInfo> dependencies = Arrays.asList(gson.fromJson(reader, GradleResolvedDependencyInfo[].class));
            conflicts = findConflicts(dependencies);
            for(GradleResolvedDependencyInfo dependency : dependencies) {
                addDependency(graph, dependency.toCoordinates(), dependency);
            }
        }
        return new ResolutionResult(graph, conflicts);
    }

    private void addDependency(MutableGraph<Coordinates> graph, Coordinates parent, GradleResolvedDependencyInfo parentInfo) {
        graph.addNode(parent);

        if (parentInfo.getChildren() != null) {
            for (GradleResolvedDependencyInfo childInfo : parentInfo.getChildren()) {
                Coordinates child = childInfo.toCoordinates();
                graph.addNode(child);
                graph.putEdge(parent, child);
                addDependency(graph, child, childInfo); // recursive traverse the graph
            }
        }
    }

    private Repository createRepository(URI uri) {
        Netrc.Credential credential = netrc.getCredential(uri.getHost());
        if(credential == null) {
           return new Repository(uri);
        }

        return new Repository(uri, true, credential.login(), credential.password());
    }

    private GradleDependency createDependency(Artifact artifact) {
        Coordinates coordinates = artifact.getCoordinates();
        return new GradleDependency(GradleDependency.Scope.IMPLEMENTATION, coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion());
    }


    private Path getGradleBuildScriptTemplate() {
        try {
            String gradleBuildPath = Runfiles.preload().withSourceRepository("rules_jvm_external")
                    .rlocation("_main/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/data/build.gradle.kts.hbs");
            return Paths.get(gradleBuildPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Conflict> findConflicts(List<GradleResolvedDependencyInfo> dependencyInfos) {
        Set<Conflict> conflicts = new HashSet<>();
        for(GradleResolvedDependencyInfo dependencyInfo : dependencyInfos) {
            walkForConflicts(dependencyInfo, conflicts);
        }

        return conflicts;
    }

    private void walkForConflicts(GradleResolvedDependencyInfo node, Set<Conflict> conflicts) {
        if (node.getChildren() == null) {
            return;
        }
        Coordinates resolved = node.toCoordinates();

        for (GradleResolvedDependencyInfo child : node.getChildren()) {
            Coordinates requested = child.toCoordinates();

            if (resolved.getGroupId().equals(requested.getGroupId()) &&
                    resolved.getArtifactId().equals(requested.getArtifactId()) &&
                    resolved.getClassifier().equals(requested.getClassifier()) &&
                    resolved.getExtension().equals(requested.getExtension()) &&
                    !resolved.getVersion().equals(requested.getVersion())) {

                conflicts.add(new Conflict(resolved, requested));
            }

            walkForConflicts(child, conflicts); // recursive
        }
    }

    private GradleProject setupFakeGradleProject(List<Repository> repositories, List<GradleDependency> dependencies, List<GradleDependency> boms, boolean useUnsafeCache) {
        try {
            Path fakeProjectDirectory = Files.createTempDirectory("rules_jvm_external");
            Path gradleBuildScriptTemplate = getGradleBuildScriptTemplate();

            Path outputBuildScript = fakeProjectDirectory.resolve("build.gradle.kts");
            GradleBuildScriptTemplate.generateBuildGradleKts(
                    gradleBuildScriptTemplate,
                    outputBuildScript,
                    repositories,
                    boms,
                    dependencies
            );

            Path gradleCacheDir = fakeProjectDirectory.resolve(".gradle");
            Files.createDirectories(gradleCacheDir);
            if (useUnsafeCache) {
                gradleCacheDir = Paths.get(System.getProperty("user.home"), ".gradle");
            }

            return new GradleProject(fakeProjectDirectory, gradleCacheDir, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}