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

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.net.URI;
import java.util.Collection;

public interface MetadataService {
  DependencyMetadata getMetadata(Coordinates coords, Collection<URI> repositories);

  /**
   * Initializes the metadata service.
   *
   * If this service is a custom implementation loaded via SPI, this method will be called
   * with the default local metadata service (e.g., LocalMetadataService). This allows
   * custom implementations to delegate to the default local resolver logic when they
   * cannot fulfill a metadata lookup themselves.
   *
   * @param defaultService the default local metadata service to delegate/fallback to
   */
  default void initialize(MetadataService defaultService) {
    // no-op by default
  }
}
