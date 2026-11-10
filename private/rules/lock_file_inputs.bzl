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

"""Explicit resolution inputs shared by lock-file writers and freshness checks."""

def _exclusion(value):
    if type(value) == "string" and not value.startswith("{"):
        return value
    value = json.decode(value) if type(value) == "string" else value
    return "%s:%s" % (value["group"], value["artifact"])

def canonical_request(value):
    """Normalise defaults without replacing a requested selector with its selection."""
    value = json.decode(value) if type(value) == "string" else value
    request = {key: item for key, item in value.items() if item != None}
    request["version"] = request.get("version", "")
    for key in ["force_version", "neverlink", "testonly"]:
        if not request.get(key):
            request.pop(key, None)
    for key in ["classifier", "packaging"]:
        if not request.get(key):
            request.pop(key, None)
    if request.get("exclusions"):
        request["exclusions"] = sorted({_exclusion(e): True for e in request["exclusions"]})
    else:
        request.pop("exclusions", None)
    if "override_license_types" in request:
        request["override_license_types"] = sorted({license: True for license in request["override_license_types"]})
    return json.decode(json.encode(request))

def canonical_requests(values):
    """Sort and deduplicate equivalent direct requests, retaining distinct variants."""
    return [json.decode(value) for value in sorted({json.encode(canonical_request(value)): True for value in values})]

def canonical_inputs(
        artifacts = [],
        boms = [],
        excluded_artifacts = [],
        resolver = "coursier",
        fetch_sources = False,
        fetch_javadoc = False,
        ignore_empty_files = False,
        version_conflict_policy = "default",
        additional_coursier_options = []):
    """Capture freshness inputs directly, with repository locations excluded."""
    return {
        "artifacts": canonical_requests(artifacts),
        # BOM precedence follows declaration order.
        "boms": [canonical_request(value) for value in boms],
        "settings": {
            "excluded_artifacts": sorted({_exclusion(value): True for value in excluded_artifacts}),
            "resolver": resolver,
            "fetch_sources": fetch_sources,
            "fetch_javadoc": fetch_javadoc,
            "ignore_empty_files": ignore_empty_files,
            "version_conflict_policy": version_conflict_policy,
            "additional_coursier_options": additional_coursier_options,
        },
    }

def inputs_from_attrs(attrs):
    """Use the same snapshot for repository validation and both pin paths."""
    return canonical_inputs(
        artifacts = attrs.artifacts,
        boms = attrs.boms,
        excluded_artifacts = attrs.excluded_artifacts,
        resolver = getattr(attrs, "resolver", "coursier"),
        fetch_sources = attrs.fetch_sources,
        fetch_javadoc = attrs.fetch_javadoc,
        ignore_empty_files = attrs.ignore_empty_files,
        version_conflict_policy = attrs.version_conflict_policy,
        additional_coursier_options = attrs.additional_coursier_options,
    )
