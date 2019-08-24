#!/usr/bin/env bash

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
#
# This script mirrors the Coursier standalone jar to the Bazel mirror
# on Google Cloud Storage for redundancy. The original artifacts are
# hosted on https://github.com/coursier/coursier/releases.

set -euo pipefail

readonly repo_name=$(ls external/ | grep coursier_cli_v*)
readonly coursier_cli_jar="external/$repo_name/file/downloaded"
chmod u+x $coursier_cli_jar

# Upload Coursier to GCS
# -n for no-clobber, so we don't overwrite existing files
gsutil cp -n $coursier_cli_jar \
  gs://bazel-mirror/coursier_cli/$repo_name.jar
