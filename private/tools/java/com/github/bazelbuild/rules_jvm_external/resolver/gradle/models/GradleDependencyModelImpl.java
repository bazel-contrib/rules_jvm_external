package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GradleDependencyModelImpl implements GradleDependencyModel {
    private final Map<String, List<GradleDependency>> declared = new LinkedHashMap<>();
    private final Map<String, List<GradleResolvedDependency>> resolved = new LinkedHashMap<>();
    private final Map<String, List<GradleDependency>> boms = new LinkedHashMap<>();

    @Override public Map<String, List<GradleDependency>> getDeclaredDependencies() { return declared; }
    @Override public Map<String, List<GradleResolvedDependency>> getResolvedDependencies() { return resolved; }
    @Override public Map<String, List<GradleDependency>> getBoms() { return boms; }
}
