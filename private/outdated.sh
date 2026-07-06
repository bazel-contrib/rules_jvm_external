#!/usr/bin/env bash

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

outdated_jar_path=$(rlocation "$1")
artifacts_file_path=$(rlocation "$2")
boms_file_path=$(rlocation "$3")
repositories_file_path=$(rlocation "$4")
extra_option_flag=${5:-}

java {proxy_opts} -jar "$outdated_jar_path" \
  --artifacts-file "$artifacts_file_path" \
  --boms-file "$boms_file_path" \
  --repositories-file "$repositories_file_path" \
  $extra_option_flag
