load("@rules_java//java:defs.bzl", "JavaInfo")
load(":has_maven_deps.bzl", "MavenInfo", "calculate_artifact_jars", "has_maven_deps")
load(":maven_utils.bzl", "determine_additional_dependencies", "generate_pom")

def _pom_file_impl(ctx):
    # Ensure the target has coordinates
    expanded_maven_deps = []
    expanded_export_deps = []
    if ctx.attr.target:
        if not ctx.attr.target[MavenInfo].coordinates:
            fail("pom_file target must have maven coordinates.")

        info = ctx.attr.target[MavenInfo]

        artifact_jars = calculate_artifact_jars(info)
        additional_deps = determine_additional_dependencies(artifact_jars, ctx.attr.additional_dependencies)

        all_maven_deps = info.maven_deps.to_list()
        export_maven_deps = info.maven_export_deps.to_list()

        for dep in additional_deps:
            for coords in dep[MavenInfo].as_maven_dep.to_list():
                all_maven_deps.append(coords)

        expanded_maven_deps = [
            ctx.expand_make_variables("additional_deps", coords, ctx.var)
            for coords in all_maven_deps
        ]
        expanded_export_deps = [
            ctx.expand_make_variables("maven_export_deps", coords, ctx.var)
            for coords in export_maven_deps
        ]

        def get_exclusion_coordinates(target):
            if not info.label_to_javainfo.get(target.label):
                print("Warning: exclusions key %s not found in dependencies" % (target))
                return None
            else:
                coords = ctx.expand_make_variables("exclusions", target[MavenInfo].coordinates, ctx.var)
                return coords

        exclusions_unsorted = {
            get_exclusion_coordinates(target): json.decode(targetExclusions)
            for target, targetExclusions in ctx.attr.exclusions.items()
        }
        exclusions_unsorted = {k: v for k, v in exclusions_unsorted.items() if k != None}

        for coords, exclusion_list in exclusions_unsorted.items():
            reformatted_exclusion_list = []
            for exclusion in exclusion_list:
                reformatted_exclusion_list.append(exclusion["group"] + ":" + exclusion["artifact"])
            exclusions_unsorted[coords] = reformatted_exclusion_list

        for maven_info in info.all_infos.to_list():
            if maven_info.coordinates and maven_info.exclusions:
                for exclusion in maven_info.exclusions:
                    if maven_info.coordinates not in exclusions_unsorted:
                        exclusions_unsorted[maven_info.coordinates] = []
                    exclusions_unsorted[maven_info.coordinates].append(exclusion)

        exclusions = {}
        for coords in exclusions_unsorted:
            exclusions[coords] = sorted(exclusions_unsorted[coords])

        # Expand maven coordinates for any variables to be replaced.
        coordinates = ctx.expand_make_variables("coordinates", info.coordinates, ctx.var)
    else:
        if not ctx.attr.coordinates:
            fail("pom_file must have either a target with maven coordinates, or manually specified coordinates.")
        coordinates = ctx.expand_make_variables("coordinates", ctx.attr.coordinates, ctx.var)

    out = generate_pom(
        ctx,
        coordinates = coordinates,
        versioned_dep_coordinates = sorted(expanded_maven_deps),
        versioned_export_dep_coordinates = expanded_export_deps,
        pom_template = ctx.file.pom_template,
        out_name = "%s.xml" % ctx.label.name,
        exclusions = exclusions,
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
    by java_library targets within the artifact. Dependencies have exclusions
    for any transitive dependencies that are occur in deploy_env.
""",
    attrs = {
        "pom_template": attr.label(
            doc = "Template file to use for the pom.xml",
            default = "//private/templates:pom.tpl",
            allow_single_file = True,
        ),
        "target": attr.label(
            doc = "The rule to base the pom file on. Must be a java_library and have a maven_coordinate tag.",
            providers = [
                [JavaInfo],
            ],
            aspects = [
                has_maven_deps,
            ],
        ),
        "coordinates": attr.string(
            doc = "Manual maven coordinates to use",
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
        "exclusions": attr.label_keyed_string_dict(
            doc = "Mapping of dependency labels to a list of exclusions (encoded as a json string). Each exclusion is a dict with a group and an artifact.",
            allow_empty = True,
            aspects = [
                has_maven_deps,
            ],
        ),
    },
)
