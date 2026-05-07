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
# Workaround lack of rlocationpath, see comment on _BUILD_PIN in coursier.bzl
maven_unsorted_file=$(rlocation "${1#external\/}")
if [[ ! -e $maven_unsorted_file ]]; then
  # for --experimental_sibling_repository_layout
  maven_unsorted_file="${1#..\/}"
fi
if [[ ! -e $maven_unsorted_file ]]; then (echo >&2 "Failed to locate the unsorted_deps.json file: $1" && exit 1) fi
readonly maven_install_json_loc={maven_install_location}

cp "$maven_unsorted_file" "$maven_install_json_loc"

# When store_bom_resolution=True, the unpinned repo's pin sh_binary passes
# two extra positional args: $2 = rlocationpath of BomResolverMain, $3 =
# rlocationpath of the args file containing --boms / --repositories /
# --artifacts entries. Invoke BomResolverMain to add a `bom_resolution`
# section to the lock file in place. If $2 is absent, this is a noop.
if [[ -n "${2:-}" ]]; then
    bom_resolver_runfile=$(rlocation "${2#external\/}")
    if [[ ! -e "$bom_resolver_runfile" ]]; then
        bom_resolver_runfile="${2#..\/}"
    fi
    if [[ ! -e "$bom_resolver_runfile" ]]; then
        echo >&2 "Failed to locate the BomResolverMain runfile: $2"
        exit 1
    fi

    bom_args_file=$(rlocation "${3#external\/}")
    if [[ ! -e "$bom_args_file" ]]; then
        bom_args_file="${3#..\/}"
    fi
    if [[ ! -e "$bom_args_file" ]]; then
        echo >&2 "Failed to locate the BOM resolver args file: $3"
        exit 1
    fi

    # The BomResolverMain wrapper is a generated java_binary launcher. When
    # invoked directly from disk it tries to locate "$0.runfiles/" next to
    # itself, but $bom_resolver_runfile resolves to the canonical bazel-bin
    # location which has no sibling runfiles tree. Instead, point it at our
    # parent script's merged runfiles tree, which is where Bazel staged the
    # java_binary's data deps and classpath jars.
    if [[ -n "${RUNFILES_DIR:-}" ]]; then
        runfiles_root="$RUNFILES_DIR"
    elif [[ -n "${RUNFILES_MANIFEST_FILE:-}" ]]; then
        # Strip the _manifest suffix to get the runfiles directory.
        runfiles_root="${RUNFILES_MANIFEST_FILE%_manifest}"
    else
        runfiles_root="$0.runfiles"
    fi
    JAVA_RUNFILES="$runfiles_root" \
    RUNFILES_DIR="$runfiles_root" \
        "$bom_resolver_runfile" \
        --lock-file="$maven_install_json_loc" \
        "@$bom_args_file"
fi

if [ "{predefined_maven_install}" = "True" ]; then
    echo "Successfully pinned resolved artifacts for @{repository_name}, $maven_install_json_loc is now up-to-date."
else
    echo "Successfully pinned resolved artifacts for @{repository_name} in $maven_install_json_loc." \
      "This file should be checked into your version control system."
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
