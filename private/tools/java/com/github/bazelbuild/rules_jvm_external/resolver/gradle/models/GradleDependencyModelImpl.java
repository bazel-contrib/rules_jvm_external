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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GradleDependencyModelImpl implements Serializable, GradleDependencyModel {
    private final Map<String, List<GradleDependency>> declared = new LinkedHashMap<>();
    private final Map<String, List<GradleResolvedDependency>> resolved = new LinkedHashMap<>();
    private final Map<String, List<GradleDependency>> boms = new LinkedHashMap<>();

    public Map<String, List<GradleDependency>> getDeclaredDependencies() { return declared; }
    public Map<String, List<GradleResolvedDependency>> getResolvedDependencies() { return resolved; }
    public Map<String, List<GradleDependency>> getBoms() { return boms; }
}
