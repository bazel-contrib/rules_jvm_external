package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

public class Exclusion {
    public final String group;
    public final String module;

    public Exclusion(String group, String module) {
        this.group = group;
        this.module = module;
    }
}
