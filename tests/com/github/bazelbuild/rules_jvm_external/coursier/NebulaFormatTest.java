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

package com.github.bazelbuild.rules_jvm_external.coursier;

import static org.junit.Assert.assertEquals;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.DependencyInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.Test;

public class NebulaFormatTest {

  private final Set<URI> repos = Set.of(URI.create("http://localhost/m2/repository"));

  @Test
  public void shouldRenderAggregatingJarsAsJarWithNullShasum() {
    DependencyInfo aggregator =
        new DependencyInfo(
            new Coordinates("com.example:aggregator:1.0.0"),
            repos,
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Set.of(),
            new TreeMap<>());

    Map<String, Object> rendered =
        new NebulaFormat()
            .render(
                repos.stream().map(Object::toString).collect(Collectors.toSet()),
                Set.of(aggregator),
                Map.of());

    Map<?, ?> artifacts = (Map<?, ?>) rendered.get("artifacts");
    Map<?, ?> data = (Map<?, ?>) artifacts.get("com.example:aggregator");
    Map<?, ?> shasums = (Map<?, ?>) data.get("shasums");

    HashMap<Object, Object> expected = new HashMap<>();
    expected.put("jar", null);
    assertEquals(expected, shasums);
  }
}
