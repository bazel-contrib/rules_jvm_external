package com.github.bazelbuild.rules_jvm_external.jar;

import java.util.SortedSet;

public class PerJarIndexResults {
    private final SortedSet<String> packages;

    public PerJarIndexResults(SortedSet<String> packages) {
        this.packages = packages;
    }

    public SortedSet<String> getPackages() {
        return this.packages;
    }
}
