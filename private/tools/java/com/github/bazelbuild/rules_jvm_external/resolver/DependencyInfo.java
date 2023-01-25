package com.github.bazelbuild.rules_jvm_external.resolver;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class DependencyInfo {

  private final Coordinates coordinates;
  private final Set<URI> repos;
  private final Path path;
  private final String sha256;
  private final Set<Coordinates> dependencies;
  private final Set<String> packages;

  public DependencyInfo(
      Coordinates coordinates,
      Set<URI> repos,
      Path path,
      String sha256,
      Set<Coordinates> dependencies,
      Set<String> packages) {
    this.coordinates = coordinates;
    this.repos = ImmutableSet.copyOf(repos);
    this.path = path;
    this.sha256 = sha256;
    this.dependencies = ImmutableSet.copyOf(new TreeSet<>(dependencies));

    this.packages = ImmutableSet.copyOf(new TreeSet<>(packages));
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public Set<URI> getRepositories() {
    return repos;
  }

  public String getSha256() {
    return sha256;
  }

  public Set<Coordinates> getDependencies() {
    return dependencies;
  }

  public Set<String> getPackages() {
    return packages;
  }

  public Path getPath() {
    return path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DependencyInfo that = (DependencyInfo) o;
    return Objects.equals(coordinates, that.coordinates)
        && Objects.equals(sha256, that.sha256)
        && Objects.equals(dependencies, that.dependencies)
        && Objects.equals(packages, that.packages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinates, sha256, dependencies, packages);
  }
}
