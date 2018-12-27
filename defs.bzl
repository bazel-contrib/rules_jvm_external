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

load("@rules_maven//:coursier.bzl", "coursier_fetch")

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
        fetch_sources = False):
    coursier_fetch(
        name = name,
        repositories = repositories,
        artifacts = artifacts,
        fetch_sources = fetch_sources,
    )

def maven_artifact(fqn):
    return "@%s//:%s" % (REPOSITORY_NAME, _escape(fqn))

def artifact(fqn, repository_name = REPOSITORY_NAME):
    return "@%s//:%s" % (repository_name, _escape(fqn))

def _escape(string):
    return string.replace(".", "_").replace("-", "_").replace(":", "_")
