# Stripped down version of a java_import Starlark rule, without invoking ijar
# to create interface jars.

# Inspired by Square's implementation of `raw_jvm_import` [0] and discussions
# on the GitHub thread [1] about ijar's interaction with Kotlin JARs.
#
# [0]: https://github.com/square/bazel_maven_repository/pull/48
# [1]: https://github.com/bazelbuild/bazel/issues/4549

load("@rules_jvm_external//settings:stamp_manifest.bzl", "StampManifestProvider")

def _jvm_import_impl(ctx):
    if len(ctx.files.jars) != 1:
        fail("Please only specify one jar to import in the jars attribute.")

    injar = ctx.files.jars[0]
    manifest_update_file = ctx.actions.declare_file(injar.basename + ".target_label_manifest", sibling = injar)
    ctx.actions.expand_template(
        template = ctx.file._manifest_template,
        output = manifest_update_file,
        substitutions = {
            "{TARGETLABEL}": "%s" % ctx.label,
        },
    )

    outjar = ctx.actions.declare_file("processed_" + injar.basename, sibling = injar)
    ctx.actions.run_shell(
        inputs = [injar, manifest_update_file] + ctx.files._host_javabase,
        outputs = [outjar],
        command = " && ".join([
            # Make a copy of the original jar, since `jar(1)` modifies the jar in place.
            "cp {input_jar} {output_jar}".format(input_jar = injar.path, output_jar = outjar.path),
            # Set the write bit on the copied jar.
            "chmod +w {output_jar}".format(output_jar = outjar.path),
            # If the jar is signed do not modify the manifest because it will
            # make the signature invalid. Otherwise append the Target-Label
            # manifest attribute using `jar umf`
            "(unzip -l {output_jar} | grep -qE 'META-INF/.*\\.SF') || ({jar} umf {manifest_update_file} {output_jar} > /dev/null 2>&1 || true)".format(
                jar = "%s/bin/jar" % ctx.attr._host_javabase[java_common.JavaRuntimeInfo].java_home,
                manifest_update_file = manifest_update_file.path,
                output_jar = outjar.path,
            ),
        ]),
        mnemonic = "StampJarManifest",
        progress_message = "Stamping the manifest of %s" % ctx.label,
    )

    if not ctx.attr._stamp_manifest[StampManifestProvider].stamp_enabled:
        outjar = injar

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
            default = Label("@bazel_tools//tools/jdk:current_host_java_runtime"),
            providers = [java_common.JavaRuntimeInfo],
        ),
        "_manifest_template": attr.label(
            default = Label("@rules_jvm_external//private/templates:manifest_target_label.tpl"),
            allow_single_file = True,
        ),
        "_stamp_manifest": attr.label(
            default = Label("@rules_jvm_external//settings:stamp_manifest"),
        )
    },
    implementation = _jvm_import_impl,
    provides = [JavaInfo],
)
