package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gradle dependency information collected from our GradleDependencyModelPlugin
 * It has the following information:
 * declaredDependencies - these are dependencies given as inputs to gradle and the versions declared by the user
 * resolvedDependencies - these are the dependencies and their versions after resolution
 * boms - these are BOMs declared and resolved.
 */
public class GradleDependencyModel implements Serializable {
    public final Map<String, List<GradleDependency>> declaredDependencies = new LinkedHashMap<>();
    public final Map<String, List<GradleResolvedDependency>> resolvedDependencies = new LinkedHashMap<>();
    public final Map<String, List<GradleDependency>> boms = new LinkedHashMap<>();
}
