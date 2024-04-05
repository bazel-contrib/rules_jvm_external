/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.bazelbuild.rules_jvm_external.resolver.gradle.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class DefaultOutgoingArtifactsModel implements OutgoingArtifactsModel, Serializable {
  private final Map<String, Set<String>> artifacts;
  private final Map<String, String> conflicts;

  public DefaultOutgoingArtifactsModel(
      Map<String, Set<String>> artifacts, Map<String, String> conflicts) {
    this.artifacts = Map.copyOf(artifacts);
    this.conflicts = Map.copyOf(conflicts);
  }

  @Override
  public Map<String, Set<String>> getArtifacts() {
    return artifacts;
  }

  public Map<String, String> getConflicts() {
    return conflicts;
  }
}
