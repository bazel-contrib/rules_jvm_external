#!/usr/bin/env bash

#IMPORTANT: Keep functionality in this Bat in sync with the win bat version (pin.bat)

# --- begin runfiles.bash initialization v3 ---
# Copy-pasted from the Bazel Bash runfiles library v3.
set -uo pipefail; set +e; f=bazel_tools/tools/bash/runfiles/runfiles.bash
source "${RUNFILES_DIR:-/dev/null}/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "${RUNFILES_MANIFEST_FILE:-/dev/null}" | cut -f2- -d' ')" 2>/dev/null || \
  source "$0.runfiles/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.exe.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  { echo>&2 "ERROR: cannot find $f"; exit 1; }; f=; set -e
# --- end runfiles.bash initialization v3 ---

set -euo pipefail
# Workaround lack of rlocationpath, see comment on _BUILD_PIN in coursier.bzl
maven_unsorted_file=$(rlocation "${1#external\/}")
if [[ ! -e $maven_unsorted_file ]]; then
  # for --experimental_sibling_repository_layout
  maven_unsorted_file="${1#..\/}"
fi
if [[ ! -e $maven_unsorted_file ]]; then (echo >&2 "Failed to locate the unsorted_deps.json file: $1" && exit 1) fi
readonly maven_install_json_loc=$BUILD_WORKSPACE_DIRECTORY/{maven_install_location}

cp "$maven_unsorted_file" "$maven_install_json_loc"

if [ "{predefined_maven_install}" = "True" ]; then
{success_msg_pinned}    
else
{success_msg_unpinned}
fi
echo
