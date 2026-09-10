#!/usr/bin/env bash

set -euo pipefail

workspace="${BUILD_WORKSPACE_DIRECTORY:?Run this target with bazel run}"

write_doc() {
  awk '
    { lines[NR] = $0 }
    END {
      last = NR
      while (last > 0 && lines[last] == "") {
        last--
      }
      for (i = 1; i <= last; i++) {
        print lines[i]
      }
    }
  ' "$1" > "$2"
}

while (( $# > 0 )); do
  source_file="$1"
  destination="$2"
  write_doc "$source_file" "$workspace/docs/$destination"
  chmod u+rw,a-x "$workspace/docs/$destination"
  shift 2
done
