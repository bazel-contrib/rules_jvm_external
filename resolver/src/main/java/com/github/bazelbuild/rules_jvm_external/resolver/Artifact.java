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
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class Artifact {

  private final Coordinates coordinates;
  private final Set<Coordinates> exclusions;
  private final boolean forceVersion;
  private final boolean packagingExplicit;

  public Artifact(Coordinates coordinates, Coordinates... exclusions) {
    this(coordinates, ImmutableSet.copyOf(exclusions));
  }

  public Artifact(Coordinates coordinates, Collection<Coordinates> exclusions) {
    this(coordinates, exclusions, false);
  }

  public Artifact(
      Coordinates coordinates, Collection<Coordinates> exclusions, boolean forceVersion) {
    this(coordinates, exclusions, forceVersion, false);
  }

  public Artifact(
      Coordinates coordinates,
      Collection<Coordinates> exclusions,
      boolean forceVersion,
      boolean packagingExplicit) {
    this.coordinates = Objects.requireNonNull(coordinates);
    this.exclusions = ImmutableSet.copyOf(exclusions);
    this.forceVersion = forceVersion;
    this.packagingExplicit = packagingExplicit;
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public Set<Coordinates> getExclusions() {
    return exclusions;
  }

  public boolean isForceVersion() {
    return forceVersion;
  }

  public boolean isPackagingExplicit() {
    return packagingExplicit;
  }

  @Override
  public String toString() {
    if (exclusions.isEmpty()) {
      return "{" + coordinates + "}";
    }
    return "{" + coordinates + ", exclusions=" + exclusions + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Artifact that = (Artifact) o;
    return Objects.equals(this.getCoordinates(), that.getCoordinates())
        && Objects.equals(this.getExclusions(), that.getExclusions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCoordinates(), getExclusions());
  }
}
