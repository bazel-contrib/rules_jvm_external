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

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * A node in a {@link ResolutionResult}: a coordinate that was resolved, together with the local
 * path to its artifact if one is known. The path is absent when the resolver has not (yet) fetched
 * a file for the coordinate.
 */
public final class ResolvedArtifact {

  private final Coordinates coordinates;
  private final Optional<Path> path;

  public ResolvedArtifact(Coordinates coordinates, Path path) {
    this.coordinates = Objects.requireNonNull(coordinates);
    this.path = Optional.ofNullable(path);
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public Optional<Path> getPath() {
    return path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ResolvedArtifact)) {
      return false;
    }
    ResolvedArtifact that = (ResolvedArtifact) o;
    return coordinates.equals(that.coordinates) && path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinates, path);
  }

  @Override
  public String toString() {
    return "ResolvedArtifact{" + coordinates + ", path=" + path.orElse(null) + "}";
  }
}
