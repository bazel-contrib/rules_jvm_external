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
load("//private/lib:utils.bzl", "is_windows", "file_to_rlocationpath")
load("//private/windows:bat_binary.bzl", "bat_binary_action", "BAT_BINARY_IMPLICIT_ATTRS")

_TEMPLATE_SH = """#!/usr/bin/env bash

{resolver_cmd} --jvm_flags={jvm_flags} --argsfile {config} --resolver {resolver} --input_hash '{input_hash}' --output $BUILD_WORKSPACE_DIRECTORY/{output}
"""

#Note: Win needs to use rlocation for all workspace paths.
_TEMPLATE_WIN = """
@echo off
call %BAT_RUNFILES_LIB% rlocation resolver_cmd_path {resolver_cmd_rpath} || goto eof
call %BAT_RUNFILES_LIB% rlocation config_path {config_rpath} || goto eof

"%resolver_cmd_path%" --jvm_flags={jvm_flags} --argsfile "%config_path%" --resolver {resolver} --input_hash {input_hash} --output "%BUILD_WORKSPACE_DIRECTORY%\\{output}"

:eof
"""

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

    input_hash = compute_dependency_inputs_signature(
        boms = ctx.attr.boms,
        artifacts = ctx.attr.artifacts,
        repositories = ctx.attr.repositories,
        excluded_artifacts = ctx.attr.excluded_artifacts,
    )

    if is_windows(ctx):
        script = ctx.actions.declare_file(ctx.label.name + ".bat")
        ctx.actions.write(
            script,
            _TEMPLATE_WIN.format(
                config_rpath = file_to_rlocationpath(ctx, config_file),
                input_hash = input_hash[0],
                resolver_cmd_rpath = file_to_rlocationpath(ctx, ctx.executable._resolver),
                resolver = ctx.attr.resolver,
                output = ctx.attr.lock_file,
                jvm_flags = ctx.attr.jvm_flags,
            ),
            is_executable = True,
        )
        default_info = bat_binary_action(
            ctx = ctx,
            src = script, 
            data_files = [config_file],
            data_defaultinfos = [ctx.attr._resolver[DefaultInfo]]
        )
    else:
        script = ctx.actions.declare_file(ctx.label.name)
        ctx.actions.write(
            script,
            _TEMPLATE_SH.format(
                config = config_file.short_path,
                input_hash = input_hash[0],
                resolver_cmd = ctx.executable._resolver.short_path,
                resolver = ctx.attr.resolver,
                output =  ctx.attr.lock_file,
                jvm_flags = ctx.attr.jvm_flags,
            ),
            is_executable = True,
        )
        default_info = DefaultInfo(
            executable = script,
            files = depset([script, config_file]),
            runfiles = ctx.runfiles(files = [script, config_file]).merge(ctx.attr._resolver[DefaultInfo].default_runfiles),
        )


    return [
        default_info
    ]

pin_dependencies = rule(
    _pin_dependencies_impl,
    executable = True,
    attrs = {
        # Note: We plan to support other resolvers (eg. `gradle`) in the future. Currently, there's just one
        #       supported option.
        "resolver": attr.string(
            doc = "The resolver to use",
            values = ["maven"],
            default = "maven",
        ),
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
        "jvm_flags": attr.string(
            doc = "JVM flags to pass to resolver",
        ),
        "_resolver": attr.label(
            executable = True,
            cfg = "target",
            default = "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/cmd:Resolver",
        ),
    } | BAT_BINARY_IMPLICIT_ATTRS,
)
