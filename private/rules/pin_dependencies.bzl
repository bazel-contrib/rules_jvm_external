# Copyright 2024 The Bazel Authors. All rights reserved.
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

load("//private/rules:coursier.bzl", "compute_dependency_inputs_signature")

_TEMPLATE = """#!/usr/bin/env bash

set -euo pipefail

{resolver_cmd} --jvm_flags={jvm_flags} --argsfile {config} --input-hash-path '{input_hash_path}' --output {output}{dependency_index_output}
{bom_resolver_invocation}
"""

_BOM_RESOLVER_INVOCATION = "{bom_resolver_cmd} --lock-file={output} @{bom_args_file}"

def _stringify_exclusions(exclusions):
    return ["%s:%s" % (e["group"], e["artifact"]) for e in exclusions]

def _pin_dependencies_impl(ctx):
    # Repositories should be simple strings
    repos = []
    for r in ctx.attr.repositories:
        repo = json.decode(r)
        if repo.get("credentials"):
            fail("Please use your .netrc file for access credentials to repos: ", repo["repo_url"])
        repos.append(repo["repo_url"])

    exclusions = []
    for e in ctx.attr.excluded_artifacts:
        exclusion = json.decode(e)
        exclusions.append("%s:%s" % (exclusion["group"], exclusion["artifact"]))

    artifacts = []
    for a in ctx.attr.artifacts:
        artifact = json.decode(a)
        artifact["exclusions"] = _stringify_exclusions(artifact.get("exclusions", []))
        artifacts.append(artifact)

    boms = []
    for b in ctx.attr.boms:
        bom = json.decode(b)
        bom["exclusions"] = _stringify_exclusions(bom.get("exclusions", []))
        boms.append(bom)

    # Prep the external config file
    config = {
        "repositories": repos,
        "artifacts": artifacts,
        "boms": boms,
        "globalExclusions": exclusions,
        "fetchSources": ctx.attr.fetch_sources,
        "fetchJavadoc": ctx.attr.fetch_javadocs,
    }

    config_file = ctx.actions.declare_file("%s-config.json" % ctx.label.name)
    ctx.actions.write(
        config_file,
        content = json.encode_indent(config, indent = "  "),
    )

    input_hash, _ = compute_dependency_inputs_signature(
        boms = ctx.attr.boms,
        artifacts = ctx.attr.artifacts,
        repositories = ctx.attr.repositories,
        excluded_artifacts = ctx.attr.excluded_artifacts,
        store_bom_resolution = ctx.attr.store_bom_resolution,
    )

    hash_file = ctx.actions.declare_file("%s-input-hash.json" % ctx.label.name)
    ctx.actions.write(
        hash_file,
        content = json.encode_indent(input_hash, indent = "  "),
    )

    dependency_index_output = ""
    if ctx.attr.dependency_index:
        dependency_index_output = " --dependency-index-output $BUILD_WORKSPACE_DIRECTORY/" + ctx.attr.dependency_index

    runfiles_files = []
    bom_resolver_invocation = ""
    if ctx.attr.store_bom_resolution:
        # Versionless artifacts only — explicit-version artifacts are silently
        # skipped, and excluded / overridden artifacts are not present in the
        # generated jvm_import set so we don't annotate them either.
        versionless = [a for a in artifacts if not a.get("version")]
        bom_args_lines = []
        for bom in boms:
            bom_args_lines.append("--boms=%s:%s:%s" % (bom["group"], bom["artifact"], bom.get("version", "")))
        for repo_url in repos:
            bom_args_lines.append("--repositories=%s" % repo_url)
        for artifact in versionless:
            coord = "%s:%s" % (artifact["group"], artifact["artifact"])
            packaging = artifact.get("packaging", "jar") or "jar"
            classifier = artifact.get("classifier", "") or ""
            if packaging != "jar" or classifier != "":
                coord += ":%s" % packaging
                if classifier != "":
                    coord += ":%s" % classifier
            bom_args_lines.append("--artifacts=%s" % coord)

        bom_args_file = ctx.actions.declare_file("%s-bom-resolver-args.txt" % ctx.label.name)
        ctx.actions.write(
            bom_args_file,
            content = "\n".join(bom_args_lines) + "\n",
        )
        bom_resolver_invocation = _BOM_RESOLVER_INVOCATION.format(
            bom_resolver_cmd = ctx.executable.bom_resolver.short_path,
            output = "$BUILD_WORKSPACE_DIRECTORY/" + ctx.attr.lock_file,
            bom_args_file = bom_args_file.short_path,
        )
        runfiles_files.append(bom_args_file)

    script = ctx.actions.declare_file(ctx.label.name)
    ctx.actions.write(
        script,
        _TEMPLATE.format(
            config = config_file.short_path,
            input_hash_path = hash_file.short_path,
            resolver_cmd = ctx.executable.resolver.short_path,
            output = "$BUILD_WORKSPACE_DIRECTORY/" + ctx.attr.lock_file,
            dependency_index_output = dependency_index_output,
            jvm_flags = ctx.attr.jvm_flags,
            bom_resolver_invocation = bom_resolver_invocation,
        ),
        is_executable = True,
    )

    runfiles = ctx.runfiles(files = [script, config_file, hash_file] + runfiles_files).merge(
        ctx.attr.resolver[DefaultInfo].default_runfiles,
    )
    if ctx.attr.store_bom_resolution:
        runfiles = runfiles.merge(ctx.attr.bom_resolver[DefaultInfo].default_runfiles)

    return [
        DefaultInfo(
            executable = script,
            files = depset([script, config_file]),
            runfiles = runfiles,
        ),
    ]

pin_dependencies = rule(
    _pin_dependencies_impl,
    executable = True,
    attrs = {
        "artifacts": attr.string_list(
            doc = "List of JSON blobs generated by parse_artifact_spec_list",
        ),
        "boms": attr.string_list(
            doc = "List of JSON blobs generated by parse_artifact_spec_list",
        ),
        "excluded_artifacts": attr.string_list(
            doc = "List of JSON blobs generated by parse_artifact_spec_list",
        ),
        "repositories": attr.string_list(
            doc = "List of URLs",
        ),
        "fetch_sources": attr.bool(),
        "fetch_javadocs": attr.bool(),
        "lock_file": attr.string(
            doc = "Location of the generated lock file",
            mandatory = True,
        ),
        "dependency_index": attr.string(
            doc = "Location of the generated dependency index file",
        ),
        "jvm_flags": attr.string(
            doc = "JVM flags to pass to resolver",
        ),
        "resolver": attr.label(
            executable = True,
            cfg = "exec",
            default = "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/cmd:Resolver",
        ),
        "store_bom_resolution": attr.bool(
            default = False,
            doc = "Whether to invoke BomResolverMain after resolution to populate the bom_resolution section.",
        ),
        "bom_resolver": attr.label(
            executable = True,
            cfg = "exec",
            default = "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/bom:BomResolverMain",
        ),
    },
)
