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

package com.github.bazelbuild.rules_jvm_external.resolver.maven;

import com.github.bazelbuild.rules_jvm_external.resolver.DownloadService;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.Transporter;

class HttpDownloaderTransporter implements Transporter {
  private final DownloadService downloadService;
  private final URI baseUri;

  public HttpDownloaderTransporter(DownloadService downloadService, RemoteRepository repository) {
    this.downloadService = downloadService;
    this.baseUri = URI.create(repository.getUrl());
  }

  private URI getAbsoluteUri(URI relative) {
    String path = baseUri.getPath();
    if (path == null) {
      path = "";
    }
    if (!path.endsWith("/")) {
      path += "/";
    }
    String relPath = relative.getPath();
    if (relPath.startsWith("/")) {
      relPath = relPath.substring(1);
    }
    path += relPath;

    try {
      return new URI(
          baseUri.getScheme(),
          baseUri.getUserInfo(),
          baseUri.getHost(),
          baseUri.getPort(),
          path,
          baseUri.getQuery(),
          baseUri.getFragment());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int classify(Throwable error) {
    if (error instanceof FileNotFoundException) {
      return ERROR_NOT_FOUND;
    }
    return ERROR_OTHER;
  }

  @Override
  public void peek(PeekTask task) throws Exception {
    URI uri = getAbsoluteUri(task.getLocation());
    if (!downloadService.head(uri)) {
      throw new FileNotFoundException("Resource not found: " + uri);
    }
  }

  @Override
  public void get(GetTask task) throws Exception {
    URI uri = getAbsoluteUri(task.getLocation());
    Path downloaded = downloadService.get(uri);
    if (downloaded == null) {
      throw new FileNotFoundException("Resource not found: " + uri);
    }

    if (task.getDataFile() != null) {
      Path dest = task.getDataFile().toPath();
      if (dest.getParent() != null) {
        Files.createDirectories(dest.getParent());
      }
      Files.copy(downloaded, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    if (null != uri.getScheme() && !uri.getScheme().equals("file")) {
      Files.deleteIfExists(downloaded);
    }
  }

  @Override
  public void put(PutTask task) throws Exception {
    throw new UnsupportedOperationException("Uploads not supported");
  }

  @Override
  public void close() {
    // HttpDownloader lifecycle is managed externally
  }
}
