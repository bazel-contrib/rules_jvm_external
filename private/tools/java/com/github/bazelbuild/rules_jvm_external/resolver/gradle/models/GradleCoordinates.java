package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

public interface GradleCoordinates {
    public String getGroupId();
    public String getArtifactId();
    public String getVersion();
    public String getClassifier();
    public String getExtension();
}
