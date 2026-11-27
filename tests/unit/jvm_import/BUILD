# Copyright 2021 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_java//java:java_import.bzl", "java_import")
load("@rules_java//java:java_library.bzl", "java_library")
load("//:defs.bzl", "artifact")
load(":jvm_import_test.bzl", "jvm_import_test_suite")

platform(
    name = "android_s390x_target_platform",
    constraint_values = [
        "@platforms//cpu:s390x",
        "@platforms//os:android",
    ],
)

java_import(
    name = "java_import_that_consumes_the_downloaded_file_directly",
    # Will produce an error if the downloaded file does not have the `.jar` file extension
    jars = ["@com_google_guava_guava_27_0_jre//file"],
)

build_test(
    name = "test_does_jar_artifact_have_the_correct_file_extension",
    targets = [
        ":java_import_that_consumes_the_downloaded_file_directly",
    ],
)

# Uses a method inherited from a class in a dependency of the imported jar. This
# only compiles with a reduced classpath if `jvm_import` lists the dependencies
# of the jar in its `.jdeps`; see `test_jvm_import_reduced_classpath` in
# `tests/bazel_run_tests.sh`.
java_library(
    name = "uses_inherited_api_of_imported_jar",
    srcs = ["reduced_classpath/UsesInheritedApi.java"],
    deps = [
        artifact(
            "com.google.auth:google-auth-library-oauth2-http",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

jvm_import_test_suite(name = "jvm_import_tests")
