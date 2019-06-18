#!/bin/bash

set -euo pipefail
readonly maven_install_json_loc=$BUILD_WORKSPACE_DIRECTORY/maven_install.json
echo {dependency_tree_json} | python -m json.tool > $maven_install_json_loc
echo "Pinned resolved artifacts for @{repository_name} in $maven_install_json_loc." \
  "This file should be checked in your version control system." \
  "Next, please add the following snippet to your WORKSPACE file if you have not" \
  "done so."
cat <<EOF
---

load("@{repository_name}//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

---
EOF
