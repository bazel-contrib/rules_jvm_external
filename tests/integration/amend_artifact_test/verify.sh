#!/bin/bash
set -e
GUAVA_OUTPUT="$1"
LANG3_OUTPUT="$2"
IO_OUTPUT="$3"
SLF4J_OUTPUT="$4"

# 1. Guava (Unset via chaining): Should be empty (testonly=0)
if [ -s "$GUAVA_OUTPUT" ]; then
  echo "Error: Guava output is NOT empty. 'testonly' was not unset."
  cat "$GUAVA_OUTPUT"
  exit 1
fi

# 2. Lang3 (Set via Int): Should be non-empty (testonly=1)
if [ ! -s "$LANG3_OUTPUT" ]; then
  echo "Error: Lang3 output is empty. 'testonly_int=1' failed."
  exit 1
fi

# 3. IO (Set via Bool): Should be non-empty (testonly=1)
if [ ! -s "$IO_OUTPUT" ]; then
  echo "Error: Commons-IO output is empty. 'testonly=True' failed."
  exit 1
fi

# 4. Slf4j (Neverlink via Int): Should be non-empty (neverlink=1)
if [ ! -s "$SLF4J_OUTPUT" ]; then
  echo "Error: Slf4j output is empty. 'neverlink_int=1' failed."
  exit 1
fi

echo "All verification checks passed!"

