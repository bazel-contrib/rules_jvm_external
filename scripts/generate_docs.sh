#!/usr/bin/env bash

# Prerequisites:
#   - https://github.com/thlorenz/doctoc - install with `npm -g install doctoc`

set -euo pipefail

bazel build :generate_api_reference && \
    cp bazel-bin/api.md docs/api.md && \
    chmod u+rw docs/api.md && \
    chmod a-x docs/api.md && \
    doctoc --title '# API Reference' docs/api.md
