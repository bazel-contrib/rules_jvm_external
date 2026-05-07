#!/bin/bash -e

# A simple test framework to verify bazel output without setting up an entire WORKSPACE
# in the bazel sandbox as is done in https://github.com/bazelbuild/bazel/blob/master/src/test/shell/unittest.bash
#
# Add a new test to the TESTS array and send all output to TEST_LOG

function force_bzlmod_lock_file_to_be_regenerated() {
  # The newly deployed jar won't be in the bazel module lock file, so force
  # that to be regenerated in a way that works with pre-bzlmod versions of
  # Bazel
  rm -f MODULE.bazel.lock
}

function test_dependency_aggregation() {
  bazel query --notool_deps 'deps(@regression_testing_coursier//:com_sun_xml_bind_jaxb_ri)' >> "$TEST_LOG" 2>&1

  # This is a transitive dep of @regression_testing_coursier//:com_sun_xml_bind_jaxb_ri
  expect_log @regression_testing_coursier//:com_sun_xml_bind_jaxb_xjc
}

function test_duplicate_version_warning() {
  bazel run @duplicate_version_warning//:pin >> "$TEST_LOG" 2>&1
  rm -f *duplicate_version_warning_install.json

  expect_log "Found duplicate artifact versions"
  expect_log "com.fasterxml.jackson.core:jackson-annotations has multiple versions"
  expect_log "com.github.jnr:jffi:native has multiple versions"
  expect_log "Successfully pinned resolved artifacts"
}

function test_duplicate_version_warning_same_version() {
  bazel run @duplicate_version_warning_same_version//:pin >> "$TEST_LOG" 2>&1
  rm -f *duplicate_version_warning_same_version_install.json

  expect_not_log "Found duplicate artifact versions"
  expect_not_log "com.fasterxml.jackson.core:jackson-annotations has multiple versions"
  expect_not_log "com.github.jnr:jffi:native has multiple versions"
  expect_log "Successfully pinned resolved artifacts"
}

function test_m2local_testing_ignore_empty_files() {
  # Testing ignore_empty_files with m2local, as it's the easiest way to imitate an empty jar file.
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

  force_bzlmod_lock_file_to_be_regenerated

  bazel run @m2local_testing_ignore_empty_files//:pin >> "$TEST_LOG" 2>&1
  expect_not_in_file '"sources": "' *m2local_testing_ignore_empty_files_install.json

  rm -f *m2local_testing_ignore_empty_files_install.json
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
  expect_log "Successfully pinned resolved artifacts"
}

function test_unpinned_m2local_testing_ignore_empty_files() {
  # Testing ignore_empty_files with m2local, as it's the easiest way to imitate an empty jar file.
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

  force_bzlmod_lock_file_to_be_regenerated

  bazel run @unpinned_m2local_testing_ignore_empty_files_repin//:pin >> "$TEST_LOG" 2>&1
  expect_not_in_file '"sources": "' tests/custom_maven_install/m2local_testing_ignore_empty_files_with_pinned_file_install.json

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

  force_bzlmod_lock_file_to_be_regenerated

  bazel build @m2local_testing//:com_example_kt >> "$TEST_LOG" 2>&1
  rm -f *m2local_testing_install.json
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
  expect_log "Successfully pinned resolved artifacts"
}

function test_unpinned_m2local_testing_found_local_artifact_through_pin_and_build() {
  m2local_dir="${HOME}/.m2/repository"
  jar_dir="${m2local_dir}/com/example/kt/1.0.0"
  rm -rf ${jar_dir}
  mkdir -p ${m2local_dir}
  # Publish a maven artifact locally - com.example.kt:1.0.0
  bazel run --define maven_repo="file://${m2local_dir}" //tests/integration/java_export:without-docs.publish >> "$TEST_LOG" 2>&1

  # Force the repo rule to be evaluated again. Without this, the "assuming maven local..." message will not be printed
  bazel clean --expunge >/dev/null 2>&1

  bazel run @unpinned_m2local_testing_repin//:pin >> "$TEST_LOG" 2>&1

  force_bzlmod_lock_file_to_be_regenerated

  bazel build @m2local_testing_repin//:com_example_no_docs >> "$TEST_LOG" 2>&1
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:no-docs:1.0.0"
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

  force_bzlmod_lock_file_to_be_regenerated

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
    "test-project.jar:kt-1.0.0.jar"
    "test-project-src.jar:kt-1.0.0-sources.jar"
    "test-pom.xml:kt-1.0.0.pom"
  )

  bazel build //tests/integration/kt_jvm_export:test.publish >> "$TEST_LOG" 2>&1
  # Populate m2local from bazel-bin
  for file_map in "${maven_files_to_copy[@]}"; do
    source="${file_map%%:*}"
    dest="${file_map##*:}"
    cp -f "bazel-bin/tests/integration/kt_jvm_export/$source" "${jar_dir}/${dest}"
  done

  # Clear cache for fresh re-build
  bazel clean --expunge >> "$TEST_LOG" 2>&1

  force_bzlmod_lock_file_to_be_regenerated

  bazel build @m2local_testing_without_checksum//:com_example_kt >> "$TEST_LOG" 2>&1
  rm -rf ${jar_dir}

  expect_log "Assuming maven local for artifact: com.example:kt:1.0.0"
}

