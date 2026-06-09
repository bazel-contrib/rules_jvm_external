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
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * A node in a {@link ResolutionResult}: a coordinate that was resolved, together with the local
 * path to its artifact if one is known. The path is absent when the resolver has not (yet) fetched
 * a file for the coordinate.
 *
 * <p>An empty path is ambiguous on its own: for some resolvers it simply means "the downloader
 * still has to fetch this", while for others it means "this coordinate has no binary at all". The
 * {@link #isAggregator()} flag disambiguates the second case: when {@code true}, the resolver has
 * positively determined that the coordinate ships no binary of its own and exists only to pull in
 * other artifacts (a Gradle module-metadata umbrella, an {@code available-at} redirect, or a {@code
 * <packaging>pom</packaging>} aggregator). Such a node should be rendered as an exports-only
 * wrapper rather than having a file downloaded and hashed for it. This is distinct from the
 * graph-surgery notion of an "aggregating dependency" in {@code GradleResolver} (which removes the
 * node entirely); an aggregator here remains in the graph as a wrapper.
 */
public final class ResolvedArtifact {

  private final Coordinates coordinates;
  private final Optional<Path> path;
  private final boolean aggregator;

  public ResolvedArtifact(Coordinates coordinates, Path path) {
    this(coordinates, path, false);
  }

  public ResolvedArtifact(Coordinates coordinates, Path path, boolean aggregator) {
    this.coordinates = Objects.requireNonNull(coordinates);
    this.path = Optional.ofNullable(path);
    this.aggregator = aggregator;
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public Optional<Path> getPath() {
    return path;
  }

  /**
   * Whether the resolver determined this coordinate has no binary of its own and exists only to
   * aggregate or redirect to other artifacts. See the class javadoc for details.
   */
  public boolean isAggregator() {
    return aggregator;
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
    return aggregator == that.aggregator
        && coordinates.equals(that.coordinates)
        && path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinates, path, aggregator);
  }

  @Override
  public String toString() {
    return "ResolvedArtifact{"
        + coordinates
        + ", path="
        + path.orElse(null)
        + ", aggregator="
        + aggregator
        + "}";
  }
}
