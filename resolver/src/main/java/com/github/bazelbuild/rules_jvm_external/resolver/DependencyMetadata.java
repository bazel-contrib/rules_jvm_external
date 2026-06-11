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

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class DependencyMetadata {
  private final String sha256;
  private final Set<URI> repositories;
  private final Set<String> packages;
  private final Set<String> classes;
  private final Map<String, ? extends Set<String>> services;

  public DependencyMetadata(
      String sha256,
      Set<URI> repositories,
      Set<String> packages,
      Set<String> classes,
      Map<String, ? extends Set<String>> services) {
    this.sha256 = sha256;
    this.repositories = repositories == null ? java.util.Set.of() : java.util.Set.copyOf(repositories);
    this.packages = packages == null ? java.util.Set.of() : java.util.Set.copyOf(packages);
    this.classes = classes == null ? java.util.Set.of() : java.util.Set.copyOf(classes);
    if (services == null) {
      this.services = java.util.Map.of();
    } else {
      java.util.Map<String, java.util.Set<String>> copy = new java.util.LinkedHashMap<>();
      for (java.util.Map.Entry<String, ? extends Set<String>> entry : services.entrySet()) {
        copy.put(entry.getKey(), entry.getValue() == null ? java.util.Set.of() : java.util.Set.copyOf(entry.getValue()));
      }
      this.services = java.util.Map.copyOf(copy);
    }
  }

  public String getSha256() {
    return sha256;
  }

  public Set<URI> getRepositories() {
    return repositories;
  }

  public Set<String> getPackages() {
    return packages;
  }

  public Set<String> getClasses() {
    return classes;
  }

  public Map<String, ? extends Set<String>> getServices() {
    return services;
  }
}
