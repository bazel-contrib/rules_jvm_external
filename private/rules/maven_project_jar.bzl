load(":has_maven_deps.bzl", "MavenInfo", "calculate_artifact_jars", "calculate_artifact_source_jars", "has_maven_deps")

def _combine_jars(ctx, merge_jars, inputs, excludes, output):
    args = ctx.actions.args()
    args.add("--output", output)
    args.add_all(inputs, before_each = "--sources")
    args.add_all(excludes, before_each = "--exclude")

    ctx.actions.run(
        mnemonic = "MergeJars",
        inputs = inputs + excludes,
        outputs = [output],
        executable = merge_jars,
        arguments = [args],
    )

def _maven_project_jar_impl(ctx):
    target = ctx.attr.target
    info = target[MavenInfo]

    # Identify the subset of JavaInfo to include in the artifact
    artifact_jars = calculate_artifact_jars(info)
    artifact_srcs = calculate_artifact_source_jars(info)

    # Merge together all the binary jars
    bin_jar = ctx.actions.declare_file("%s.jar" % ctx.label.name)
    _combine_jars(
        ctx,
        ctx.executable._merge_jars,
        artifact_jars,
        depset(transitive =
            [ji.transitive_runtime_jars for ji in info.dep_infos.to_list()] +
            [jar[JavaInfo].transitive_runtime_jars for jar in ctx.attr.deploy_env]).to_list(),
        bin_jar,
    )

    src_jar = ctx.actions.declare_file("%s-src.jar" % ctx.label.name)
    _combine_jars(
        ctx,
        ctx.executable._merge_jars,
        artifact_srcs,
        depset(transitive = [ji.transitive_source_jars for ji in info.dep_infos.to_list()]).to_list(),
        src_jar,
    )

    java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo]
    ijar = java_common.run_ijar(
        actions = ctx.actions,
        jar = bin_jar,
        target_label = ctx.label,
        java_toolchain = java_toolchain,
    )

    # Grab the exported javainfos
    exported_infos = []
    targets = target[MavenInfo].transitive_exports.to_list()

    for label in targets:
        export_info = info.label_to_javainfo.get(label)
        if export_info != None:
            exported_infos.append(export_info)

    java_info = JavaInfo(
        output_jar = bin_jar,
        compile_jar = ijar,
        source_jar = src_jar,

        # TODO: calculate runtime_deps too
        deps = info.dep_infos.to_list(),
        exports = exported_infos,
    )

    return [
        DefaultInfo(files = depset([bin_jar])),
        OutputGroupInfo(
            maven_artifact = [bin_jar],
            maven_source = [src_jar],
            # Same outputgroup name used by `java_library`
            _source_jars = [src_jar],
        ),
        java_info,
    ]

maven_project_jar = rule(
    _maven_project_jar_impl,
    doc = """Combines all project jars into a jar suitable for uploading to maven.

A "project" is defined as the `target` library and all it's dependencies
that are not tagged with `maven_coordinates=`. This allows you to group
code within your repo however you choose, using fine-grained `java_library`
targets and dependencies loaded via `maven_install`, but still produce a
single artifact that other teams can download and use.
""",
    attrs = {
        "target": attr.label(
            doc = "The rule to build the jar from",
            mandatory = True,
            providers = [
                [JavaInfo],
            ],
            aspects = [
                has_maven_deps,
            ],
        ),
        "deploy_env": attr.label_list(
            doc = "A list of targets to exclude from the generated jar",
            providers = [
                [JavaInfo],
            ],
            allow_empty = True,
        ),
        # Bazel's own singlejar doesn't respect java service files,
        # so use our own.
        "_merge_jars": attr.label(
            executable = True,
            cfg = "host",
            default = "//private/tools/java/rules/jvm/external/jar:MergeJars",
        ),
        "_java_toolchain": attr.label(
            default = "@bazel_tools//tools/jdk:current_java_toolchain",
        ),
    },
)
