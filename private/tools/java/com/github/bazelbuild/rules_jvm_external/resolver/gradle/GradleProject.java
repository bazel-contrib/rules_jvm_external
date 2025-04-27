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

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GradleProject implements AutoCloseable  {

    private final Path projectDir;
    private final Path gradleCacheDir;
    private ProjectConnection connection;
    private final Path gradleJavaHome;

    public GradleProject(Path projectDir, Path gradleCacheDir, Path gradleJavaHome) {
        this.projectDir = projectDir;
        this.gradleCacheDir = gradleCacheDir;
        this.gradleJavaHome = gradleJavaHome;
    }

    public void setupProject() throws IOException {
        Files.createDirectories(projectDir);

        Files.writeString(
                projectDir.resolve("settings.gradle"),
                "rootProject.name = 'rules_jvm_external'\n",
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public void connect() throws IOException {
        System.setProperty("gradle.user.home", gradleCacheDir.toAbsolutePath().toString());
        System.setProperty("org.gradle.java.home", gradleJavaHome.toAbsolutePath().toString());
        connection = GradleConnector.newConnector()
                .forProjectDirectory(projectDir.toFile())
                .connect();
    }

    /**
     * Triggers dependency resolution by running the custom task to resolve gradle dependencies
     */
    public void resolveDependencies(Map<String, String> gradleProperties) {
        if (connection == null) {
            throw new IllegalStateException("Gradle connection not established. Call connect() first.");
        }

        // This allows us to pass sensitive information like repository credentials without
        // leaking it into the actual build file
        List<String> arguments = gradleProperties.entrySet().stream()
                .map(entry -> "-P" + entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.toList());

        connection.newBuild()
                .forTasks("dumpResolvedDependencies")
                .setStandardOutput(System.out)
                .setStandardError(System.err)
                .withArguments(arguments)
                .run();
    }


    @Override
    public void close() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    public Path getProjectDir() {
        return projectDir;
    }
}
