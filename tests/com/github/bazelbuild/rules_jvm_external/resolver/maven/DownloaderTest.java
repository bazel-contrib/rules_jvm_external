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

package com.github.bazelbuild.rules_jvm_external.resolver.maven;

import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.MavenRepo;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.DownloadResult;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.Downloader;
import com.github.bazelbuild.rules_jvm_external.resolver.ui.NullListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class DownloaderTest {
  @Test
  public void downloaderTreatsAggregatingCoordinateAsNoBinary() throws IOException {
    // A coordinate the resolver marked as an aggregator (no binary of its own) downloads to a
    // binary-less result, even though its POM does not declare `<packaging>pom</packaging>`.
    Coordinates coords = new Coordinates("com.example:sample:1.0");

    // Write only the POM (no JAR) for the coordinate.
    Path repo = MavenRepo.create().add(coords.setExtension("pom")).getPath();
    Path localRepo = Files.createTempDirectory("local");

    DownloadResult downloadResult =
        new Downloader(
                Netrc.fromUserHome(),
                localRepo,
                Set.of(repo.toUri()),
                new NullListener(),
                false,
                Map.of(),
                Set.of(coords))
            .download(coords);

    assertTrue(downloadResult.getPath().isEmpty());
    assertTrue(downloadResult.getSha256().isEmpty());
  }

  @Test
  public void downloaderHandleUndeclaredCharacterEntityInPOM() throws IOException {
    Coordinates coords = new Coordinates("com.example:characterentity:1.0");

    String pomContents =
        "<project>\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>"
            + coords.getGroupId()
            + "</groupId>\n"
            + "  <artifactId>"
            + coords.getArtifactId()
            + "</artifactId>\n"
            + "  <packaging>pom</packaging>\n"
            + "  <version>"
            + coords.getVersion()
            + "</version>\n"
            + "  <developers>\n"
            + "    <developer>\n"
            + "      <name>First Las&oslash;t</name>\n"
            + "    </developer>\n"
            + "  </developers>\n"
            + "</project>\n";

    Path repo = MavenRepo.create().writePomFile(coords, pomContents).getPath();
    Path localRepo = Files.createTempDirectory("local");

    DownloadResult downloadResult =
        new Downloader(
                Netrc.fromUserHome(),
                localRepo,
                Set.of(repo.toUri()),
                new NullListener(),
                false,
                Map.of())
            .download(coords);

    assertTrue(downloadResult.getPath().isEmpty());
  }
}
