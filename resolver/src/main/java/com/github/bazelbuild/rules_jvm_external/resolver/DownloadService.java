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

import java.net.URI;
import java.nio.file.Path;

/**
 * Service defining operations to download files and inspect remote URIs.
 * Custom implementations of this SPI can be registered on the classpath to
 * intercept or override caching and authentication during resolution.
 */
public interface DownloadService {
  /**
   * Gets/downloads the content of the specified URI to a local path.
   *
   * @param uri the URI of the resource to download
   * @return the local Path where the resource was downloaded, or null if the download fails
   */
  Path get(URI uri);

  /**
   * Checks if the resource at the specified URI exists.
   *
   * @param uri the URI to inspect
   * @return true if the resource exists, false otherwise
   */
  boolean head(URI uri);

  /**
   * Initializes the download service.
   *
   * If this service is a custom implementation loaded via SPI, this method will be called
   * with the default local HTTP downloader. This allows custom implementations to delegate
   * to the default HTTP downloader.
   *
   * @param defaultService the default download service
   */
  default void initialize(DownloadService defaultService) {
    // no-op by default
  }
}
