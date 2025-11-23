#!/bin/bash
set -e

# The genquery output file should contain the target label if it matches the visibility attribute.
# The file path is passed as the first argument or found in runfiles.

QUERY_OUTPUT=$(find . -name "verify_visibility_query")

if [ -z "$QUERY_OUTPUT" ]; then
  echo "Could not find query output file"
  exit 1
fi

CONTENT=$(cat "$QUERY_OUTPUT")

if [ -n "$CONTENT" ]; then
  echo "SUCCESS: Target has private visibility"
else
  echo "FAILURE: Target does not have private visibility. Content is empty."
  exit 1
fi
