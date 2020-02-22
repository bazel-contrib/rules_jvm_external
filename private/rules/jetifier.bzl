load(":jetifier_maven_map.bzl", "jetifier_maven_map")
load(":jvm_import.bzl", "jvm_import")

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
            cfg = "host",
        ),
    },
    implementation = _jetify_impl,
)

def jetify_aar_import(name, aar, **kwargs):
    jetify(
        name = "jetified_" + name,
        srcs = [aar],
    )

    native.aar_import(
        name = name,
        aar = ":jetified_" + name,
        **kwargs
    )

def jetify_jvm_import(name, jars, **kwargs):
    jetify(
        name = "jetified_" + name,
        srcs = jars,
    )

    jvm_import(
        name = name,
        jars = [":jetified_" + name],
        **kwargs
    )

def jetify_maven_coord(coord):
    return jetifier_maven_map.get(coord, coord)
