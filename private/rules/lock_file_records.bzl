# Copyright 2026 The Bazel Authors. All rights reserved.
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

"""Compact resolved records and their repository-independent integrity checks."""

load("//private/lib:coordinates.bzl", "to_external_form", "to_key")

def coordinates_from_identity(identity, version):
    parts = identity.split(":")
    if len(parts) not in [2, 3, 4] or not all(parts[:2]):
        fail("Invalid artifact identity: %s" % identity)
    return {
        "group": parts[0],
        "artifact": parts[1],
        "version": version,
        "packaging": parts[2] if len(parts) > 2 else "jar",
        "classifier": parts[3] if len(parts) > 3 else "jar",
    }

def parse_external_form(coordinate):
    """Parse the known external syntax without Maven's four-field ambiguity."""
    sections = coordinate.split("@")
    pieces = sections[0].split(":")
    if len(sections) > 2 or len(pieces) not in [3, 4] or not all(pieces):
        fail("Invalid Gradle external coordinate: %s" % coordinate)
    parsed = struct(
        group = pieces[0],
        artifact = pieces[1],
        version = pieces[2],
        classifier = pieces[3] if len(pieces) == 4 else None,
        packaging = sections[1] if len(sections) == 2 else None,
    )
    if to_external_form(parsed) != coordinate:
        fail("Artifact coordinate must use the full Gradle external form: %s" % coordinate)
    return parsed

def canonical_identity(identity):
    return to_key(coordinates_from_identity(identity, ""))

def normalise_v3(contents):
    """Normalise legacy aliases before graph checks, hashes and download hints."""
    converted = dict(contents)
    dependencies = {}
    for parent, children in contents.get("dependencies", {}).items():
        key = canonical_identity(parent)
        dependencies.setdefault(key, {}).update({canonical_identity(child): True for child in children})
    converted["dependencies"] = {parent: sorted(children) for parent, children in dependencies.items()}
    converted["repositories"] = {repo: sorted({canonical_identity(member): True for member in members}) for repo, members in contents.get("repositories", {}).items()}
    if "skipped" in contents:
        converted["skipped"] = sorted({canonical_identity(identity): True for identity in contents["skipped"]})
    return converted

def metadata(row):
    return row[3] if len(row) == 4 else {}

def records_by_identity(contents):
    records = {}
    for row in contents["artifacts"]:
        if type(row) != "list" or len(row) not in [3, 4] or type(row[0]) != "string":
            fail("Invalid artifact record; expected [coordinate, SHA-256, hash, optional metadata]")
        parsed = parse_external_form(row[0])
        if row[1] != None and (type(row[1]) != "string" or len(row[1]) != 64 or not all([c in "0123456789abcdef" for c in row[1].elems()])):
            fail("Invalid SHA-256 for %s" % row[0])
        if type(metadata(row)) != "dict":
            fail("Invalid artifact metadata: %s" % row[0])
        for key in ["requests", "variants"]:
            if key in metadata(row) and type(metadata(row)[key]) != "list":
                fail("Invalid %s metadata for %s" % (key, row[0]))
        identity = to_key(parsed)
        if identity in records:
            fail("Duplicate artifact identity: %s" % identity)
        records[identity] = row
    return records

def row_hash(row, children, local, skipped):
    """Hash own data and immediate identities, without propagating child updates."""
    return hash(json.encode([row[0], row[1], sorted(children), metadata(row), local, skipped]))

def compute_hashes(contents):
    records = records_by_identity(contents)
    dependencies = contents.get("dependencies", {})
    local = contents.get("m2local_artifacts", [])
    skipped = contents.get("skipped", [])
    hashes = {}
    for identity, row in records.items():
        references = dependencies.get(identity, []) + metadata(row).get("variants", [])
        valid = all([child in records for child in references])

        # Invalid hashes retain the ordinary warning and REPIN recovery path.
        hashes[identity] = row_hash(row, dependencies.get(identity, []), identity in local, identity in skipped) if valid else None
    for identity in list(dependencies) + local + skipped:
        if identity not in records:
            hashes["__missing__:" + identity] = None
    if local and not contents.get("m2local"):
        hashes["__m2local__"] = None
    return hashes

def stored_hashes(contents):
    return {identity: row[2] for identity, row in records_by_identity(contents).items() if type(row[2]) == "int"}

def records_from_v3(contents):
    """Expand legacy grouped checksums into independently versioned records."""
    records = {}
    identities = {}
    for identity, info in contents["artifacts"].items():
        if len(identity.split(":")) > 3:
            fail("Invalid v3 artifact root: %s" % identity)
        for classifier, sha in info["shasums"].items():
            parsed = coordinates_from_identity(identity, info["version"])
            parsed["classifier"] = classifier
            coordinate = to_external_form(parsed)
            key = to_key(parsed)
            if key in identities:
                fail("Duplicate artifact identity: %s" % key)
            identities[key] = True
            records[coordinate] = [coordinate, sha, 0]
    for raw_identity in contents.get("skipped", []):
        identity = canonical_identity(raw_identity)
        if identity in identities:
            continue
        parsed = coordinates_from_identity(identity, "")
        versions = {
            parse_external_form(row[0]).version: True
            for row in records.values()
            if row[0].startswith("%s:%s:" % (parsed["group"], parsed["artifact"])) and (parse_external_form(row[0]).packaging or "jar") == parsed["packaging"]
        }
        if len(versions) != 1:
            fail("Cannot recover version for skipped artifact %s" % identity)
        parsed["version"] = versions.keys()[0]
        coordinate = to_external_form(parsed)
        records[coordinate] = [coordinate, None, 0]
    return [records[coordinate] for coordinate in sorted(records)]
