# Stripped down version of a java_import Starlark rule, without invoking ijar
# to create interface jars.

# Inspired by Square's implementation of `raw_jvm_import` [0] and discussions
# on the GitHub thread [1] about ijar's interaction with Kotlin JARs.
#
# [0]: https://github.com/square/bazel_maven_repository/pull/48
# [1]: https://github.com/bazelbuild/bazel/issues/4549

load("//settings:stamp_manifest.bzl", "StampManifestProvider")

def _jvm_import_impl(ctx):
    if len(ctx.files.jars) != 1:
        fail("Please only specify one jar to import in the jars attribute.")

    injar = ctx.files.jars[0]
    outjar = ctx.actions.declare_file("processed_" + injar.basename, sibling = injar)
    ctx.actions.run_shell(
        inputs = [injar] + ctx.files._add_jar_manifest_entry,
        outputs = [outjar],
        command = "\n".join([
            # If the jar is signed do not modify the manifest because it will
            # make the signature invalid.
            "if unzip -l {injar} | grep -qE 'META-INF/.*\\.SF'; then".format(injar = injar.path),
            "  cp {injar} {outjar}".format(injar = injar.path, outjar = outjar.path),
            "else",
            "  set -e",
            "  {add_jar_manifest_entry} --source {injar} --manifest-entry 'Target-Label:{target_label}' --output {outjar}".format(
                add_jar_manifest_entry = ctx.attr._add_jar_manifest_entry.files_to_run.executable.path,
                injar = injar.path,
                target_label = ctx.label,
                outjar = outjar.path,
            ),
            "fi",
        ]),
        mnemonic = "StampJarManifest",
        progress_message = "Stamping the manifest of %s" % ctx.label,
        tools = [ctx.attr._add_jar_manifest_entry.files_to_run],
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
        "_add_jar_manifest_entry": attr.label(
            executable = True,
            cfg = "host",
            default = "//private/tools/java/rules/jvm/external/jar:AddJarManifestEntry",
        ),
        "_stamp_manifest": attr.label(
            default = Label("@rules_jvm_external//settings:stamp_manifest"),
        ),
    },
    implementation = _jvm_import_impl,
    provides = [JavaInfo],
)
