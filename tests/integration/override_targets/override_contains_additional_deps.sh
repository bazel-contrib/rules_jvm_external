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

deps_file=$(rlocation rules_jvm_external/tests/integration/override_targets/trace_otel_deps)

# we should contain the original target
if ! grep -q "@override_target_in_deps//:io_opentelemetry_opentelemetry_sdk" $deps_file; then
  echo "Unable to find SDK target"
  exit 1
fi

# should contain the "raw" dep
if ! grep -q "@override_target_in_deps//:original_io_opentelemetry_opentelemetry_api" $deps_file; then
  echo "Unable to find raw API target"
  exit 1
fi

# the "context" dependency is depended upon by `io.opentelemetry:opentelemetry-api` and
# nothing else in the SDK. If we have built the raw dependency properly, this should
# also be present in the dependencies
if ! grep -q "@override_target_in_deps//:io_opentelemetry_opentelemetry_context" $deps_file; then
  echo "Unable to find transitive dep of raw target"
  exit 1
fi

# Finally, we expect jedis (which is not an OTel dep) to have also been added
if ! grep -q "@override_target_in_deps//:redis_clients_jedis" $deps_file; then
  echo "Unable to find additional target added to a transitive dep of the SDK"
  exit 1
fi
