package com.github.bazelbuild.rules_jvm_external.resolver.cmd;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class ExternalConfig {

  private Set<String> repositories;
  private Set<String> globalExclusions;
  private Set<ConfigArtifact> artifacts;
  private Set<ConfigArtifact> boms;

  private boolean fetchSources;
  private boolean fetchJavadoc;
  private boolean useUnsafeSharedCache;

  public Set<String> getRepositories() {
    return repositories == null ? ImmutableSet.of() : ImmutableSet.copyOf(repositories);
  }

  public Set<Coordinates> getGlobalExclusions() {
    if (globalExclusions == null) {
      return Set.of();
    }

    return globalExclusions.stream().map(Coordinates::new).collect(ImmutableSet.toImmutableSet());
  }

  public Set<ConfigArtifact> getArtifacts() {
    return artifacts == null ? ImmutableSet.of() : ImmutableSet.copyOf(artifacts);
  }

  public Set<ConfigArtifact> getBoms() {
    return boms == null ? ImmutableSet.of() : ImmutableSet.copyOf(boms);
  }

  public boolean isFetchSources() {
    return fetchSources;
  }

  public boolean isFetchJavadoc() {
    return fetchJavadoc;
  }

  public boolean isUsingUnsafeSharedCache() {
    return useUnsafeSharedCache;
  }
}
