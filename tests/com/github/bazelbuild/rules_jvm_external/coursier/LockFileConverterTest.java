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

package com.github.bazelbuild.rules_jvm_external.coursier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LockFileConverterTest {

  @Rule public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldPreserveIndexedClasses() throws IOException {
    Path input = writeDependencies(dependency("first", "com.example.First"));

    assertEquals(
        Set.of("com.example.First"),
        new LockFileConverter(Set.of(), input).getDependencies().iterator().next().getClasses());
  }

  @Test
  public void shouldAcceptDependenciesWithoutClasses() throws IOException {
    Path input = writeDependencies(Map.of("coord", "com.example:first:1.0"));

    assertTrue(
        new LockFileConverter(Set.of(), input)
            .getDependencies()
            .iterator()
            .next()
            .getClasses()
            .isEmpty());
  }

  @Test
  public void shouldWriteUniquePackagesAndSplitPackageClassesWhenIndexRequested()
      throws IOException {
    Path input =
        writeDependencies(
            dependency("first", "com.example.First", "com.unique.Only"),
            dependency("second", "com.example.Second"));
    Path lockFile = temp.newFile("lock.json").toPath();
    Path indexFile = temp.newFile("index.json").toPath();

    LockFileConverter.main(
        new String[] {
          "--json", input.toString(),
          "--output", lockFile.toString(),
          "--dependency-index-output", indexFile.toString()
        });

    JsonObject index = readJson(indexFile);
    assertEquals(1, index.get("version").getAsInt());
    assertEquals(
        new Gson().toJsonTree(Map.of("com.example:first", Set.of("com.unique"))),
        index.get("packages"));
    assertEquals(
        new Gson()
            .toJsonTree(
                Map.of(
                    "com.example:first", Map.of("com.example", Set.of("First")),
                    "com.example:second", Map.of("com.example", Set.of("Second")))),
        index.get("split_package_classes"));
    assertTrue(readJson(lockFile).getAsJsonObject("artifacts").has("com.example:first"));
  }

  @Test
  public void shouldKeepPackagesInLockFileWithoutIndex() throws IOException {
    Path input = writeDependencies(dependency("first", "com.example.First"));
    Path lockFile = temp.newFile("lock.json").toPath();

    LockFileConverter.main(
        new String[] {"--json", input.toString(), "--output", lockFile.toString()});

    assertEquals(
        new Gson().toJsonTree(Map.of("com.example:first", Set.of("com.example"))),
        readJson(lockFile).get("packages"));
  }

  private Map<String, Object> dependency(String artifact, String... classes) {
    return Map.of(
        "coord",
        "com.example:" + artifact + ":1.0",
        "file",
        "com/example/" + artifact + "/1.0/" + artifact + "-1.0.jar",
        "packages",
        Set.of("com.example"),
        "classes",
        List.of(classes));
  }

  @SafeVarargs
  private final Path writeDependencies(Map<String, Object>... dependencies) throws IOException {
    Path input = temp.newFile().toPath();
    Files.writeString(input, new Gson().toJson(Map.of("dependencies", List.of(dependencies))));
    return input;
  }

  private JsonObject readJson(Path path) throws IOException {
    return new Gson().fromJson(Files.readString(path), JsonObject.class);
  }
}
