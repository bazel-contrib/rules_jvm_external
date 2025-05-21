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

public interface GradleResolvedDependency {
    public String getGroup();

    public void setGroup(String group);

    public String getName();

    public void setName(String name);

    public String getVersion();

    public void setVersion(String version);
    public String getRequestedVersion();

    public void setRequestedVersion(String requestedVersion);

    public boolean isConflict();

    public void setConflict(boolean conflict);

    public List<GradleResolvedDependency> getChildren();

    public void setChildren(List<GradleResolvedDependency> children);

    public boolean isFromBom();
    public void setFromBom(boolean fromBom);

    public List<GradleResolvedArtifact> getArtifacts();

    public void setArtifacts(List<GradleResolvedArtifact> artifacts);

    public void addArtifact(GradleResolvedArtifact artifact);
}
