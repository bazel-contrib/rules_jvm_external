package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import java.util.List;

public class Dependency {
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

    public Dependency(Scope scope, String group, String artifact, String version, List<Exclusion> exclusions) {
        this.scope = scope;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.exclusions = exclusions != null ? exclusions : List.of();
    }

    public Dependency(Scope scope, String group, String artifact, String version) {
        this(scope, group, artifact, version, List.of());
    }
}