#!/bin/bash
#
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

set -eux
set -o pipefail

readonly SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# try to run it once here to bootstrap
$SCRIPT_DIR/../../third_party/coursier/coursier

# Get list of inputs to the artifact library, which is a transitive exports of deps
bazel query @other_maven//:com_google_guava_guava_27_0_jre --output=xml \
  | grep "rule-input" \
  | cut -d"\"" -f2 \
  | cut -d":" -f2 > /tmp/without_exclusions.txt

# Check that the artifacts are not excluded.
grep "com_google_j2objc_j2objc_annotations_1_1" /tmp/without_exclusions.txt
grep "org_codehaus_mojo_animal_sniffer_annotations_1_17" /tmp/without_exclusions.txt

# Get list of inputs to the artifact library with excluded artifacts
bazel query @other_maven_with_exclusions//:com_google_guava_guava_27_0_jre --output=xml \
  | grep "rule-input" \
  | cut -d"\"" -f2 \
  | cut -d":" -f2 > /tmp/with_exclusions.txt

# Check that the artifacts are excluded.
grep -v "com_google_j2objc_j2objc_annotations_1_1" /tmp/with_exclusions.txt
grep -v "org_codehaus_mojo_animal_sniffer_annotations_1_17" /tmp/with_exclusions.txt
