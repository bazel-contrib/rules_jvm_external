#!/bin/bash -e

# A simple test framework to verify bazel output without setting up an entire WORKSPACE
# in the bazel sandbox as is done in https://github.com/bazelbuild/bazel/blob/master/src/test/shell/unittest.bash
#
# Add a new test to the TESTS array and send all output to TEST_LOG

function test_dependency_aggregation() {
  bazel query --notool_deps 'deps(@regression_testing//:com_sun_xml_bind_jaxb_ri)' >> "$TEST_LOG" 2>&1

  # This is a transitive dep of @regression_testing//:com_sun_xml_bind_jaxb_ri
  expect_log @regression_testing//:com_sun_xml_bind_jaxb_xjc
}

function test_duplicate_version_warning() {
  bazel run @duplicate_version_warning//:pin >> "$TEST_LOG" 2>&1
  rm -f duplicate_version_warning_install.json

  expect_log "Found duplicate artifact versions"
  expect_log "com.fasterxml.jackson.core:jackson-annotations has multiple versions"
  expect_log "com.github.jnr:jffi:native has multiple versions"
  expect_log "Successfully pinned resolved artifacts"
}

function test_m2local_testing_ignore_file_if_empty() {
  m2local_dir="${HOME}/.m2/repository"
  jar_dir="${m2local_dir}/com/example/kt/1.0.0"
  rm -rf ${jar_dir}
  mkdir -p ${m2local_dir}
  bazel clean --expunge >> "$TEST_LOG" 2>&1 # for https://github.com/bazelbuild/rules_jvm_external/issues/800
  # Publish a maven artifact locally - com.example.kt:1.0.0
  bazel run --define maven_repo="file://${m2local_dir}" //tests/integration/kt_jvm_export:test.publish >> "$TEST_LOG" 2>&1

  # Imitate an empty sources jar.
  rm -rf ${jar_dir}/kt-1.0.0-sources.jar*
  touch ${jar_dir}/kt-1.0.0-sources.jar
  echo "da39a3ee5e6b4b0d3255bfef95601890afd80709" > ${jar_dir}/kt-1.0.0-sources.jar.sha1

  bazel run @m2local_testing_fetch_sources//:pin >> "$TEST_LOG" 2>&1
  expect_not_in_file '"sources": "' m2local_testing_fetch_sources_install.json

  rm -f m2local_testing_fetch_sources_install.json
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
  expect_log "Successfully pinned resolved artifacts"
}

function test_unpinned_m2local_testing_ignore_file_if_empty() {
  m2local_dir="${HOME}/.m2/repository"
  jar_dir="${m2local_dir}/com/example/kt/1.0.0"
  rm -rf ${jar_dir}
  mkdir -p ${m2local_dir}
  bazel clean --expunge >> "$TEST_LOG" 2>&1 # for https://github.com/bazelbuild/rules_jvm_external/issues/800
  # Publish a maven artifact locally - com.example.kt:1.0.0
  bazel run --define maven_repo="file://${m2local_dir}" //tests/integration/kt_jvm_export:test.publish >> "$TEST_LOG" 2>&1

  # Imitate an empty sources jar.
  rm -rf ${jar_dir}/kt-1.0.0-sources.jar*
  touch ${jar_dir}/kt-1.0.0-sources.jar
  echo "da39a3ee5e6b4b0d3255bfef95601890afd80709" > ${jar_dir}/kt-1.0.0-sources.jar.sha1

  bazel run @unpinned_m2local_testing_fetch_sources_repin//:pin >> "$TEST_LOG" 2>&1
  expect_not_in_file '"sources": "' tests/custom_maven_install/m2local_testing_fetch_sources_with_pinned_file_install.json

  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
  expect_log "Successfully pinned resolved artifacts"
}

function test_m2local_testing_found_local_artifact_through_pin_and_build() {
  m2local_dir="${HOME}/.m2/repository"
  jar_dir="${m2local_dir}/com/example/kt/1.0.0"
  rm -rf ${jar_dir}
  mkdir -p ${m2local_dir}
  bazel clean --expunge >> "$TEST_LOG" 2>&1 # for https://github.com/bazelbuild/rules_jvm_external/issues/800
  # Publish a maven artifact locally - com.example.kt:1.0.0
  bazel run --define maven_repo="file://${m2local_dir}" //tests/integration/kt_jvm_export:test.publish >> "$TEST_LOG" 2>&1
  bazel run @m2local_testing//:pin >> "$TEST_LOG" 2>&1
  bazel build @m2local_testing//:com_example_kt >> "$TEST_LOG" 2>&1
  rm -f m2local_testing_install.json
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
  expect_log "Successfully pinned resolved artifacts"
}

function test_unpinned_m2local_testing_found_local_artifact_through_pin_and_build() {
  m2local_dir="~/.m2/repository"
  mkdir -p ${m2local_dir}
  # Publish a maven artifact locally - com.example.kt:1.0.0
  bazel run --define maven_repo="file://${HOME}/.m2/repository" //tests/integration/kt_jvm_export:test.publish 
  bazel run @unpinned_m2local_testing_repin//:pin >> "$TEST_LOG" 2>&1
  bazel build @m2local_testing_repin//:com_example_kt >> "$TEST_LOG" 2>&1
  rm -f m2local_testing_install.json
  rm -rf ~/.m2/repository

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
  expect_log "Successfully pinned resolved artifacts"
}

