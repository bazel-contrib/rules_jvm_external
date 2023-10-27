package com.github.bazelbuild.rules_jvm_external.resolver.remote;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DownloadResult {

  private final Coordinates coordinates;
  private final Set<URI> repos;
  private final Optional<Path> path;
  private final Optional<String> sha256;

  public DownloadResult(Coordinates coordinates, Set<URI> repos, Path path, String sha256) {
    this.coordinates = Objects.requireNonNull(coordinates);
    this.repos = ImmutableSet.copyOf(repos);
    this.path = Optional.ofNullable(path);
    this.sha256 = Optional.ofNullable(sha256);
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public Set<URI> getRepositories() {
    return repos;
  }

  public Optional<Path> getPath() {
    return path;
  }

  public Optional<String> getSha256() {
    return sha256;
  }
}
