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

set -euox pipefail

deps_file=$(rlocation rules_jvm_external/tests/integration/override_targets/root_module_can_override)

function clean_up_workspace_names() {
  local file_name="$1"
  local target="$2"
  # The first `sed` command replaces `@@` with `@`. The second extracts the visible name
  # from the bzlmod mangled workspace name
  cat "$file_name" | sed -e 's|^@@|@|g; s|\r||g' | sed -e 's|^@[^/]*[+~]|@|g; s|\r||g' | grep "$target"
  cat "$file_name" | sed -e 's|^@@|@|g; s|\r||g' | sed -e 's|^@[^/]*[+~]|@|g; s|\r||g' | grep -q "$target"
}

if ! clean_up_workspace_names "$deps_file" "@root_module_can_override//:com_squareup_okhttp3_okhttp"; then
  exit 1
fi

if ! clean_up_workspace_names "$deps_file" "@root_module_can_override//:com_squareup_javapoet"; then
  exit 1
fi
