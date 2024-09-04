def unpack_coordinates(coords):
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

    if nparts == 2:
        return struct(
            groupId = parts[0],
            artifactId = parts[1],
            type = None,
            scope = None,
            version = None,
            classifier = None,
        )

    if nparts < 3 or nparts > 5:
        fail("Unparsed: %s" % coords)

    version = parts[-1]
    parts = dict(enumerate(parts[:-1]))
    return struct(
        groupId = parts.get(0),
        artifactId = parts.get(1),
        type = parts.get(2),
        scope = parts.get(3),
        classifier = None,
        version = version,
    )

def to_external_form(coords):
    """Formats `coords` as a string suitable for use by tools such as Gradle.

    The returned format matches Gradle's "external dependency" short-form
    syntax: `group:name:version:classifier@type`
    """

    if type(coords) == "string":
        unpacked = unpack_coordinates(coords)
    else:
        unpacked = coords

    to_return = "%s:%s:%s" % (unpacked.groupId, unpacked.artifactId, unpacked.version)

    if hasattr(unpacked, "classifier"):
        to_return += ":" + unpacked.classifier

    if hasattr(unpacked, "type"):
        to_return += "@" + unpacked.type

    return to_return
