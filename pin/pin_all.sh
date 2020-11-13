#!/usr/bin/env bash
set -euo pipefail

(
    cd $BUILD_WORKING_DIRECTORY
    # TODO: use --ui_event_filters to filter DEBUG lines away when it's in a Bazel release.
    for R in $(bazel query --noshow_progress --noshow_loading_progress //external:all | grep unpinned);
    do
        name=$(echo $R | cut -d':' -f2)
        bazel run @$name//:pin
    done
)
