load("//:specs.bzl", "maven", "parse")
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

def jetify_coord_str(coord_str):
    """
    Finds and returns jetified coordinate of given coordinate, otherwise returns original coordinate
    """
    artifact = parse.parse_maven_coordinate(coord_str)
    jetify_coord_tuple = jetify_maven_coord(
        artifact["group"],
        artifact["artifact"],
        artifact["version"],
    )
    if jetify_coord_tuple:
        artifact["group"] = jetify_coord_tuple[0]
        artifact["artifact"] = jetify_coord_tuple[1]
        artifact["version"] = jetify_coord_tuple[2]
        return maven.artifact_to_coord_str(artifact)
    else:
        return coord_str

def jetify_maven_coord(group, artifact, version = None):
    """
    Looks up support -> androidx artifact mapping, returns None if no mapping found.
    """
    if (group, artifact) not in jetifier_maven_map:
        return None

    if version:
        return jetifier_maven_map[(group, artifact)].get(version, None)
    else:
        return jetifier_maven_map[(group, artifact)].values()[0][:2]

def jetify_artifact_dependencies(deps):
    """Takes in list of maven coordinates and returns a list of jetified maven coordinates"""
    return [jetify_coord_str(coord) for coord in deps]
