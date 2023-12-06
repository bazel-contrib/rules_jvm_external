#!/usr/bin/env bash
set -euo pipefail

# We expect the deps to contain guava at version 23.3-jre
cat $1 | grep guava | grep 23.3-jre
