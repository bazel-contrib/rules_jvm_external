load("@rules_java//java:defs.bzl", "JavaInfo")
load(":has_maven_deps.bzl", "MavenInfo", "calculate_artifact_jars", "has_maven_deps")
load(":maven_utils.bzl", "determine_additional_dependencies", "generate_pom")

def _pom_file_impl(ctx):
    # Ensure the target has coordinates
    if not ctx.attr.target[MavenInfo].coordinates:
        fail("pom_file target must have maven coordinates.")

    info = ctx.attr.target[MavenInfo]

    artifact_jars = calculate_artifact_jars(info)
    additional_deps = determine_additional_dependencies(artifact_jars, ctx.attr.additional_dependencies)

    def get_implementation_coordinates(target):
        if not info.label_to_javainfo.get(target.label):
            return None
        if not target[MavenInfo].coordinates:
            return None

        return ctx.expand_make_variables("implementation_deps", target[MavenInfo].coordinates, ctx.var)

    implementation_deps = [
        get_implementation_coordinates(target)
        for target in ctx.attr.implementation_deps
    ]
    implementation_deps = [dep for dep in implementation_deps if dep]

    all_maven_deps = info.maven_deps.to_list()
    for dep in additional_deps:
        for coords in dep[MavenInfo].as_maven_dep.to_list():
            all_maven_deps.append(coords)
    expanded_maven_deps = [
        ctx.expand_make_variables("additional_deps", coords, ctx.var)
        for coords in all_maven_deps
    ]

    # Expand maven coordinates for any variables to be replaced.
    coordinates = ctx.expand_make_variables("coordinates", info.coordinates, ctx.var)

    out = generate_pom(
        ctx,
        coordinates = coordinates,
        versioned_dep_coordinates = sorted(expanded_maven_deps),
        pom_template = ctx.file.pom_template,
        out_name = "%s.xml" % ctx.label.name,
        implementation_deps = implementation_deps,
    )

    return [
        DefaultInfo(
            files = depset([out]),
            data_runfiles = ctx.runfiles([out]),
        ),
        OutputGroupInfo(
            pom = depset([out]),
        ),
    ]

pom_file = rule(
    _pom_file_impl,
    doc = """Generate a pom.xml file that lists first-order maven dependencies.

The following substitutions are performed on the template file:

  {groupId}: Replaced with the maven coordinates group ID.
  {artifactId}: Replaced with the maven coordinates artifact ID.
  {version}: Replaced by the maven coordinates version.
  {type}: Replaced by the maven coordinates type, if present (defaults to "jar")
  {scope}: Replaced by the maven coordinates type, if present (defaults to "compile")
  {dependencies}: Replaced by a list of maven dependencies directly relied upon
    by java_library targets within the artifact.
""",
    attrs = {
        "pom_template": attr.label(
            doc = "Template file to use for the pom.xml",
            default = "//private/templates:pom.tpl",
            allow_single_file = True,
        ),
        "target": attr.label(
            doc = "The rule to base the pom file on. Must be a java_library and have a maven_coordinate tag.",
            mandatory = True,
            providers = [
                [JavaInfo],
            ],
            aspects = [
                has_maven_deps,
            ],
        ),
        "additional_dependencies": attr.label_keyed_string_dict(
            doc = "Mapping of `Label`s to the excluded workspace names",
            allow_empty = True,
            providers = [
                [JavaInfo],
            ],
            aspects = [
                has_maven_deps,
            ],
        ),
        "implementation_deps": attr.label_list(
            doc = "A list of labels of Java targets to include as 'implementation' dependencies. These are given runtime scope on the generated pom file.",
            allow_empty = True,
            aspects = [
                has_maven_deps,
            ],
        ),
    },
)
