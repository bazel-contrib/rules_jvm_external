package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.util.List;

public interface GradleDependency {
    String getGroup();
    String getArtifact();
    String getVersion();
    Scope getScope(); // referring to inner enum
    String getClassifier();
    String getExtension();
    List<Exclusion> getExclusions();

    enum Scope {
        IMPLEMENTATION,
        COMPILE_ONLY,
        RUNTIME_ONLY,
        TEST_IMPLEMENTATION
    }
}
