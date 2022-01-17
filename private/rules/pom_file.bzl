load(":has_maven_deps.bzl", "MavenInfo", "has_maven_deps")

def _format_dep(unpacked):
    return "".join([
 "        <dependency>\n",
 "            <groupId>%s</groupId>\n" % unpacked.groupId,
 "            <artifactId>%s</artifactId>\n" % unpacked.artifactId,
 "            <version>%s</version>\n" % unpacked.version,
("            <type>%s</type>\n" % unpacked.type) if unpacked.type and unpacked.type != "jar" else "",
("            <scope>%s</scope>\n" % unpacked.scope) if unpacked.scope and unpacked.scope != "compile" else "",
 "        </dependency>",
    ])

def _unpack_coordinates(coords):
    """Takes a maven coordinate and unpacks it into a struct with fields
    `groupId`, `artifactId`, `version`, `type`, `scope`
    where type and scope are optional.

    Assumes following maven coordinate syntax:
    groupId:artifactId[:type[:scope]]:version
    """
    if not coords:
        return None

    parts = coords.split(":")
    nparts = len(parts)
    if nparts < 3 or nparts > 5:
        fail("Unparsed: %s" % coords)

    version = parts[-1]
    parts = dict(enumerate(parts[:-1]))
    return struct(
        groupId = parts.get(0),
        artifactId = parts.get(1),
        type = parts.get(2),
        scope = parts.get(3),
        version = version,
    )

def _pom_file_impl(ctx):
    # Ensure the target has coordinates
    if not ctx.attr.target[MavenInfo].coordinates:
        fail("pom_file target must have maven coordinates.")

    info = ctx.attr.target[MavenInfo]

    coordinates = _unpack_coordinates(info.coordinates)
    substitutions = {
        "{groupId}": coordinates.groupId,
        "{artifactId}": coordinates.artifactId,
        "{version}": coordinates.version,
        "{type}": coordinates.type,
        "{scope}": coordinates.scope,
    }

    deps = []
    for dep in sorted(info.maven_deps.to_list()):
        unpacked = _unpack_coordinates(dep)
        deps.append(_format_dep(unpacked))
    substitutions.update({"{dependencies}": "\n".join(deps)})

    out = ctx.actions.declare_file("%s.xml" % ctx.label.name)
    ctx.actions.expand_template(
        template = ctx.file.pom_template,
        output = out,
        substitutions = substitutions,
    )

    return [
        DefaultInfo(files = depset([out])),
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
  {type}: Replaced by the maven coordintes type, if present (defaults to "jar")
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
    },
)
