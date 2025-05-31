package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.io.Serializable;

public class GradleUnresolvedDependencyImpl implements GradleUnresolvedDependency, Serializable {
    private final String group;
    private final String name;
    private final String version;
    private final FailureReason failureReason;
    private final String failureDetails;
    public GradleUnresolvedDependencyImpl(String group, String name, String version, FailureReason failureReason, String failureDetails) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.failureReason = failureReason;
        this.failureDetails = failureDetails;
    }
    @Override
    public String getGroup() {
        return this.group;
    }


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public FailureReason getFailureReason() {
        return this.failureReason;
    }

    @Override
    public String getFailureDetails() { return this.failureDetails; }
}
