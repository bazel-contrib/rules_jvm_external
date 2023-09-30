load(":jvm_import.bzl", "jvm_import")

_DEPRECATION_MESSAGE = "Please update your dependencies to no longer require jetification."

def _jetify_impl(ctx):
    srcs = ctx.attr.srcs
    outfiles = []
    for src in srcs:
        for artifact in src.files.to_list():
            jetified_outfile = ctx.actions.declare_file("jetified_" + artifact.basename, sibling = artifact)
            jetify_args = ctx.actions.args()
            jetify_args.add("-l", "error")
            jetify_args.add("-o", jetified_outfile)
            jetify_args.add("-i", artifact)
            jetify_args.add("-timestampsPolicy", "keepPrevious")
            ctx.actions.run(
                mnemonic = "Jetify",
                inputs = [artifact],
                outputs = [jetified_outfile],
                progress_message = "Jetifying {}".format(artifact.owner),
                executable = ctx.executable._jetifier,
                arguments = [jetify_args],
            )
            outfiles.append(jetified_outfile)

    return [DefaultInfo(files = depset(outfiles))]

jetify = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = [".jar", ".aar"]),
        "_jetifier": attr.label(
            executable = True,
            default = Label("@rules_jvm_external//third_party/jetifier"),
            cfg = "exec",
        ),
    },
    implementation = _jetify_impl,
)

def jetify_aar_import(name, aar, _aar_import = None, visibility = None, **kwargs):
    jetify(
        name = "jetified_" + name,
        deprecation = _DEPRECATION_MESSAGE,
        srcs = [aar],
        visibility = visibility,
    )

    if not _aar_import:
        _aar_import = native.aar_import

    _aar_import(
        name = name,
        aar = ":jetified_" + name,
        visibility = visibility,
        **kwargs
    )

def jetify_jvm_import(name, jars, visibility = None, **kwargs):
    jetify(
        name = "jetified_" + name,
        deprecation = _DEPRECATION_MESSAGE,
        srcs = jars,
        visibility = visibility,
    )

    jvm_import(
        name = name,
        jars = [":jetified_" + name],
        visibility = visibility,
        **kwargs
    )

