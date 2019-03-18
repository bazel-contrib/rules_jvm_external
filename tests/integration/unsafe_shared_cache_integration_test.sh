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

# tests for use_unsafe_shared_cache = True
bazel fetch @unsafe_shared_cache//:all

file $(bazel info output_base)/external/unsafe_shared_cache/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar | grep "symbolic link to" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = True: guava-27.0-jre.jar is not a symbolic link"

file $(bazel info output_base)/external/unsafe_shared_cache/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre-sources.jar | grep "symbolic link to" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = True: guava-27.0-jre-sources.jar is not a symbolic link"

# tests for use_unsafe_shared_cache = False
bazel fetch @other_maven//:all

file $(bazel info output_base)/external/other_maven/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar | grep "Java archive data (JAR)" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = False: guava-27.0-jre.jar is not a JAR"

file $(bazel info output_base)/external/other_maven/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre-sources.jar | grep "Zip archive data" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = False: guava-27.0-jre-sources.jar is not a Zip archive data"
