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
    private final boolean kmpEnabled;
    private final Path gradleJavaHome;

    public GradleProject(Path projectDir, Path gradleCacheDir, boolean kmpEnabled, Path gradleJavaHome) {
        this.projectDir = projectDir;
        this.gradleCacheDir = gradleCacheDir;
        this.kmpEnabled = kmpEnabled;
        this.gradleJavaHome = gradleJavaHome;
    }

    public void setupProject(List<String> dependencies) throws IOException {
        Files.createDirectories(projectDir);

        Files.writeString(
                projectDir.resolve("settings.gradle"),
                "rootProject.name = 'rules_jvm_external'\n",
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        );

        StringBuilder buildScript = new StringBuilder();
        buildScript.append("plugins {\n")
                .append("    id 'java'\n")
                .append("}\n\n")
                .append("repositories {\n")
                .append("    mavenCentral()\n")
                .append("}\n\n")
                .append("dependencies {\n");

        for (String dep : dependencies) {
            buildScript.append("    implementation '").append(dep).append("'\n");
        }

        buildScript.append("}\n\n")
                .append("tasks.register(\"resolveOnly\") {\n")
                .append("    doLast {\n")
                .append("        configurations.compileClasspath.resolve()\n")
                .append("    }\n")
                .append("}\n");

        Files.writeString(
                projectDir.resolve("build.gradle"),
                buildScript.toString(),
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
}
