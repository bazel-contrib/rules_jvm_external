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

import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import org.gradle.api.Named;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.MultipleCandidatesDetails;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

/**
 * This is a plugin that we register to allow resolving gradle dependencies using Gradle's
 * resolution
 */
public class GradleDependencyModelPlugin implements Plugin<Project> {
  private static final Attribute<String> KOTLIN_PLATFORM_ATTRIBUTE =
      Attribute.of("org.jetbrains.kotlin.platform.type", String.class);
  private static final String ANDROID_JVM_PLATFORM = "androidJvm";
  private static final String JVM_PLATFORM = "jvm";
  private static final String VARIANT_PROPERTY_PREFIX = "rules_jvm_external.variant.";

  private final ToolingModelBuilderRegistry registry;

  @Inject
  public GradleDependencyModelPlugin(ToolingModelBuilderRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void apply(Project project) {
    registry.register(new GradleDependencyModelBuilder());
    configureKotlinPlatformSchema(project);
  }

  // Registered for every resolution, but inert for JVM consumers: both rules only fire
  // when the consumer declares the androidJvm platform.
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

  static Map<String, String> getVariantAttributes(Project project) {
    Map<String, String> variants = new TreeMap<>();
    project
        .getProperties()
        .forEach(
            (name, value) -> {
              if (name.startsWith(VARIANT_PROPERTY_PREFIX)) {
                variants.put(name.substring(VARIANT_PROPERTY_PREFIX.length()), value.toString());
              }
            });
    return variants;
  }

  static void configureVariantAttributes(
      Project project, Configuration configuration, Map<String, String> variants) {
    variants.forEach(
        (name, value) -> {
          Attribute<?> attribute =
              project.getDependencies().getAttributesSchema().getAttributes().stream()
                  .filter(candidate -> candidate.getName().equals(name))
                  .findFirst()
                  .orElse(Attribute.of(name, String.class));
          setAttribute(project, configuration, attribute, value);
        });
  }

  private static <T> void setAttribute(
      Project project, Configuration configuration, Attribute<T> attribute, String value) {
    Class<T> type = attribute.getType();
    Object typedValue;
    if (type == String.class) {
      typedValue = value;
    } else if (type == Integer.class) {
      typedValue = Integer.valueOf(value);
    } else if (type == Boolean.class && (value.equals("true") || value.equals("false"))) {
      typedValue = Boolean.valueOf(value);
    } else if (Named.class.isAssignableFrom(type)) {
      typedValue = project.getObjects().named(type.asSubclass(Named.class), value);
    } else {
      throw new IllegalArgumentException(
          "Unsupported value for variant attribute " + attribute + ": " + value);
    }
    configuration.getAttributes().attribute(attribute, type.cast(typedValue));
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
