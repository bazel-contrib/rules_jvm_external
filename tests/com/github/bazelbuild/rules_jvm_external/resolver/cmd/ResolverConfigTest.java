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
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.Artifact;
import com.github.bazelbuild.rules_jvm_external.resolver.ui.NullListener;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class ResolverConfigTest {

  @Test
  public void argsFilePreservesBomForceVersion() throws Exception {
    Path argsFile = Files.createTempFile("resolver-config", ".json");
    Files.writeString(
        argsFile,
        "{"
            + "\"repositories\":[\"https://repo1.maven.org/maven2\"],"
            + "\"boms\":[{"
            + "\"group\":\"com.example\","
            + "\"artifact\":\"example-bom\","
            + "\"version\":\"1.0\","
            + "\"force_version\":true"
            + "}],"
            + "\"artifacts\":[]"
            + "}");

    ResolverConfig config =
        new ResolverConfig(new NullListener(), "--argsfile", argsFile.toString());

    Artifact bom = config.getResolutionRequest().getBoms().get(0);
    Coordinates coordinates = bom.getCoordinates();
    assertTrue(bom.isForceVersion());
    assertEquals("com.example", coordinates.getGroupId());
    assertEquals("example-bom", coordinates.getArtifactId());
    assertEquals("pom", coordinates.getExtension());
    assertEquals("1.0", coordinates.getVersion());
  }
}
