#!/bin/bash

set -euo pipefail
readonly maven_install_json_loc=$BUILD_WORKSPACE_DIRECTORY/maven_install.json
echo {dependency_tree_json} | python -m json.tool > $maven_install_json_loc
echo "Pinned resolved artifacts for @{repository_name} in $maven_install_json_loc." \
  "This file should be checked in your version control system."
