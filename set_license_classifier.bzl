# Copyright 2022 The Bazel Authors. All rights reserved.
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

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def _ensure_rules_license():
    if not native.existing_rule("rules_license"):
        http_archive(
            name = "rules_license",
            urls = [
                "https://mirror.bazel.build/github.com/bazelbuild/rules_license/releases/download/0.0.1/rules_license-0.0.1.tar.gz",
            ],
            sha256 = "4865059254da674e3d18ab242e21c17f7e3e8c6b1f1421fffa4c5070f82e98b5",
        )


def set_default_license_classifier_impl(rctx):
    rctx.file("license_classifier.bzl", content = """
def lookup_license(url=None, sha256=None, maven_id=None):
    return None
""")
    rctx.file("BUILD", content = "")

_set_default_license_classifier = repository_rule(
    implementation=set_default_license_classifier_impl,
)

def use_default_license_classifier():
    _ensure_rules_license()
    if not native.existing_rule("rules_jvm_license_classifier"):
        _set_default_license_classifier(
            name = "rules_jvm_license_classifier",
        )

def set_license_classifier(path):
    _ensure_rules_license()
    if native.existing_rule("rules_jvm_license_classifier"):
        fail("You are trying to set the rules_jvm_external license classifier a second time.")
    native.local_repository(
        name = "rules_jvm_license_classifier",
        path = path)
