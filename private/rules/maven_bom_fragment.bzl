load(":has_maven_deps.bzl", "MavenInfo", "has_maven_deps")

MavenBomFragmentInfo = provider(
    fields = {
        "coordinates": "Maven coordinates for this part of the BOM",
        "artifact": "The `maven_project_jar` that forms the main artifact",
        "srcs": "The src-jar of the artifact",
        "javadocs": "The javadocs of the artifact. May be `None`",
        "pom_template": "The `pom.xml` template file",
        "maven_info": "The `MavenInfo` of `artifact`",
    },
)

def _maven_bom_fragment_impl(ctx):
    return [
        MavenBomFragmentInfo(
            coordinates = ctx.attr.maven_coordinates,
            artifact = ctx.file.artifact,
            srcs = ctx.file.src_artifact,
            javadocs = ctx.file.javadoc_artifact,
            pom_template = ctx.file.pom_template,
            maven_info = ctx.attr.artifact[MavenInfo],
        ),
    ]

maven_bom_fragment = rule(
    _maven_bom_fragment_impl,
    attrs = {
        "maven_coordinates": attr.string(
            doc = """The maven coordinates that should be used for the generated artifact""",
            mandatory = True,
        ),
        "artifact": attr.label(
            doc = """The `maven_project_jar` that forms the primary artifact of the maven coordinates""",
            allow_single_file = True,
            mandatory = True,
            providers = [
                [JavaInfo],
            ],
            aspects = [
                has_maven_deps,
            ],
        ),
        "src_artifact": attr.label(
            doc = """The source jar generated from `artifact`""",
            allow_single_file = True,
            mandatory = True,
        ),
        "javadoc_artifact": attr.label(
            doc = """The javadoc jar generated from the `artifact`""",
            allow_single_file = True,
        ),
        "pom_template": attr.label(
            doc = """The template to use when generating the `pom.xml` file""",
            allow_single_file = True,
            default = "//private/templates:pom-with-parent.tpl",
        ),
    },
    provides = [
        MavenBomFragmentInfo,
    ],
)
