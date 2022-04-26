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
    "json",
    "aar",
    "bundle",
    "eclipse-plugin",
    "orbit",
    "test-jar",
    "hk2-jar",
    "maven-plugin",
    "scala-jar",
]

# See https://github.com/bazelbuild/rules_jvm_external/issues/686
# A single package uses classifier to distingush the jar files (one per platform),
# So we need to check these are not dependnecies of each other.
PLATFORM_CLASSIFIER = [
    "linux-aarch_64",
    "linux-x86_64",
    "osx-aarch_64",
    "osx-x86_64",
    "windows-x86_64"
]

def strip_packaging_and_classifier(coord):
    # Strip some packaging and classifier values based on the following maven coordinate formats
    # groupId:artifactId:version
    # groupId:artifactId:packaging:version
    # groupId:artifactId:packaging:classifier:version
    coordinates = coord.split(":")
    if len(coordinates) > 4:
        if coordinates[3] in ["sources", "natives"]:
            coordinates.pop(3)

    if len(coordinates) > 3:
        # We add "pom" into SUPPORTED_PACKAGING_TYPES here because "pom" is not a
        # packaging type that Coursier CLI accepts.
        if coordinates[2] in SUPPORTED_PACKAGING_TYPES + ["pom"]:
            coordinates.pop(2)

    return ":".join(coordinates)

def strip_packaging_and_classifier_and_version(coord):
    coordinates = coord.split(":")

    # Support for simplified versionless groupId:artifactId coordinate format
    if len(coordinates) == 2:
        return ":".join(coordinates)
    return ":".join(strip_packaging_and_classifier(coord).split(":")[:-1])

# TODO: Should these methods be combined with _parse_maven_coordinate_string in specs.
def get_packaging(coord):
    # Get packaging from the following maven coordinate formats
    # groupId:artifactId:version
    # groupId:artifactId:packaging:version
    # groupId:artifactId:packaging:classifier:version
    coordinates = coord.split(":")
    if len(coordinates) > 3:
        return coordinates[2]
    else:
        return None

def get_classifier(coord):
    # Get classifier from the following maven coordinate formats
    # groupId:artifactId:version
    # groupId:artifactId:packaging:version
    # groupId:artifactId:packaging:classifier:version
    coordinates = coord.split(":")
    if len(coordinates) > 4:
        return coordinates[3]
    else:
        return None

def get_artifact(coord):
    # Get the artifact from the following maven coordinate formats
    # groupId:artifactId:version
    # groupId:artifactId:packaging:version
    # groupId:artifactId:packaging:classifier:version

    coordinates = coord.split(":")
    return coordinates[2]


def escape(string):
    for char in [".", "-", ":", "/", "+"]:
        string = string.replace(char, "_")
    return string.replace("[", "").replace("]", "").split(",")[0]
