package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import com.github.bazelbuild.rules_jvm_external.Coordinates;

import java.util.List;

/**
 * Represents a single dependency resolved by gradle
 */
public class GradleResolvedDependencyInfo {
    private String group;
    private String name;
    private String version;
    private String requestedVersion;
    private boolean conflict;
    private List<GradleResolvedDependencyInfo> children;

    public GradleResolvedDependencyInfo() {
        // Default constructor needed for Gson
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRequestedVersion() {
        return requestedVersion;
    }

    public void setRequestedVersion(String requestedVersion) {
        this.requestedVersion = requestedVersion;
    }

    public boolean isConflict() {
        return conflict;
    }

    public void setConflict(boolean conflict) {
        this.conflict = conflict;
    }

    public List<GradleResolvedDependencyInfo> getChildren() {
        return children;
    }

    public void setChildren(List<GradleResolvedDependencyInfo> children) {
        this.children = children;
    }

    public Coordinates toCoordinates() {
            return new Coordinates(
                    this.getGroup(),
                    this.getName(),
                    "jar",
                    "",           // TODO: add classifiers
                    this.getVersion() != null ? this.getVersion() : ""
            );
    }
}