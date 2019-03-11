#!/bin/bash

set -eux
set -o pipefail

readonly SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# try to run it once here to bootstrap
$SCRIPT_DIR/../../third_party/coursier/coursier

# tests for use_unsafe_shared_cache = True
bazel fetch @unsafe_shared_cache//:all

file $(bazel info output_base)/external/unsafe_shared_cache/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar | grep "symbolic link to" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = True: guava-27.0-jre.jar is not a symbolic link"

file $(bazel info output_base)/external/unsafe_shared_cache/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre-sources.jar | grep "symbolic link to" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = True: guava-27.0-jre-sources.jar is not a symbolic link"

# tests for use_unsafe_shared_cache = False
bazel fetch @other_maven//:all

file $(bazel info output_base)/external/other_maven/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar | grep "Java archive data (JAR)" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = False: guava-27.0-jre.jar is not a JAR"

file $(bazel info output_base)/external/other_maven/v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre-sources.jar | grep "Zip archive data" \
  || echo "Unsafe cache test failed for use_unsafe_shared_cache = False: guava-27.0-jre-sources.jar is not a Zip archive data"
