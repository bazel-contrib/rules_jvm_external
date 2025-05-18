package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.io.Serializable;

public class GradleCoordinatesImpl implements Serializable, GradleCoordinates {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String extension;

    public GradleCoordinatesImpl(String groupId, String artifactId, String version, String classifier, String extension) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
    }
    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getExtension() {
        return extension;
    }
}
