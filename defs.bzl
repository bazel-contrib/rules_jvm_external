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

load("//:coursier.bzl", "coursier_fetch")
load("//:specs.bzl", "maven", "parse", "json")

def gmaven_artifact(fqn):
  parts = fqn.split(":")
  packaging = "jar"

  if len(parts) == 3:
    group_id, artifact_id, version = parts
  elif len(parts) == 4:
    group_id, artifact_id, packaging, version = parts
  elif len(parts) == 5:
    _, _, _, classifier, _ = parts
    fail("Classifiers are currently not supported. Please remove it from the coordinate: %s" % classifier)
  else:
    fail("Invalid qualified name for artifact: %s" % fqn)

  return "@%s_%s_%s//%s" % (
      escape(group_id),
      escape(artifact_id),
      escape(version),
      packaging
      )

def escape(string):
  return string.replace(".", "_").replace("-", "_")

REPOSITORY_NAME = "maven"

def maven_install(
        name = REPOSITORY_NAME,
        repositories = [],
        artifacts = [],
        fetch_sources = False,
        use_unsafe_shared_cache = False):
    """Resolves and fetches artifacts transitively from Maven repositories.

    This macro runs a repository rule that invokes the Coursier CLI to resolve
    and fetch Maven artifacts transitively.

    Args:
      name: A unique name for this Bazel external repository.
      repositories: A list of Maven repository URLs, specified in lookup order.

        Supports URLs with HTTP Basic Authentication, e.g. "https://username:password@example.com".
      artifacts: A list of Maven artifact coordinates in the form of `group-id:artifact-id:version`.
      fetch_sources: Additionally fetch source JARs.
      use_unsafe_shared_cache: Download artifacts into a persistent shared cache on disk. Unsafe as Bazel is
        currently unable to detect modifications to the cache.
    """

    repositories_json_strings = []
    for repository in parse.parse_repository_spec_list(repositories):
        repositories_json_strings.append(json.write_repository_spec(repository))

    artifacts_json_strings = []
    for artifact in parse.parse_artifact_spec_list(artifacts):
        artifacts_json_strings.append(json.write_artifact_spec(artifact))

    coursier_fetch(
        name = name,
        repositories = repositories_json_strings,
        artifacts = artifacts_json_strings,
        fetch_sources = fetch_sources,
        use_unsafe_shared_cache = use_unsafe_shared_cache,
    )

def artifact(a, repository_name = REPOSITORY_NAME):
    """A helper macro to translate Maven coordinates into a Bazel target label.

    For example:

    `artifact("com.google.guava:guava")` translates into `@maven//:com_google_guava_guava`

    `artifact("com.google.guava:guava", repository_name = "custom_maven")` translates into `@custom_maven//:com_google_guava_guava`

    Args:
      name: The coordinates of a Maven artifact in the form of `group:artifact`. The version is not needed.
      repository_name: The name of the `maven_install` declaration in the WORKSPACE file containing this artifact.
    """
    artifact_obj = _parse_artifact_str(a) if type(a) == "string" else a
    return "@%s//:%s" % (repository_name, _escape(artifact_obj["group"] + ":" + artifact_obj["artifact"]))

def maven_artifact(a):
    return artifact(a, repository_name = REPOSITORY_NAME)

def _escape(string):
    return string.replace(".", "_").replace("-", "_").replace(":", "_")

def _parse_artifact_str(artifact_str):
    pieces = artifact_str.split(":")
    if len(pieces) == 2:
        return { "group": pieces[0], "artifact": pieces[1] }
    else:
        return parse.parse_maven_coordinate(artifact_str)
