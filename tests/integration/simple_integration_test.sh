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

# java_binary test
bazel build //tests/integration:java_binary_test_deploy.jar
jar tf $SCRIPT_DIR/../../bazel-bin/tests/integration/java_binary_test_deploy.jar | sort > /tmp/java_binary_deploy_jar.actual
diff $SCRIPT_DIR/java_binary_deploy_jar.golden /tmp/java_binary_deploy_jar.actual

# java_binary srcjar test
bazel build //tests/integration:java_binary_test_deploy-src.jar
jar tf $SCRIPT_DIR/../../bazel-bin/tests/integration/java_binary_test_deploy-src.jar | sort > /tmp/java_binary_deploy_src_jar.actual
diff $SCRIPT_DIR/java_binary_deploy_src_jar.golden /tmp/java_binary_deploy_src_jar.actual

# android_binary test
bazel build //tests/integration:android_binary_test
jar tf $SCRIPT_DIR/../../bazel-bin/tests/integration/android_binary_test_deploy.jar | sort > /tmp/android_binary_deploy_jar.actual
diff $SCRIPT_DIR/android_binary_deploy_jar.golden /tmp/android_binary_deploy_jar.actual
