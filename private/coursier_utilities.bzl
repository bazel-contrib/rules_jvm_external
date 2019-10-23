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

# Coursier uses these types to determine what files it should resolve and fetch.
# For example, some jars have the type "eclipse-plugin", and Coursier would not
# download them if it's not asked to to resolve "eclipse-plugin".
SUPPORTED_PACKAGING_TYPES = [
    "jar",
    "aar",
    "bundle",
    "eclipse-plugin",
    "orbit",
    "test-jar",
    "hk2-jar",
    "maven-plugin",
    "scala-jar",
]

def strip_packaging_and_classifier(coord):
    # We add "pom" into _COURSIER_PACKAGING_TYPES here because "pom" is not a
    # packaging type that Coursier CLI accepts.
    for packaging_type in SUPPORTED_PACKAGING_TYPES + ["pom"]:
        coord = coord.replace(":%s:" % packaging_type, ":")
    for classifier_type in ["sources", "natives"]:
        coord = coord.replace(":%s:" % classifier_type, ":")

    return coord

def strip_packaging_and_classifier_and_version(coord):
    return ":".join(strip_packaging_and_classifier(coord).split(":")[:-1])

def escape(string):
    for char in [".", "-", ":", "/", "+"]:
        string = string.replace(char, "_")
    return string.replace("[", "").replace("]", "").split(",")[0]
