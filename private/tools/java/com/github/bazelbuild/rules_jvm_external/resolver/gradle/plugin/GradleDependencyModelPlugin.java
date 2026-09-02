// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.MultipleCandidatesDetails;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

/**
 * This is a plugin that we register to allow resolving gradle dependencies using Gradle's
 * resolution
 */
public class GradleDependencyModelPlugin implements Plugin<Project> {
  private static final Attribute<TargetJvmEnvironment> JVM_ENVIRONMENT_ATTRIBUTE =
      TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE;
  private static final Attribute<String> KOTLIN_PLATFORM_ATTRIBUTE =
      Attribute.of("org.jetbrains.kotlin.platform.type", String.class);
  private static final String ANDROID_JVM_PLATFORM = "androidJvm";
  private static final String JVM_PLATFORM = "jvm";
  private static final String RESOLVE_FOR_PROPERTY = "rules_jvm_external.resolve_for";

  private final ToolingModelBuilderRegistry registry;

  @Inject
  public GradleDependencyModelPlugin(ToolingModelBuilderRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void apply(Project project) {
    registry.register(new GradleDependencyModelBuilder());
    configureKotlinPlatformSchema(project);
    if ("android".equals(project.findProperty(RESOLVE_FOR_PROPERTY))) {
      project.getPlugins().withId("java", plugin -> configureAndroidConsumer(project));
    }
  }

  private static void configureKotlinPlatformSchema(Project project) {
    project
        .getDependencies()
        .getAttributesSchema()
        .attribute(
            KOTLIN_PLATFORM_ATTRIBUTE,
            strategy -> {
              strategy.getCompatibilityRules().add(KotlinPlatformCompatibilityRule.class);
              strategy.getDisambiguationRules().add(KotlinPlatformDisambiguationRule.class);
            });
  }

  private static void configureAndroidConsumer(Project project) {
    project
        .getConfigurations()
        .getByName("runtimeClasspath")
        .getAttributes()
        .attribute(
            JVM_ENVIRONMENT_ATTRIBUTE,
            project.getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.ANDROID))
        .attribute(KOTLIN_PLATFORM_ATTRIBUTE, ANDROID_JVM_PLATFORM);
  }

  public static class KotlinPlatformCompatibilityRule
      implements AttributeCompatibilityRule<String> {
    @Override
    public void execute(CompatibilityCheckDetails<String> details) {
      if (ANDROID_JVM_PLATFORM.equals(details.getConsumerValue())
          && JVM_PLATFORM.equals(details.getProducerValue())) {
        details.compatible();
      }
    }
  }

  public static class KotlinPlatformDisambiguationRule
      implements AttributeDisambiguationRule<String> {
    @Override
    public void execute(MultipleCandidatesDetails<String> details) {
      if (ANDROID_JVM_PLATFORM.equals(details.getConsumerValue())
          && details.getCandidateValues().contains(ANDROID_JVM_PLATFORM)
          && details.getCandidateValues().contains(JVM_PLATFORM)) {
        details.closestMatch(ANDROID_JVM_PLATFORM);
      }
    }
  }
}
