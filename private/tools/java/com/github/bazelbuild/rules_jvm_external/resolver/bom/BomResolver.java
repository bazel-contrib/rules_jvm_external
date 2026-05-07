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

package com.github.bazelbuild.rules_jvm_external.resolver.bom;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

/**
 * Pure logic for computing which directly-declared BOM(s) manage each requested versionless
 * artifact. The result is suitable to be written into the {@code bom_resolution} section of a v3
 * lock file.
 *
 * <p>This class is intentionally I/O-light: it owns the Aether session lifecycle and the actual
 * BOM resolution, but nothing else (no JSON, no CLI argument parsing, no filesystem writes).
 */
public final class BomResolver {

  private BomResolver() {}

  /**
   * Compute the BOM resolution mapping.
   *
   * @param repositories ordered list of Maven repository URIs to consult
   * @param bomCoordinates ordered list of directly-declared BOM coordinate strings; declaration
   *     order is preserved in the returned mapping's value lists
   * @param versionlessArtifacts the requested versionless artifacts that may be managed by a BOM.
   *     The caller is responsible for filtering out {@code excluded_artifacts}, {@code
   *     override_targets}, and explicitly version-pinned artifacts before invocation.
   * @param netrcFile optional path to a {@code .netrc} file for repository credentials; may be
   *     {@code null}
   * @return a map keyed by artifact key (per the rules in {@link #artifactKey(Coordinates)}) to a
   *     deduplicated, declaration-ordered list of managing BOM coordinate strings. Entries with
   *     no managing BOM are omitted.
   * @throws RuntimeException if any BOM cannot be resolved (no partial output is returned)
   */
  public static Map<String, List<String>> buildBomResolutionMapping(
      List<URI> repositories,
      List<String> bomCoordinates,
      List<Coordinates> versionlessArtifacts,
      Path netrcFile) {
    Objects.requireNonNull(repositories, "repositories");
    Objects.requireNonNull(bomCoordinates, "bomCoordinates");
    Objects.requireNonNull(versionlessArtifacts, "versionlessArtifacts");

    if (bomCoordinates.isEmpty() || versionlessArtifacts.isEmpty()) {
      return new LinkedHashMap<>();
    }

    Netrc netrc = loadNetrc(netrcFile);
    RepositorySystem system = createRepositorySystem();
    RepositorySystemSession session = createSession(system);
    List<RemoteRepository> remoteRepositories = createRemoteRepositories(repositories, netrc);

    // For each declared BOM, compute the set of managed g:a[:e[:c]] keys.
    // Keep declaration order via a LinkedHashMap.
    Map<String, Set<String>> bomToManagedKeys = new LinkedHashMap<>();
    for (String bomCoord : bomCoordinates) {
      Set<String> managedKeys =
          getEffectiveManagedDependencies(system, session, remoteRepositories, bomCoord);
      bomToManagedKeys.put(bomCoord, managedKeys);
    }

    // For each versionless artifact, walk the BOMs in declaration order and record matches.
    // Collapse exact-duplicate BOM coords; "first wins" for any group:artifact-conflicting BOMs
    // is naturally preserved by iterating declaration order and using a LinkedHashSet.
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (Coordinates artifact : versionlessArtifacts) {
      String artifactKey = artifactKey(artifact);
      Set<String> bomsForArtifact = new LinkedHashSet<>();

      for (Map.Entry<String, Set<String>> entry : bomToManagedKeys.entrySet()) {
        if (entry.getValue().contains(artifactKey)) {
          bomsForArtifact.add(entry.getKey());
        }
      }

      if (!bomsForArtifact.isEmpty()) {
        result.put(artifactKey, new ArrayList<>(bomsForArtifact));
      }
    }
    return result;
  }

