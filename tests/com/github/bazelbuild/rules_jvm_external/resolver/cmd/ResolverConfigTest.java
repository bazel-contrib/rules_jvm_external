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

package com.github.bazelbuild.rules_jvm_external.resolver.cmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ResolverConfigTest {
  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void shouldReadExplicitInputMetadataWithoutHashingIt() throws Exception {
    Path input = temporary.newFile().toPath();
    Files.writeString(
        input, "{\"artifacts\":[],\"boms\":[],\"settings\":{\"fetch_sources\":true}}");
    ResolverConfig config =
        new ResolverConfig(event -> {}, "--input-metadata-path", input.toString());
    assertEquals(Map.of("fetch_sources", true), config.getInputMetadata().get("settings"));
    assertNull(config.getInputHash());
  }

  @Test
  public void shouldKeepLegacyConfigurationWithoutInputMetadata() throws Exception {
    assertNull(new ResolverConfig(event -> {}).getInputMetadata());
  }
}
