#!/usr/bin/env bash

set -uo pipefail

status=0
count=0

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
  name="$1"
  checked_in="$2"
  generated="$3"
  normalized="$TEST_TMPDIR/generated-$count.md"

  write_doc "$generated" "$normalized"

  if ! diff -u "$checked_in" "$normalized"; then
    echo "$name is out of date. Run: bazel run //docs:update" >&2
    status=1
  fi

  shift 3
  count=$((count + 1))
done

exit "$status"
