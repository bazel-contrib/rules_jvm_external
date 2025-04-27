package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.util.List;

/**
 * Represents a Gradle dependency in build.gradle.kts
 */
public class GradleDependency {
    public enum Scope {
        IMPLEMENTATION,
        COMPILE_ONLY,
        RUNTIME_ONLY,
        TEST_IMPLEMENTATION,
    }

    public final Scope scope;
    public final String group;
    public final String artifact;
    public final String version;
    public final List<Exclusion> exclusions;

    public GradleDependency(Scope scope, String group, String artifact, String version, List<Exclusion> exclusions) {
        this.scope = scope;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.exclusions = exclusions != null ? exclusions : List.of();
    }

    public GradleDependency(Scope scope, String group, String artifact, String version) {
        this(scope, group, artifact, version, List.of());
    }
}