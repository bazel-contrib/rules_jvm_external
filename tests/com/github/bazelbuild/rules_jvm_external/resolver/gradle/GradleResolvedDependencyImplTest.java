// Copyright 2026 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleResolvedDependencyImpl;
import java.util.List;
import org.junit.Test;

public class GradleResolvedDependencyImplTest {

  @Test
  public void variantWithNoCapabilitiesIsNotAFeatureVariant() {
    assertFalse(dependencyWithCapabilities(List.of()).isFeatureVariant());
  }

  @Test
  public void variantWithOnlyTheImplicitCapabilityIsNotAFeatureVariant() {
    assertFalse(dependencyWithCapabilities(List.of("com.example:sample")).isFeatureVariant());
  }

  @Test
  public void variantWithADifferentCapabilityIsAFeatureVariant() {
    assertTrue(
        dependencyWithCapabilities(List.of("com.example:sample-test-fixtures")).isFeatureVariant());
  }

  @Test
  public void variantDeclaringTheImplicitCapabilityAlongsideOthersIsNotAFeatureVariant() {
    assertFalse(
        dependencyWithCapabilities(
                List.of("com.example:sample", "com.example:sample-test-fixtures"))
            .isFeatureVariant());
  }

  private GradleResolvedDependencyImpl dependencyWithCapabilities(List<String> capabilities) {
    GradleResolvedDependencyImpl dependency = new GradleResolvedDependencyImpl();
    dependency.setGroup("com.example");
    dependency.setName("sample");
    dependency.setVersion("1.0");
    dependency.setVariantCapabilities(capabilities);
    return dependency;
  }
}
