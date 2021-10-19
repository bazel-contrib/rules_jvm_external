# Copyright 2019 The Bazel Authors. All rights reserved.
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

"""
This module contains a test suite for testing jvm_import
"""

load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")

TagsInfo = provider(
    doc = "Provider to propagate jvm_import's tags for testing purposes",
    fields = {
        "tags": "tags to be propagated for jvm_import's tests",
    },
)

def _tags_propagator_impl(target, ctx):
    tags = getattr(ctx.rule.attr, "tags")
    return TagsInfo(tags = tags)

tags_propagator = aspect(
    doc = "Aspect that propagates tags to help with testing jvm_import",
    attr_aspects = ["deps"],
    implementation = _tags_propagator_impl,
)

def _does_jvm_import_have_tags_impl(ctx):
    env = analysistest.begin(ctx)

    expected_tags = [
        "maven_coordinates=com.google.code.findbugs:jsr305:3.0.2",
        "maven_url=https://jcenter.bintray.com/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar",
    ]

    asserts.equals(env, ctx.attr.src[TagsInfo].tags, expected_tags)
    return analysistest.end(env)

does_jvm_import_have_tags_test = analysistest.make(
    _does_jvm_import_have_tags_impl,
    attrs = {
        "src": attr.label(
            doc = "Target to traverse for tags",
            aspects = [tags_propagator],
            mandatory = True,
        ),
    },
)

def jvm_import_test_suite(name):
    does_jvm_import_have_tags_test(
        name = "does_jvm_import_have_tags_test",
        target_under_test = "@jvm_import_test//:com_google_code_findbugs_jsr305_3_0_2",
        src = "@jvm_import_test//:com_google_code_findbugs_jsr305_3_0_2",
    )
    native.test_suite(
        name = name,
        tests = [
            ":does_jvm_import_have_tags_test",
        ],
    )
