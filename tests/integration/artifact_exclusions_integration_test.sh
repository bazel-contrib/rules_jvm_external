#!/bin/bash

set -eux
set -o pipefail

readonly SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# try to run it once here to bootstrap
$SCRIPT_DIR/../../third_party/coursier/coursier

# Get list of inputs to the artifact library, which is a transitive exports of deps
bazel query @other_maven//:com_google_guava_guava_lib --output=xml \
  | grep "rule-input" \
  | cut -d"\"" -f2 \
  | cut -d":" -f2 \
  | sort > /tmp/without_exclusions.txt

# Get list of inputs to the artifact library with excluded artifacts
bazel query @other_maven_with_exclusions//:com_google_guava_guava_lib --output=xml \
  | grep "rule-input" \
  | cut -d"\"" -f2 \
  | cut -d":" -f2 \
  | sort > /tmp/with_exclusions.txt

# Get the diff between the two
diff /tmp/with_exclusions.txt /tmp/without_exclusions.txt > /tmp/exclusion_diff.txt || true

# Assert that the diff matches the two excluded artifacts
diff /tmp/exclusion_diff.txt $SCRIPT_DIR/artifact_exclusions.golden
