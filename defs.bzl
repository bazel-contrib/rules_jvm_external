# Copyright 2019 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@rules_jvm_external//:coursier.bzl", "coursier_fetch")
load("@rules_jvm_external//:specs.bzl", "json", "parse")

DEFAULT_REPOSITORY_NAME = "maven"

def maven_install(
        name = DEFAULT_REPOSITORY_NAME,
        repositories = [],
        artifacts = [],
        fail_on_missing_checksum = True,
        fetch_sources = False,
        use_unsafe_shared_cache = False,
        excluded_artifacts = [],
        generate_compat_repositories = False,
        maven_install_json = None):
    repositories_json_strings = []
    for repository in parse.parse_repository_spec_list(repositories):
        repositories_json_strings.append(json.write_repository_spec(repository))

    artifacts_json_strings = []
    for artifact in parse.parse_artifact_spec_list(artifacts):
        artifacts_json_strings.append(json.write_artifact_spec(artifact))

    excluded_artifacts_json_strings = []
    for exclusion in parse.parse_exclusion_spec_list(excluded_artifacts):
        excluded_artifacts_json_strings.append(json.write_exclusion_spec(exclusion))

    coursier_fetch(
        name = name,
        repositories = repositories_json_strings,
        artifacts = artifacts_json_strings,
        fail_on_missing_checksum = fail_on_missing_checksum,
        fetch_sources = fetch_sources,
        use_unsafe_shared_cache = use_unsafe_shared_cache,
        excluded_artifacts = excluded_artifacts_json_strings,
        generate_compat_repositories = generate_compat_repositories,
    )

    if maven_install_json != None:
        coursier_fetch(
            name = "pinned_" + name,
            maven_install_json = maven_install_json,
            fetch_sources = fetch_sources,
            generate_compat_repositories = generate_compat_repositories,
        )


def artifact(a, repository_name = DEFAULT_REPOSITORY_NAME):
    artifact_obj = _parse_artifact_str(a) if type(a) == "string" else a
    return "@%s//:%s" % (repository_name, _escape(artifact_obj["group"] + ":" + artifact_obj["artifact"]))

def maven_artifact(a):
    return artifact(a, repository_name = DEFAULT_REPOSITORY_NAME)

def _escape(string):
    return string.replace(".", "_").replace("-", "_").replace(":", "_")

def _parse_artifact_str(artifact_str):
    pieces = artifact_str.split(":")
    if len(pieces) == 2:
        return {"group": pieces[0], "artifact": pieces[1]}
    else:
        return parse.parse_maven_coordinate(artifact_str)