function test_publish_java_binary_jar_with_maven_export {
  m2local_dir="${HOME}/.m2/repository"
  jar_dir="${m2local_dir}/com/example_binary/exe/1.0.0"
  rm -rf ${jar_dir}
  mkdir -p ${m2local_dir}

  # should run successfully
  bazel run --define maven_repo="file://${m2local_dir}" //tests/integration/java_export:some-java-binary-export.publish >> "$TEST_LOG" 2>&1

  rm -rf ${jar_dir}
}

function test_found_artifact_with_plus_through_pin_and_build() {
  bazel clean --expunge >> "$TEST_LOG" 2>&1 # for https://github.com/bazelbuild/rules_jvm_external/issues/800
  bazel run @artifact_with_plus//:pin >> "$TEST_LOG" 2>&1

  force_bzlmod_lock_file_to_be_regenerated

  bazel build @artifact_with_plus//:ch_epfl_scala_compiler_interface >> "$TEST_LOG" 2>&1
  rm -f *artifact_with_plus_install.json

  expect_log "Successfully pinned resolved artifacts"
}

function test_unpinned_found_artifact_with_plus_through_pin_and_build() {
  # Force the repo rule to be evaluated again. Without this, the "assuming maven local..." message will not be printed
  bazel clean --expunge >/dev/null 2>&1

  bazel run @unpinned_artifact_with_plus_repin//:pin >> "$TEST_LOG" 2>&1

  force_bzlmod_lock_file_to_be_regenerated

  bazel build @artifact_with_plus_repin//:ch_epfl_scala_compiler_interface >> "$TEST_LOG" 2>&1

  expect_log "Successfully pinned resolved artifacts"
}

function test_outdated() {
  bazel run @regression_testing_coursier//:outdated >> "$TEST_LOG" 2>&1

  expect_log "Checking for updates of .* artifacts against the following repositories"
  expect_log "junit:junit \[4.12"
}

function test_outdated_no_external_runfiles() {
  bazel run @regression_testing_coursier//:outdated --nolegacy_external_runfiles >> "$TEST_LOG" 2>&1

  expect_log "Checking for updates of .* artifacts against the following repositories"
  expect_log "junit:junit \[4.12"
}

function test_outdated_with_boms() {
  bazel run @regression_testing_maven//:outdated >> "$TEST_LOG" 2>&1

  expect_log "Checking for updates of .* boms and .* artifacts against the following repositories"
  expect_log "io.opentelemetry:opentelemetry-bom \[1.31.0"
}

function test_outdated_with_boms_does_not_include_artifacts_without_a_version() {
  bazel run @coursier_resolved_with_boms//:outdated >> "$TEST_LOG" 2>&1

  expect_log "Checking for updates of .* boms and .* artifacts against the following repositories"
  expect_log "com.google.cloud:libraries-bom \[26.59.0"
  expect_not_log "com.google.cloud:google-cloud-bigquery"
  expect_not_log "\[None"
}

# ---- BOM resolution feature tests --------------------------------------------------------
#
# These tests follow the anti-faking guardrails from the spec:
#   1. Reset to `{}` before pinning so stale data can't produce a false positive.
#   2. Use jq for exact assertions (not grep on the section name).
#   3. Assert minimum-N counts and exact mappings (not "section non-empty").
#   4. Verify both presence (enabled) AND absence (disabled) of the section + marker.
#   5. Hard-fail tests assert non-zero exit AND lock file unchanged.

