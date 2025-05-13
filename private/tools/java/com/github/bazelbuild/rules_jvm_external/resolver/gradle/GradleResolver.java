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

import com.facebook.ktfmt.format.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.*;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.events.LogEvent;
import com.github.bazelbuild.rules_jvm_external.resolver.events.PhaseEvent;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.Exclusion;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependency;
import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleResolvedDependencyInfo;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.devtools.build.runfiles.AutoBazelRepository;
import com.google.devtools.build.runfiles.Runfiles;
import com.google.gson.Gson;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@AutoBazelRepository
public class GradleResolver implements Resolver {

    private final EventListener eventListener;
    private final Netrc netrc;
    private final int maxThreads;

    public GradleResolver(Netrc netrc, int maxThreads, EventListener eventListener) {
        this.netrc = netrc;
        this.eventListener = eventListener;
        this.maxThreads = maxThreads;
    }

    private boolean isVerbose() {
        return System.getenv("RJE_VERBOSE").equals("true");
    }

    @Override
    public ResolutionResult resolve(ResolutionRequest request) {
        List<Repository> repositories = request.getRepositories().stream().map(this::createRepository).collect(Collectors.toList());
        List<GradleDependency> dependencies = request.getDependencies().stream().map(this::createDependency).collect(Collectors.toList());
        List<GradleDependency> boms = request.getBoms().stream().map(this::createDependency).collect(Collectors.toList());

        Path gradlePath = getGradleInstallationPath();
        try (GradleProject project = setupFakeGradleProject(
                repositories,
                dependencies,
                boms,
                request.getGlobalExclusions(),
                request.isUseUnsafeSharedCache()
        )) {
            project.setupProject();
            eventListener.onEvent(new PhaseEvent("Gathering dependencies"));
            project.connect(gradlePath);
            if(isVerbose()) {
                System.err.println("Resolving dependencies with gradle project: " + URI.create(project.getProjectDir().toString()).toString());
            }
            project.resolveDependencies(getGradleTaskProperties(repositories, project.getProjectDir()));
            Path dependenciesJsonFile = getDependenciesJsonFile(project.getProjectDir());
            if(!Files.exists(dependenciesJsonFile)) {
                throw new IllegalStateException("Failed resolving dependencies with gradle");
            }
            return parseDependencies(dependenciesJsonFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        if(isVerbose()) {
            properties.put("org.gradle.debug", "true");
        }
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
            // Read the resolve dependencies from the gradle task
            List<GradleResolvedDependencyInfo> dependencies = Arrays.asList(gson.fromJson(reader, GradleResolvedDependencyInfo[].class));
            // Find any conflicts
            conflicts = findConflicts(dependencies);

            // And build the dependency graph
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
            Runfiles.Preloaded runfiles = Runfiles.preload();
            // Check for Bazel 8 path
            String gradleBuildPath = runfiles.withSourceRepository(AutoBazelRepository_GradleResolver.NAME)
                    .rlocation("rules_jvm_external+/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/data/build.gradle.kts.hbs");
            if(gradleBuildPath == null) {
                // Check for Bazel 7 path
                gradleBuildPath = runfiles.withSourceRepository(AutoBazelRepository_GradleResolver.NAME)
                        .rlocation("rules_jvm_external~/private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/data/build.gradle.kts.hbs");
            }
            return Paths.get(gradleBuildPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getGradleInstallationPath() {
        try {
            Runfiles.Preloaded runfiles = Runfiles.preload();
            String gradleReadmePath = runfiles.withSourceRepository(AutoBazelRepository_GradleResolver.NAME)
                    .rlocation("gradle/gradle-bin/README");
            Path gradlePath = Paths.get(gradleReadmePath).getParent();
            if (!gradlePath.toFile().exists()) {
                throw new IllegalStateException("Gradle installation path does not exist: " + gradleReadmePath);
            }
            return gradlePath;
        } catch(IOException e) {
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

    private GradleProject setupFakeGradleProject(List<Repository> repositories, List<GradleDependency> dependencies, List<GradleDependency> boms, Set<Coordinates> globalExclusions, boolean useUnsafeCache) {
        try {
            Path fakeProjectDirectory = Files.createTempDirectory("rules_jvm_external");
            Path gradleBuildScriptTemplate = getGradleBuildScriptTemplate();
            List<Exclusion> exclusions = globalExclusions.stream().map(exclusion -> new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId())).collect(Collectors.toList());
            Path outputBuildScript = fakeProjectDirectory.resolve("build.gradle.kts");
            GradleBuildScriptTemplate.generateBuildGradleKts(
                    gradleBuildScriptTemplate,
                    outputBuildScript,
                    repositories,
                    boms,
                    dependencies,
                    exclusions
            );

            formatgradleBuildFile(outputBuildScript);

            if(isVerbose()) {
                eventListener.onEvent(new LogEvent("gradle", Files.readString(outputBuildScript), null));
            }

            Path gradleCacheDir = fakeProjectDirectory.resolve(".gradle");
            Files.createDirectories(gradleCacheDir);
            if (useUnsafeCache) {
                gradleCacheDir = Paths.get(System.getProperty("user.home"), ".gradle");
            }

            return new GradleProject(fakeProjectDirectory, gradleCacheDir, null, eventListener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void formatgradleBuildFile(Path filePath) {
        try {
            String content = Files.readString(filePath);

            String formattedContent = Formatter.format(content);
            Files.writeString(filePath, formattedContent);
        } catch (IOException | FormatterException e ) {
            throw new RuntimeException(e);
        }
    }
}