  /**
   * Returns the set of artifact keys managed by the BOM identified by {@code bomCoord}, expanded
   * via Aether's {@link RepositorySystem#readArtifactDescriptor}. Sub-BOMs imported transitively
   * are flattened into this set, so the caller does not need to walk a tree.
   *
   * <p>Visible for testing.
   */
  public static Set<String> getEffectiveManagedDependencies(
      RepositorySystem system,
      RepositorySystemSession session,
      List<RemoteRepository> repositories,
      String bomCoord) {
    Coordinates coords = new Coordinates(bomCoord);
    DefaultArtifact bomArtifact =
        new DefaultArtifact(
            coords.getGroupId(),
            coords.getArtifactId(),
            "",
            "pom",
            coords.getVersion());

    // First, ensure the BOM POM itself exists. Aether's readArtifactDescriptor can
    // silently return an empty result for a missing POM in a file:// repo (no 404 is
    // generated by the file transport). Resolving the POM artifact up front gives us
    // a real ArtifactResolutionException for missing BOMs, satisfying Constraint #11
    // (hard-fail on resolution errors; never write a partial bom_resolution section).
    try {
      system.resolveArtifact(session, new ArtifactRequest(bomArtifact, repositories, null));
    } catch (ArtifactResolutionException e) {
      throw new RuntimeException(
          "Failed to resolve BOM " + bomCoord + ": " + e.getMessage(), e);
    }

    ArtifactDescriptorRequest request =
        new ArtifactDescriptorRequest(bomArtifact, repositories, JavaScopes.COMPILE);

    ArtifactDescriptorResult result;
    try {
      result = system.readArtifactDescriptor(session, request);
    } catch (ArtifactDescriptorException e) {
      throw new RuntimeException(
          "Failed to resolve BOM " + bomCoord + ": " + e.getMessage(), e);
    }

    // Belt-and-suspenders: also catch result-embedded exceptions.
    if (!result.getExceptions().isEmpty()) {
      Exception first = result.getExceptions().get(0);
      throw new RuntimeException(
          "Failed to resolve BOM " + bomCoord + ": " + first.getMessage(), first);
    }

    Set<String> managed = new LinkedHashSet<>();
    for (Dependency managedDep : result.getManagedDependencies()) {
      org.eclipse.aether.artifact.Artifact a = managedDep.getArtifact();
      Coordinates managedCoords =
          new Coordinates(
              a.getGroupId(), a.getArtifactId(), a.getExtension(), a.getClassifier(), null);
      managed.add(artifactKey(managedCoords));
    }
    return managed;
  }

  /**
   * Compute the lock-file map key for an artifact.
   *
   * <p>Per the spec:
   *
   * <ul>
   *   <li>Default packaging ({@code jar}) and no classifier: {@code group:artifact}
   *   <li>Otherwise: {@code group:artifact:packaging[:classifier]}
   * </ul>
   */
  public static String artifactKey(Coordinates coords) {
    String extension = coords.getExtension();
    String classifier = coords.getClassifier();
    boolean defaultExtension = extension == null || extension.isEmpty() || "jar".equals(extension);
    boolean noClassifier = classifier == null || classifier.isEmpty();

    StringBuilder sb = new StringBuilder();
    sb.append(coords.getGroupId()).append(":").append(coords.getArtifactId());
    if (defaultExtension && noClassifier) {
      return sb.toString();
    }

    String renderedExtension = defaultExtension ? "jar" : extension;
    sb.append(":").append(renderedExtension);
    if (!noClassifier) {
      sb.append(":").append(classifier);
    }
    return sb.toString();
  }

  private static Netrc loadNetrc(Path netrcFile) {
    if (netrcFile == null) {
      return Netrc.fromUserHome();
    }
    if (!Files.exists(netrcFile)) {
      return Netrc.fromUserHome();
    }
    try (java.io.InputStream is = Files.newInputStream(netrcFile);
        java.io.BufferedInputStream bis = new java.io.BufferedInputStream(is)) {
      return Netrc.fromStream(bis);
    } catch (java.io.IOException e) {
      throw new RuntimeException("Unable to read netrc file " + netrcFile, e);
    }
  }

  private static RepositorySystem createRepositorySystem() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    return locator.getService(RepositorySystem.class);
  }

  private static RepositorySystemSession createSession(RepositorySystem system) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    Path tempLocalRepo;
    try {
      tempLocalRepo = Files.createTempDirectory("bom-resolver-cache");
    } catch (java.io.IOException e) {
      throw new RuntimeException("Unable to create local Aether cache", e);
    }
    LocalRepository localRepository = new LocalRepository(tempLocalRepo.toAbsolutePath().toString());
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
    session.setIgnoreArtifactDescriptorRepositories(true);

    Map<Object, Object> configProperties = new HashMap<>();
    configProperties.putAll(System.getProperties());
    session.setConfigProperties(configProperties);
    session.setSystemProperties(System.getProperties());
    return session;
  }

  private static List<RemoteRepository> createRemoteRepositories(
      List<URI> repositories, Netrc netrc) {
    return repositories.stream()
        .map(uri -> createRemoteRepository(uri, netrc))
        .collect(Collectors.toList());
  }

  private static RemoteRepository createRemoteRepository(URI uri, Netrc netrc) {
    RemoteRepository.Builder builder =
        new RemoteRepository.Builder(uri.toString(), "default", uri.toString());
    builder.setSnapshotPolicy(
        new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY,
            RepositoryPolicy.CHECKSUM_POLICY_WARN));
    builder.setReleasePolicy(
        new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY,
            RepositoryPolicy.CHECKSUM_POLICY_WARN));

    Netrc.Credential credential = null;
    if (uri.getHost() != null) {
      credential = netrc.getCredential(uri.getHost());
    }
    if (credential == null) {
      credential = netrc.defaultCredential();
    }
    if (credential != null) {
      Authentication authentication =
          new AuthenticationBuilder()
              .addUsername(credential.login())
              .addPassword(credential.password())
              .build();
      builder.setAuthentication(authentication);
    }
    return builder.build();
  }
}
