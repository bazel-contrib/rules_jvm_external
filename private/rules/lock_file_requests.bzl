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

"""Attach declarations to selections while keeping requested versions explicit."""

load("//private/lib:coordinates.bzl", "to_key")
load(":lock_file_inputs.bzl", "canonical_requests")
load(":lock_file_records.bzl", "metadata", "parse_external_form")

def _matches(request, parsed):
    return request["group"] == parsed.group and request["artifact"] == parsed.artifact and (request.get("classifier") or "jar") == (parsed.classifier or "jar") and (not request.get("packaging") or request["packaging"] == (parsed.packaging or "jar"))

def _select(request, candidates):
    matching = [(row, parsed) for row, parsed in candidates if _matches(request, parsed)]
    jars = [row for row, parsed in matching if (parsed.packaging or "jar") == "jar"]
    if jars:
        return jars[0]
    downloads = [(row, parsed) for row, parsed in matching if parsed.packaging != "pom"]
    matching = downloads or matching
    if len(matching) > 1:
        fail("Ambiguous packaging for request %s:%s" % (request["group"], request["artifact"]))
    return matching[0][0] if matching else None

def _mutable_metadata(row):
    if len(row) == 3:
        row.append({})
    return row[3]

def attach_requests(contents, inputs):
    """Attach every effective direct declaration or retain it as unresolved."""
    by_module = {}
    for row in contents["artifacts"]:
        parsed = parse_external_form(row[0])
        by_module.setdefault(parsed.group + ":" + parsed.artifact, []).append((row, parsed))
    unresolved = []
    for request in inputs["artifacts"]:
        candidates = by_module.get(request["group"] + ":" + request["artifact"], [])
        row = _select(request, candidates)
        if row == None:
            unresolved.append(request)
            continue
        compact = {key: value for key, value in request.items() if key not in ["group", "artifact"]}
        if compact["version"] == parse_external_form(row[0]).version:
            compact.pop("version")
        _mutable_metadata(row).setdefault("requests", []).append(compact)
    for candidates in by_module.values():
        anchors = [(row, parsed) for row, parsed in candidates if parsed.classifier not in ["sources", "javadoc"]]
        jars = [row for row, parsed in anchors if (parsed.classifier or "jar") == "jar" and (parsed.packaging or "jar") == "jar"]
        variants = sorted([to_key(parsed) for _, parsed in candidates if parsed.classifier in ["sources", "javadoc"]])
        if anchors and variants:
            anchor = jars[0] if jars else anchors[0][0]
            _mutable_metadata(anchor)["variants"] = variants
    for row in contents["artifacts"]:
        if "requests" in metadata(row):
            row[3]["requests"] = [json.decode(value) for value in sorted({json.encode(request): True for request in row[3]["requests"]})]
    contents.update(boms = inputs["boms"], settings = inputs["settings"], unresolved_requests = unresolved)

def read_inputs(contents):
    """Recover declarations, including those whose requested version lost a conflict."""
    requests = list(contents["unresolved_requests"])
    for row in contents["artifacts"]:
        parsed = parse_external_form(row[0])
        for compact in metadata(row).get("requests", []):
            if "group" in compact or "artifact" in compact:
                fail("Request metadata must use its artifact's identity: %s" % row[0])
            request = dict(group = parsed.group, artifact = parsed.artifact, version = parsed.version)
            request.update(compact)
            if not _matches(request, parsed):
                fail("Request attached to wrong artifact: %s" % row[0])
            requests.append(request)
    return {
        "artifacts": canonical_requests(requests),
        "boms": contents["boms"],
        "settings": contents["settings"],
    }
