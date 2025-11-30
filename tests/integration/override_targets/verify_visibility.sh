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

# The query should return the target itself
if [ -n "$CONTENT" ] && [[ "$CONTENT" == *"com_squareup_okio_okio"* ]]; then
  echo "SUCCESS: Target exists and has custom visibility set"
else
  echo "FAILURE: Target not found or visibility not properly set. Content: $CONTENT"
  exit 1
fi
