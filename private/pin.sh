#!/bin/bash

set -euo pipefail
readonly maven_install_json_loc=$BUILD_WORKSPACE_DIRECTORY/{repository_name}_install.json
echo {dependency_tree_json} | python -m json.tool > $maven_install_json_loc
echo "Successfully pinned resolved artifacts for @{repository_name} in $maven_install_json_loc." \
  "This file should be checked in your version control system."
echo
echo "Next, please update your WORKSPACE file by adding the maven_install_json attribute" \
  "and loading pinned_maven_install from @{repository_name}//:defs.bzl".
echo
echo "For example:"
echo
cat <<EOF
=============================================================

maven_install(
    artifacts = # ...,
    repositories = # ...,
    maven_install_json = "//:{repository_name}_install.json",
)

load("@{repository_name}//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

=============================================================
EOF

echo
echo "To update {repository_name}_install.json, run this command to re-pin the unpinned repository:"
echo
echo "    bazel run @unpinned_{repository_name}//:pin"
echo
