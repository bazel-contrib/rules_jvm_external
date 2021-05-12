#!/usr/bin/env bash

set -euo pipefail
readonly maven_install_json_loc={maven_install_location}
# `jq` is a platform-specific dependency with an unpredictable path.
# Note that $(rootpath) will always give external/unpinned_maven/jq, however under --nolegacy_external_runfiles
# there is only pin.runfiles/unpinned_maven/jq not also pin.runfiles/user_repo/external/unpinned_maven/jq
# So replace leading external/ with ../
readonly jq=${1/#external\//..\/}
cat <<"RULES_JVM_EXTERNAL_EOF" | "$jq" --sort-keys --indent 4 . - > $maven_install_json_loc
{dependency_tree_json}
RULES_JVM_EXTERNAL_EOF

if [ "{predefined_maven_install}" = "True" ]; then
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
    maven_install_json = "@//:{repository_name}_install.json",
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
