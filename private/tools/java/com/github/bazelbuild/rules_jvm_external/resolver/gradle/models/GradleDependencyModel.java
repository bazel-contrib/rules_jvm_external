package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Gradle dependency information collected from our GradleDependencyModelPlugin
 * It has the following information:
 * declaredDependencies - these are dependencies given as inputs to gradle and the versions declared by the user
 * resolvedDependencies - these are the dependencies and their versions after resolution
 * boms - these are BOMs declared and resolved.
 */
public interface GradleDependencyModel extends Serializable {
    Map<String, List<GradleDependency>> getDeclaredDependencies();
    Map<String, List<GradleResolvedDependency>> getResolvedDependencies();
    Map<String, List<GradleDependency>> getBoms();
}
