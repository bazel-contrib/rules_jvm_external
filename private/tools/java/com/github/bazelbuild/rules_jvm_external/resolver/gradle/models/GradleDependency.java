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


package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.util.List;

/**
 * Represents a Gradle dependency in build.gradle.kts
 */
public class GradleDependency {
    public enum Scope {
        IMPLEMENTATION,
        COMPILE_ONLY,
        RUNTIME_ONLY,
        TEST_IMPLEMENTATION,
    }

    public final Scope scope;
    public final String group;
    public final String artifact;
    public final String version;
    public final List<Exclusion> exclusions;

    public GradleDependency(Scope scope, String group, String artifact, String version, List<Exclusion> exclusions) {
        this.scope = scope;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.exclusions = exclusions != null ? exclusions : List.of();
    }

    public GradleDependency(Scope scope, String group, String artifact, String version) {
        this(scope, group, artifact, version, List.of());
    }
}