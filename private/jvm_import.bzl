# Stripped down version of a java_import Starlark rule, without invoking ijar
# to create interface jars.

# Inspired by Square's implementation of `raw_jvm_import` [0] and discussions
# on the GitHub thread [1] about ijar's interaction with Kotlin JARs.
#
# [0]: https://github.com/square/bazel_maven_repository/pull/48
# [1]: https://github.com/bazelbuild/bazel/issues/4549

def _jvm_import_impl(ctx):
    if len(ctx.files.jars) != 1:
        fail("Please only specify one jar to import in the jars attribute.")

    injar = ctx.files.jars[0]
    outjar = ctx.actions.declare_file("stamped_" + injar.basename, sibling = injar)
    args = ctx.actions.args()
    args.add("--output")
    args.add(outjar)
    args.add("--sources")
    args.add(injar)
    args.add("--deploy_manifest_lines")

    # Required for buildozer's add_dep feature with strict deps
    args.add("Target-Label: %s" % ctx.label)

    ctx.actions.run(
        executable = ctx.executable._singlejar,
        arguments = [args],
        inputs = [injar],
        outputs = [outjar],
        mnemonic = "StampJar",
        progress_message = "Stamping manifest of %s" % ctx.label,
    )

    return [
        DefaultInfo(
            files = depset([outjar]),
        ),
        JavaInfo(
            compile_jar = outjar,
            output_jar = outjar,
            source_jar = ctx.file.srcjar,
            deps = [
                dep[JavaInfo]
                for dep in ctx.attr.deps
                if JavaInfo in dep
            ],
            neverlink = ctx.attr.neverlink,
        ),
    ]

jvm_import = rule(
    attrs = {
        "jars": attr.label_list(
            allow_files = True,
            mandatory = True,
            cfg = "target",
        ),
        "srcjar": attr.label(
            allow_single_file = True,
            mandatory = False,
            cfg = "target",
        ),
        "deps": attr.label_list(
            default = [],
            providers = [JavaInfo],
        ),
        "neverlink": attr.bool(
            default = False,
        ),
        "_singlejar": attr.label(
            default = Label("@bazel_tools//tools/jdk:singlejar"),
            executable = True,
            cfg = "host",
        ),
    },
    implementation = _jvm_import_impl,
    provides = [JavaInfo],
)
