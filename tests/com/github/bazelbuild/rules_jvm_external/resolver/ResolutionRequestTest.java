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

package com.github.bazelbuild.rules_jvm_external.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class ResolutionRequestTest {

  @Test
  public void shouldDeleteTemporaryUserHomeOnCleanup() throws IOException {
    ResolutionRequest request = new ResolutionRequest();

    Path userHome = request.getUserHome();
    Path marker = request.getLocalCache("maven").resolve("cleanup-marker");
    Files.writeString(marker, "cleanup-me");

    assertTrue(Files.exists(userHome));
    assertTrue(Files.exists(marker));

    request.close();

    assertFalse(Files.exists(userHome));
  }

  @Test
  public void shouldNotDeleteSharedUserHomeWhenUsingUnsafeCache() {
    ResolutionRequest request = new ResolutionRequest();
    request.useUnsafeSharedCache(true);

    Path userHome = request.getUserHome();
    request.close();

    assertEquals(Paths.get(System.getProperty("user.home")), userHome);
    assertTrue(Files.exists(userHome));
  }
}
