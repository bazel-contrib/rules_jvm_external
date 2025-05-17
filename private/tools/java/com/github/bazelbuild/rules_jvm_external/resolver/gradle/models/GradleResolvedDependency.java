package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import com.github.bazelbuild.rules_jvm_external.Coordinates;

import java.util.List;

public interface GradleResolvedDependency {
    public String getGroup();

    public void setGroup(String group);

    public String getName();

    public void setName(String name);

    public String getVersion();

    public void setVersion(String version);
    public String getRequestedVersion();

    public void setRequestedVersion(String requestedVersion);

    public boolean isConflict();

    public void setConflict(boolean conflict);

    public List<GradleResolvedDependency> getChildren();

    public void setChildren(List<GradleResolvedDependency> children);

    public Coordinates toCoordinates();

    public Coordinates toConflictVersionCoordinates();

    public boolean isFromBom();
    public void setFromBom(boolean fromBom);
}