function test_bom_resolution_coursier_with_boms() {
  local lock_file="tests/custom_maven_install/bom_resolution_coursier_install.json"
  echo '{}' > "$lock_file"

  REPIN=1 bazel run @unpinned_bom_resolution_coursier//:pin >> "$TEST_LOG" 2>&1

  expect_log "Successfully pinned resolved artifacts"
  # Exact: bom_resolution must contain at least both versionless artifacts.
  local count
  count=$(jq '.bom_resolution | length' "$lock_file")
  if (( count < 2 )); then
    printf "FAILED: expected bom_resolution to have >= 2 entries, got %s\n" "$count"
    cat "$lock_file"
    return 1
  fi
  # Both expected mappings must point at the junit-bom.
  local api_bom
  api_bom=$(jq -r '.bom_resolution["org.junit.jupiter:junit-jupiter-api"][0]' "$lock_file")
  if [[ "$api_bom" != "org.junit:junit-bom:5.10.0" ]]; then
    printf "FAILED: expected junit-bom to manage junit-jupiter-api, got '%s'\n" "$api_bom"
    return 1
  fi
}

function test_bom_resolution_maven_with_boms() {
  local lock_file="tests/custom_maven_install/bom_resolution_maven_install.json"
  echo '{}' > "$lock_file"

  bazel run @bom_resolution_maven//:pin >> "$TEST_LOG" 2>&1

  local count
  count=$(jq '.bom_resolution | length' "$lock_file")
  if (( count < 2 )); then
    printf "FAILED: expected maven flow bom_resolution to have >= 2 entries, got %s\n" "$count"
    cat "$lock_file"
    return 1
  fi
  local api_bom
  api_bom=$(jq -r '.bom_resolution["org.junit.jupiter:junit-jupiter-api"][0]' "$lock_file")
  if [[ "$api_bom" != "org.junit:junit-bom:5.10.0" ]]; then
    printf "FAILED: expected junit-bom (maven flow), got '%s'\n" "$api_bom"
    return 1
  fi
}

function test_bom_resolution_disabled_no_section_no_marker() {
  local lock_file="tests/custom_maven_install/bom_resolution_disabled_install.json"
  echo '{}' > "$lock_file"

  REPIN=1 bazel run @unpinned_bom_resolution_disabled//:pin >> "$TEST_LOG" 2>&1

  # Disabled => bom_resolution section MUST be absent (anti-faking #4).
  local has_section
  has_section=$(jq 'has("bom_resolution")' "$lock_file")
  if [[ "$has_section" != "false" ]]; then
    printf "FAILED: bom_resolution section unexpectedly present in disabled lock file\n"
    cat "$lock_file"
    return 1
  fi
  # Asymmetric encoding: marker MUST be absent from __INPUT_ARTIFACTS_HASH.
  local has_marker
  has_marker=$(jq '.__INPUT_ARTIFACTS_HASH | has("bom_resolution_enabled")' "$lock_file")
  if [[ "$has_marker" != "false" ]]; then
    printf "FAILED: bom_resolution_enabled marker unexpectedly present in disabled __INPUT_ARTIFACTS_HASH\n"
    cat "$lock_file"
    return 1
  fi
}

