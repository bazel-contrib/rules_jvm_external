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
    ctx.actions.run_shell(
        inputs = [injar] + ctx.files._host_javabase,
        outputs = [outjar],
        arguments = [],
        command = " && ".join([
            "cp {input_jar} {output_jar}".format(input_jar = injar.path, output_jar = outjar.path),
            "chmod u+w {output_jar}".format(output_jar = outjar.path),
            "echo 'Target-Label: {label}' > manifest.txt".format(label = ctx.label),
            "{jar} ufm {output_jar} manifest.txt".format(
                jar = "%s/bin/jar" % ctx.attr._host_javabase[java_common.JavaRuntimeInfo].java_home,
                output_jar = outjar.path,
            ),
        ]),
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
        "_host_javabase": attr.label(
            cfg = "host",
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
    },
    implementation = _jvm_import_impl,
    provides = [JavaInfo],
)
