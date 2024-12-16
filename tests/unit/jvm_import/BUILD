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
load("@rules_java//java:defs.bzl", "java_import")
load(":jvm_import_test.bzl", "jvm_import_test_suite")
load(":platform_transition_jar.bzl", "platform_transition_jar")

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

platform(
    name = "android",
    constraint_values = ["@platforms//os:android"],
)

platform(
    name = "linux",
    constraint_values = ["@platforms//os:linux"],
)

# Platform constraint is valid on Android but invalid on Linux:
#
#     maven_install(
#        name = "jvm_import_test",
#        # ...
#        targets_compatible_with = ["@platforms//os:android"],
#        # ...
#     )
platform_transition_jar(
    name = "findbugs_for_android",
    src = "@jvm_import_test//:com_google_code_findbugs_jsr305_3_0_2",
    platform = ":android",
)

platform_transition_jar(
    name = "findbugs_for_linux",
    src = "@jvm_import_test//:com_google_code_findbugs_jsr305_3_0_2",
    platform = ":linux",
)

build_test(
    name = "test_does_jar_artifact_work_with_matching_platform_constraint",
    targets = [
        ":findbugs_for_android",
    ],
)

jvm_import_test_suite(name = "jvm_import_tests")
