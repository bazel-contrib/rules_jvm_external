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

package com.github.bazelbuild.rules_jvm_external.resolver.remote;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.jar.IndexJar;
import com.github.bazelbuild.rules_jvm_external.jar.PerJarIndexResults;
import com.github.bazelbuild.rules_jvm_external.resolver.DependencyMetadata;
import com.github.bazelbuild.rules_jvm_external.resolver.MetadataService;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

public class LocalMetadataService implements MetadataService {
  private final Downloader downloader;

  public LocalMetadataService(Downloader downloader) {
    this.downloader = downloader;
  }

  @Override
  public DependencyMetadata getMetadata(Coordinates coords, Collection<URI> repositories) {
    DownloadResult result = downloader.download(coords);
    if (result == null) {
      return null;
    }

    PerJarIndexResults indexResults;
    if (result.getPath().isPresent()) {
      try {
        indexResults = new IndexJar().index(result.getPath().get());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    } else {
      indexResults = new PerJarIndexResults(new TreeSet<>(), new TreeSet<>(), new TreeMap<>());
    }

    return new DependencyMetadata(
        result.getSha256().orElse(null),
        ImmutableSet.copyOf(result.getRepositories()),
        indexResults.getPackages(),
        indexResults.getClasses(),
        indexResults.getServiceImplementations());
  }
}