function test_bom_resolution_jvm_import_tags() {
  local lock_file="tests/custom_maven_install/bom_resolution_coursier_install.json"
  if [[ ! -s "$lock_file" ]] || [[ "$(cat "$lock_file")" == "{}" ]]; then
    REPIN=1 bazel run @unpinned_bom_resolution_coursier//:pin >> "$TEST_LOG" 2>&1
  fi

  # Inspect the generated jvm_import to confirm a maven_bom_coordinate= tag is emitted.
  local build_output
  build_output=$(bazel query --output=build @bom_resolution_coursier//:org_junit_jupiter_junit_jupiter_api 2>>"$TEST_LOG")
  if ! echo "$build_output" | grep -q 'maven_bom_coordinate=org.junit:junit-bom:5.10.0'; then
    printf "FAILED: generated jvm_import is missing maven_bom_coordinate tag\n"
    echo "$build_output" | head -40
    return 1
  fi
}

function test_bom_resolution_unresolvable_bom_aborts_pin() {
  local lock_file="tests/custom_maven_install/bom_resolution_coursier_install.json"
  # Pre-populate with a known-good lock file so we can verify it isn't modified
  # by the failed pin (anti-faking guardrail #7).
  REPIN=1 bazel run @unpinned_bom_resolution_coursier//:pin >> "$TEST_LOG" 2>&1
  local before_hash
  before_hash=$(shasum "$lock_file" | cut -d ' ' -f 1)

  # Construct an args file with a bogus BOM coordinate and run BomResolverMain
  # directly. This is the moral equivalent of declaring a non-existent BOM in
  # maven.install — but doesn't require a separate test repo.
  local tmp_args
  tmp_args=$(mktemp)
  cat > "$tmp_args" <<EOF
--lock-file=$(pwd)/$lock_file
--boms=com.example:does-not-exist:0.0.1
--repositories=https://repo1.maven.org/maven2
--artifacts=org.junit.jupiter:junit-jupiter-api
EOF

  set +e
  bazel run //private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/bom:BomResolverMain -- "@$tmp_args" >> "$TEST_LOG" 2>&1
  local exit_code=$?
  set -e
  rm -f "$tmp_args"

  if [[ $exit_code -eq 0 ]]; then
    printf "FAILED: BomResolverMain unexpectedly succeeded with bogus BOM\n"
    return 1
  fi

  local after_hash
  after_hash=$(shasum "$lock_file" | cut -d ' ' -f 1)
  if [[ "$before_hash" != "$after_hash" ]]; then
    printf "FAILED: lock file was modified despite hard-fail\n"
    return 1
  fi
}

function test_coursier_resolution_with_boms() {
    # Only run for Bazel 7 or above
    RELEASE="$(bazel info release | sed -e 's/release //' | cut -d '.' -f 1)"
    # Bail if we can't figure out the release
    if [[ -z "$RELEASE" ]]; then
      echo "$RELEASE is a zero-length string"
      return
    fi
    if ! [[ "$RELEASE" =~ ^[0-9]+$ ]]; then
      return
    fi

    # should run successfully
    REPIN=1 bazel run @coursier_resolved_with_boms//:pin >> "$TEST_LOG" 2>&1

    expect_log "Successfully pinned resolved artifacts"

    expect_file_is_not_empty "tests/custom_maven_install/coursier_resolved_install.json"

    # TODO: Add back in once coursier supports index files
    # expect_file_is_not_empty "tests/custom_maven_install/coursier_resolved_index.json"
}

function test_v1_lock_file_format() {
  # Because we run with `-e` this command succeeding is enough to
  # know that the v1 lock file format was parsed successfully
  bazel build @v1_lock_file_format//:io_ous_jtoml >> "$TEST_LOG" 2>&1
}

function test_dependency_pom_exclusion() {
  bazel query --notool_deps 'deps(@regression_testing_coursier//:org_mockito_mockito_core)' >> "$TEST_LOG" 2>&1

  # byte-buddy should be a dependency of "mockito-core" even though "androidx.arch.core:core-testing" has exclusion rule for it in POM
  expect_log "@regression_testing_coursier//:net_bytebuddy_byte_buddy"
}

function test_maven_resolution() {
    # Only run for Bazel 7 or above
    RELEASE="$(bazel info release | sed -e 's/release //' | cut -d '.' -f 1)"
    # Bail if we can't figure out the release
    if [[ -z "$RELEASE" ]]; then
      echo "$RELEASE is a zero-length string"
      return
    fi
    if ! [[ "$RELEASE" =~ ^[0-9]+$ ]]; then
      return
    fi

    # should run successfully
    bazel run @maven_resolved_with_boms//:pin >> "$TEST_LOG" 2>&1

    expect_file_is_not_empty "tests/custom_maven_install/maven_resolved_install.json"
    expect_file_is_not_empty "tests/custom_maven_install/maven_resolved_index.json"
}

function test_transitive_dependency_with_type_of_pom {
  # transitive_dependency_with_type_of_pom installs an artifact which depends on
  # org.javamoney:moneta:pom, which should expand into the transitive
  # dependencies of that type=pom artifact, such as
  # org.javamoney.moneta:moneta-core
  bazel query @transitive_dependency_with_type_of_pom//:org_javamoney_moneta_moneta_core >> "$TEST_LOG" 2>&1
}

function test_when_both_pom_and_jar_artifact_are_available_jar_artifact_is_present {
  # The `maven_coordinates` of the target should be set to the coordinates of the jar
  # If the `pom` classifier is asked for, something has gone wrong and no results will
  # match
  bazel query 'attr(tags, "com.github.spotbugs:spotbugs:4.7.0", @regression_testing_coursier//:com_github_spotbugs_spotbugs)' >> "$TEST_LOG" 2>&1

  expect_log "@regression_testing_coursier//:com_github_spotbugs_spotbugs"
}

function test_when_both_pom_and_jar_artifact_are_dependencies_jar_artifact_is_present {
  # The `maven_coordinates` of the target should be set to the coordinates of the jar
  # If both the `jar` and `pom` classifiers are asked for, something has gone wrong and no results
  # will match
  bazel query 'attr(tags, "org.mockito:mockito-core:3.3.3", @regression_testing_coursier//:org_mockito_mockito_core)' >> "$TEST_LOG" 2>&1

  expect_log "@regression_testing_coursier//:org_mockito_mockito_core"
}

 function test_gradle_metadata_is_resolved_correctly_for_aar_artifact {
    # This artifact in maven_install only has gradle metadata, but it should then automatically resolve to the right aar artifact
    # and make it available
    bazel query @regression_testing_gradle//:androidx_compose_foundation_foundation_layout_android >> "$TEST_LOG" 2>&1

    expect_log "@regression_testing_gradle//:androidx_compose_foundation_foundation_layout_android"

    expect_file_is_not_empty "tests/custom_maven_install/regression_testing_gradle_install.json"
    expect_file_is_not_empty "tests/custom_maven_install/regression_testing_gradle_index.json"
 }

function test_gradle_metadata_is_resolved_correctly_for_jvm_artifact {
  # This artifact in maven_install only has gradle metadata, but it should then automatically resolve to the right jvm artifact
  # and make it available
  bazel query @regression_testing_gradle//:androidx_annotation_annotation_jvm >> "$TEST_LOG" 2>&1

  expect_log "@regression_testing_gradle//:androidx_annotation_annotation_jvm"

  # This is KMP artifact which is a transitive dependency
  # and the JAR for this coordinate will just be a dummy jar/placeholder (in some cases a klib file)
  # as gradle will use metadata to resolve the right one.
  # Regardless we'll want to pull this in because the actual artifacts will be its children
  # in the resolved graph with gradle
  bazel query @regression_testing_gradle//:com_squareup_okio_okio >> "$TEST_LOG" 2>&1

  expect_log "@regression_testing_gradle//:com_squareup_okio_okio"

  # This is the actual JVM artifact which will have the jar for the KMP artifact
  bazel query @regression_testing_gradle//:com_squareup_okio_okio_jvm >> "$TEST_LOG" 2>&1

  expect_log "@regression_testing_gradle//:com_squareup_okio_okio_jvm"
}

function test_gradle_versions_catalog {
  # When source files are requested and we have a bug, this will fail
  bazel build @from_files//:all
}

TESTS=(
  "test_coursier_resolution_with_boms"
  "test_maven_resolution"
  "test_dependency_aggregation"
  "test_duplicate_version_warning"
  "test_duplicate_version_warning_same_version"
  "test_outdated"
  "test_outdated_no_external_runfiles"
  "test_outdated_with_boms"
  "test_outdated_with_boms_does_not_include_artifacts_without_a_version"
  "test_m2local_testing_found_local_artifact_through_pin_and_build"
  "test_unpinned_m2local_testing_found_local_artifact_through_pin_and_build"
  "test_m2local_testing_found_local_artifact_through_build"
  "test_m2local_testing_found_local_artifact_after_build_copy"
  "test_m2local_testing_ignore_empty_files"
  "test_unpinned_m2local_testing_ignore_empty_files"
  "test_found_artifact_with_plus_through_pin_and_build"
  "test_unpinned_found_artifact_with_plus_through_pin_and_build"
  "test_v1_lock_file_format"
  "test_dependency_pom_exclusion"
  "test_transitive_dependency_with_type_of_pom"
  "test_when_both_pom_and_jar_artifact_are_available_jar_artifact_is_present"
  "test_when_both_pom_and_jar_artifact_are_dependencies_jar_artifact_is_present"
  "test_publish_java_binary_jar_with_maven_export"
  # "test_gradle_metadata_is_resolved_correctly_for_aar_artifact"
  "test_gradle_metadata_is_resolved_correctly_for_jvm_artifact"
  "test_gradle_versions_catalog"
  # BOM resolution feature tests
  "test_bom_resolution_coursier_with_boms"
  "test_bom_resolution_maven_with_boms"
  "test_bom_resolution_disabled_no_section_no_marker"
  "test_bom_resolution_jvm_import_tags"
  "test_bom_resolution_unresolvable_bom_aborts_pin"
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
  if [ ! -f $file ]; then
    printf "NOT FOUND: $file (most probably wrong test configuration)\n"
    return 1
  fi

  local message=${3:-Expected not to find regexp \""$pattern"\", but it was found}
  grep -sq -- "$pattern" $file || return 0

  printf "FAILED\n"
  cat $file
  printf "FAILURE: $message\n"
  return 1
}

function expect_file_is_not_empty() {
  local file=$1
  if [ ! -f $file ]; then
    printf "NOT FOUND: $file (most probably wrong test configuration)\n"
    return 1
  fi
  if [ ! -s $file ]; then
    printf "FAILED: $file is empty\n"
    return 1
  fi
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
