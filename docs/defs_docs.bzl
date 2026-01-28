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

"""Documentation-only re-exports to keep stardoc deps minimal."""

load("//private/rules:java_export.bzl", _java_export = "java_export")
load("//private/rules:javadoc.bzl", _javadoc = "javadoc")
load("//private/rules:maven_bom.bzl", _maven_bom = "maven_bom")
load("//private/rules:maven_install.bzl", _maven_install = "maven_install")

javadoc = _javadoc
java_export = _java_export
maven_bom = _maven_bom
maven_install = _maven_install
