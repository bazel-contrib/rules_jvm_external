package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

/**
 * This is a plugin that we register to allow resolving gradle dependencies using Gradle's resolution
 */
public class GradleDependencyModelPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions()
                .getByType(ToolingModelBuilderRegistry.class)
                .register(new GradleDependencyModelBuilder());
    }
}
