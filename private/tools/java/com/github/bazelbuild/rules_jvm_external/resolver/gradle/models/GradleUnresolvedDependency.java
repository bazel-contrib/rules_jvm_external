package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

public interface GradleUnresolvedDependency {
    public enum FailureReason {
        NOT_FOUND,
        INTERNAL
    }

    public String getGroup();


    public String getName();


    public String getVersion();

    public FailureReason getFailureReason();

    public String getFailureDetails();
}
