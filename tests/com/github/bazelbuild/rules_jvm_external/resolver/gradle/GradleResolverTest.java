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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.MavenRepo;
import com.github.bazelbuild.rules_jvm_external.resolver.Resolver;
import com.github.bazelbuild.rules_jvm_external.resolver.ResolverTestBase;
import com.github.bazelbuild.rules_jvm_external.resolver.cmd.ResolverConfig;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import com.google.common.graph.Graph;
import com.google.devtools.build.runfiles.AutoBazelRepository;
import com.google.devtools.build.runfiles.Runfiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;

@AutoBazelRepository
public class GradleResolverTest extends ResolverTestBase {

  @Override
  protected Resolver getResolver(Netrc netrc, EventListener listener) {
    return new GradleResolver(netrc, ResolverConfig.DEFAULT_MAX_THREADS, listener);
  }

  @Test
  public void resolvesSimpleJvmVariant() throws IOException, XMLStreamException {
    // This test validates gradle can resolve a artifact using only gradle module metadata
    // In this case, there's a root artifact com.example.sample which points to
    // com.example.sample-jvm, which satisfies the runtimeClasspath configuration
    // as it has the JVM variant attributes by default.
    Coordinates baseCoordinates = new Coordinates("com.example:sample:1.0");
    Coordinates jvmCoordinates = new Coordinates("com.example:sample-jvm:1.0");
    MavenRepo mavenRepo = MavenRepo.create();
    GradleModuleMetadataHelper moduleMetadataHelper = new GradleModuleMetadataHelper(mavenRepo);

    Runfiles runfiles =
        Runfiles.preload().withSourceRepository(AutoBazelRepository_GradleResolverTest.NAME);
    Path baseMetadataPath =
        Paths.get(
            runfiles.rlocation(
                "rules_jvm_external/tests/com/github/bazelbuild/rules_jvm_external/resolver/gradle/fixtures/simpleJvmVariant/sample-1.0.module"));
    String baseMetadata = Files.readString(baseMetadataPath);
    moduleMetadataHelper.addToMavenRepo(baseCoordinates, baseMetadata);

    Path jvmMetadataPath =
        Paths.get(
            runfiles.rlocation(
                "rules_jvm_external/tests/com/github/bazelbuild/rules_jvm_external/resolver/gradle/fixtures/simpleJvmVariant/sample-jvm-1.0.module"));
    String jvmMetadata = Files.readString(jvmMetadataPath);
    moduleMetadataHelper.addToMavenRepo(jvmCoordinates, jvmMetadata);

    Graph<Coordinates> resolved =
        resolver
            .resolve(prepareRequestFor(mavenRepo.getPath().toUri(), baseCoordinates))
            .getResolution();

    assertEquals(2, resolved.nodes().size());
    // sample-jvm resolves indirectly through sample using the gradle module metadata redirect
    assertEquals(List.of(baseCoordinates, jvmCoordinates), new ArrayList<>(resolved.nodes()));
  }

  @Test
  public void resolvesJvmButNotAndroidVariant() throws IOException, XMLStreamException {
    // This test validates a scenario similar to
    // https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/5.1.0/okhttp-5.1.0.module
    // which supports 2 different coordinates with one base coordinate - one for JVM and android
    // using variant selection.
    // Right now, we only resolve the default runtime classpath configuration, so we'll only resolve
    // the JVM variant
    // and won't have the android variant
    Coordinates baseCoordinates = new Coordinates("com.example:sample:1.0");
    Coordinates jvmCoordinates = new Coordinates("com.example:sample-jvm:1.0");
    Coordinates androidCoordinates = new Coordinates("com.example:sample-android:1.0");
    MavenRepo mavenRepo = MavenRepo.create();
    GradleModuleMetadataHelper moduleMetadataHelper = new GradleModuleMetadataHelper(mavenRepo);

    Runfiles runfiles =
        Runfiles.preload().withSourceRepository(AutoBazelRepository_GradleResolverTest.NAME);
    Path baseMetadataPath =
        Paths.get(
            runfiles.rlocation(
                "rules_jvm_external/tests/com/github/bazelbuild/rules_jvm_external/resolver/gradle/fixtures/jvmAndAndroidVariants/sample-1.0.module"));
    String baseMetadata = Files.readString(baseMetadataPath);
    moduleMetadataHelper.addToMavenRepo(baseCoordinates, baseMetadata);

    Path jvmMetadataPath =
        Paths.get(
            runfiles.rlocation(
                "rules_jvm_external/tests/com/github/bazelbuild/rules_jvm_external/resolver/gradle/fixtures/jvmAndAndroidVariants/sample-jvm-1.0.module"));
    String jvmMetadata = Files.readString(jvmMetadataPath);
    moduleMetadataHelper.addToMavenRepo(jvmCoordinates, jvmMetadata);

    Path androidMetadataPath =
        Paths.get(
            runfiles.rlocation(
                "rules_jvm_external/tests/com/github/bazelbuild/rules_jvm_external/resolver/gradle/fixtures/jvmAndAndroidVariants/sample-android-1.0.module"));
    String androidMetadata = Files.readString(androidMetadataPath);
    moduleMetadataHelper.addToMavenRepo(androidCoordinates, androidMetadata);

    Graph<Coordinates> resolved =
        resolver
            .resolve(prepareRequestFor(mavenRepo.getPath().toUri(), baseCoordinates))
            .getResolution();

    // sample-jvm resolves indirectly through sample using the gradle module metadata redirect
    // but not sample-android as we don't resolve multiple variants currently. O
    assertEquals(2, resolved.nodes().size());
    ArrayList allNodes = new ArrayList<>(resolved.nodes());
    assertEquals(List.of(baseCoordinates, jvmCoordinates), allNodes);
    // Once we support resolving android variant, this test should be updated to ensure
    // sample-android is also
    // resolved
    assertFalse(allNodes.contains(androidCoordinates));
  }
}