function test_m2local_testing_found_local_artifact_through_build() {
  m2local_dir="${HOME}/.m2/repository"
  jar_dir="${m2local_dir}/com/example/kt/1.0.0"
  rm -rf ${jar_dir}
  mkdir -p ${m2local_dir}
  bazel clean --expunge >> "$TEST_LOG" 2>&1 # for https://github.com/bazelbuild/rules_jvm_external/issues/800
  # Publish a maven artifact locally - com.example.kt:1.0.0
  bazel run --define maven_repo="file://${m2local_dir}" //tests/integration/kt_jvm_export:test.publish >> "$TEST_LOG" 2>&1
  bazel build @m2local_testing//:com_example_kt >> "$TEST_LOG" 2>&1
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
}

function test_m2local_testing_found_local_artifact_after_build_copy() {
  jar_dir="${HOME}/.m2/repository/com/example/kt/1.0.0"
  mkdir -p "${jar_dir}"

  # We need to copy from binaries to local maven repo to appropriate paths
  # this is mapping of the two, since bash 3 doesn't support maps
  maven_files_to_copy=(
    "test-docs.jar:kt-1.0.0-javadoc.jar"
    "test-lib.jar:kt-1.0.0.jar"
    "test-lib-sources.jar:kt-1.0.0-sources.jar"
    "test-pom.xml:kt-1.0.0.pom"
  )

  bazel build //tests/integration/kt_jvm_export:test.publish >> "$TEST_LOG" 2>&1
  # Populate m2local from bazel-bin
  for file_map in "${maven_files_to_copy[@]}"; do
    source="${file_map%%:*}"
    dest="${file_map##*:}"
    cp "bazel-bin/tests/integration/kt_jvm_export/$source" "${jar_dir}/${dest}"
  done

  # Clear cache for fresh re-build
  bazel clean --expunge >> "$TEST_LOG" 2>&1
  bazel build @m2local_testing_without_checksum//:com_example_kt >> "$TEST_LOG" 2>&1
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
}

function test_duplicate_version_warning_same_version() {
  bazel run @duplicate_version_warning_same_version//:pin >> "$TEST_LOG" 2>&1
  rm -f duplicate_version_warning_same_version_install.json

  expect_not_log "Found duplicate artifact versions"
  expect_not_log "com.fasterxml.jackson.core:jackson-annotations has multiple versions"
  expect_not_log "com.github.jnr:jffi:native has multiple versions"
  expect_log "Successfully pinned resolved artifacts"
}

function test_outdated() {
  bazel run @regression_testing//:outdated >> "$TEST_LOG" 2>&1

  expect_log "Checking for updates of .* artifacts against the following repositories"
  expect_log "junit:junit \[4.12"
}

function test_outdated_no_external_runfiles() {
  bazel run @regression_testing//:outdated --nolegacy_external_runfiles >> "$TEST_LOG" 2>&1

  expect_log "Checking for updates of .* artifacts against the following repositories"
  expect_log "junit:junit \[4.12"
}

function test_v1_lock_file_format() {
  # Because we run with `-e` this command succeeding is enough to
  # know that the v1 lock file format was parsed successfully
  bazel build @v1_lock_file_format//:io_ous_jtoml >> "$TEST_LOG" 2>&1
}

function test_dependency_pom_exclusion() {
  bazel query --notool_deps 'deps(@regression_testing//:org_mockito_mockito_core)' >> "$TEST_LOG" 2>&1

  # byte-buddy should be a dependency of "mockito-core" even though "androidx.arch.core:core-testing" has exclusion rule for it in POM
  expect_log "@regression_testing//:net_bytebuddy_byte_buddy"
}

TESTS=(
  "test_dependency_aggregation"
  "test_duplicate_version_warning"
  "test_duplicate_version_warning_same_version"
  "test_outdated"
  "test_outdated_no_external_runfiles"
  "test_m2local_testing_found_local_artifact_through_pin_and_build"
  "test_unpinned_m2local_testing_found_local_artifact_through_pin_and_build"
  "test_m2local_testing_found_local_artifact_through_build"
  "test_m2local_testing_found_local_artifact_after_build_copy"
  "test_m2local_testing_ignore_file_if_empty"
  "test_unpinned_m2local_testing_ignore_file_if_empty"
  "test_v1_lock_file_format"
  "test_dependency_pom_exclusion"
)

function run_tests() {
  printf "Running bazel run tests:\n"
  for test in ${TESTS[@]}; do
    printf "  ${test} "
    TEST_LOG=/tmp/${test}_test.log
    rm -f "$TEST_LOG"
    DUMPED_TEST_LOG=0
    ${test}
    printf "PASSED\n"
    rm -f "$TEST_LOG"
  done
}

function expect_not_in_file() {
  local pattern=$1
  local file=$2
  local message=${3:-Expected not to find regexp \""$pattern"\", but it was found}
  grep -sq -- "$pattern" $file || return 0

  printf "FAILED\n"
  cat $file
  printf "FAILURE: $message\n"
  return 1
}

function expect_log() {
  local pattern=$1
  local message=${2:-Expected regexp \""$pattern"\" not found}
  grep -sq -- "$pattern" $TEST_LOG && return 0

  printf "FAILED\n"
  cat $TEST_LOG
  DUMPED_TEST_LOG=1
  printf "FAILURE: $message\n"
  return 1
}

function expect_not_log() {
  local pattern=$1
  local message=${2:-Expected not to find regexp \""$pattern"\", but it was found}
  grep -sq -- "$pattern" $TEST_LOG || return 0

  printf "FAILED\n"
  cat $TEST_LOG
  DUMPED_TEST_LOG=1
  printf "FAILURE: $message\n"
  return 1
}

function exit_handler() {
  local exit_code=$?
  if [ $exit_code != "0" ] && [ $DUMPED_TEST_LOG == "0" ]; then
    printf "ERROR\n"
    cat $TEST_LOG
  fi
  return $exit_code
}

trap exit_handler EXIT

run_tests
