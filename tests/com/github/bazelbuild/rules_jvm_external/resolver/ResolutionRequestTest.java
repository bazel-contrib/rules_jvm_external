// Copyright 2024 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.resolver;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ResolutionRequestTest {
  @Test
  public void variantsAreEmptyByDefault() {
    assertEquals(Map.of(), new ResolutionRequest().getVariants());
  }

  @Test
  public void replacingDependenciesPreservesVariantAttributes() {
    Map<String, String> variants = new HashMap<>(Map.of("org.gradle.jvm.environment", "android"));
    ResolutionRequest request = new ResolutionRequest().variants(variants);
    variants.clear();
    ResolutionRequest replaced = request.replaceDependencies(List.of());
    request.variants(Map.of());

    assertEquals(Map.of("org.gradle.jvm.environment", "android"), replaced.getVariants());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void variantAttributesCannotBeMutatedThroughTheGetter() {
    new ResolutionRequest().variants(Map.of("custom.attribute", "value")).getVariants().clear();
  }
}
