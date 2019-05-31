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

    return [
        DefaultInfo(
            files = depset(ctx.files.jars),
        ),
        JavaInfo(
            compile_jar = ctx.files.jars[0],
            output_jar = ctx.files.jars[0],
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
    },
    implementation = _jvm_import_impl,
    provides = [JavaInfo],
)
