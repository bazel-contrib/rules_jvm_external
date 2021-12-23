#!/bin/bash -e

# A simple test framework to verify bazel output without setting up an entire WORKSPACE
# in the bazel sandbox as is done in https://github.com/bazelbuild/bazel/blob/master/src/test/shell/unittest.bash
#
# Add a new test to the TESTS array and send all output to TEST_LOG

function test_duplicate_version_warning() {
  bazel run @duplicate_version_warning//:pin >> "$TEST_LOG" 2>&1
  rm -f duplicate_version_warning_install.json

  expect_log "Found duplicate artifact versions"
  expect_log "com.fasterxml.jackson.core:jackson-annotations has multiple versions"
  expect_log "com.github.jnr:jffi:native has multiple versions"
  expect_log "Successfully pinned resolved artifacts"
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

test_xdg_cache_home() {
  readonly cachedir=/tmp/${test}-cache
  XDG_CACHE_HOME=$cachedir bazel run @unsafe_shared_cache//:pin >> "$TEST_LOG" 2>&1
  rm -rf $cachedir
  expect_log "Successfully pinned resolved artifacts"
}

TESTS=(
  "test_duplicate_version_warning"
  "test_duplicate_version_warning_same_version"
  "test_outdated"
  "test_outdated_no_external_runfiles"
  "test_xdg_cache_home"
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
