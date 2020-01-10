# --- begin runfiles.bash initialization v2 ---
# Copy-pasted from the Bazel Bash runfiles library v2.
set -uo pipefail; f=bazel_tools/tools/bash/runfiles/runfiles.bash
source "${RUNFILES_DIR:-/dev/null}/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "${RUNFILES_MANIFEST_FILE:-/dev/null}" | cut -f2- -d' ')" 2>/dev/null || \
  source "$0.runfiles/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.exe.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  { echo>&2 "ERROR: cannot find $f"; exit 1; }; f=; set -e
# --- end runfiles.bash initialization v2 ---

set -euxo pipefail

readonly classes_txt=$(rlocation rules_jvm_external/tests/unit/jetifier/jetify_include_list_classes.txt)

# support-appcompat is in the jetify_include_list, we expect it to be jetified
grep "androidx/appcompat" "$classes_txt"
! grep "android/support/v7" "$classes_txt"

# swiperefreshlayout is not in the jetify_include_list, we do NOT expect it to be jetified
grep "android/support/v4/widget/SwipeRefreshLayout" "$classes_txt"
! grep "androidx/swiperefreshlayout/widget/SwipeRefreshLayout" "$classes_txt"
