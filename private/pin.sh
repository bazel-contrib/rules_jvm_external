#!/usr/bin/env bash

set -euo pipefail
readonly maven_install_json_loc=$BUILD_WORKSPACE_DIRECTORY/{repository_name}_install.json
readonly execution_root=$(bazel info execution_root)
readonly workspace_name=$(basename $execution_root)
cat <<"RULES_JVM_EXTERNAL_EOF" | python -m json.tool > $maven_install_json_loc
{dependency_tree_json}
RULES_JVM_EXTERNAL_EOF

if [ "{reading_from_json}" = "True" ]; then
    echo "Successfully pinned resolved artifacts for @{repository_name}, $maven_install_json_loc is now up-to-date."
else
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
    maven_install_json = "@$workspace_name//:{repository_name}_install.json",
)

load("@{repository_name}//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

=============================================================
EOF

    echo
    echo "To update {repository_name}_install.json, run this command to re-pin the unpinned repository:"
    echo
    echo "    bazel run @unpinned_{repository_name}//:pin"
fi
echo
