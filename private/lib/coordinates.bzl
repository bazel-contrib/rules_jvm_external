def unpack_coordinates(coords):
    """Takes a maven coordinate and unpacks it into a struct with fields
    `group`, `artifact`, `version`, `packaging`, `classifier`
    where `version,` `packaging` and `classifier` may be `None`

    Assumes `coords` is in one of the following syntaxes:
     * group:artifact[:packaging[:classifier]]:version
     * group:artifact[:version][:classifier][@packaging]
    """
    if not coords:
        return None

    pieces = coords.split(":")
    if len(pieces) < 2:
        fail("Could not parse maven coordinate: %s" % coords)
    group = pieces[0]
    artifact = pieces[1]

    if len(pieces) == 2:
        return struct(group = group, artifact = artifact, version = "", packaging = None, classifier = None)

    # Unambiguously the original format
    if len(pieces) == 5:
        packaging = pieces[2]
        classifier = pieces[3]
        version = pieces[4]
        return struct(group = group, artifact = artifact, packaging = packaging, classifier = classifier, version = version)

    # If we're using BOMs, the version is optional. That means at this point
    # we could be dealing with g:a:p or g:a:v
    is_gradle = _is_version(pieces[2])

    if len(pieces) == 3:
        if is_gradle:
            if "@" in pieces[2]:
                (version, packaging) = pieces[2].split("@", 2)
                return struct(group = group, artifact = artifact, packaging = packaging, version = version, classifier = None)
            version = pieces[2]
            return struct(group = group, artifact = artifact, version = version, packaging = None, classifier = None)
        else:
            packaging = pieces[2]
            return struct(group = group, artifact = artifact, packaging = packaging, version = "", classifier = None)

    if len(pieces) == 4:
        if is_gradle:
            version = pieces[2]
            if "@" in pieces[3]:
                (classifier, packaging) = pieces[3].split("@", 2)
                return struct(group = group, artifact = artifact, packaging = packaging, classifier = classifier, version = version)
            classifier = pieces[3]
            return struct(group = group, artifact = artifact, classifier = classifier, version = version, packaging = None)
        else:
            packaging = pieces[2]
            version = pieces[3]
            return struct(group = group, artifact = artifact, packaging = packaging, version = version, classifier = None)

    fail("Could not parse maven coordinate: %s" % coords)

def _is_version(part):
    # The maven spec allows a version to be alpha-numeric characters plus "." and "-"
    # We are going to take a slight shortcut and assume that a version will have at
    # least one digit, if this breaks and an artifact has only non-numeric characters
    # in its version then that artifact will need to be a fully specified as an
    # artifact instead of in short-form
    for char in part.elems():
        if char.isdigit():
            return True
    return False

def to_external_form(coords):
    """Formats `coords` as a string suitable for use by tools such as Gradle.

    The returned format matches Gradle's "external dependency" short-form
    syntax: `group:name:version:classifier@packaging`
    """

    if type(coords) == "string":
        unpacked = unpack_coordinates(coords)
    else:
        unpacked = coords

    to_return = "%s:%s:%s" % (unpacked.group, unpacked.artifact, unpacked.version)

    if hasattr(unpacked, "classifier"):
        if unpacked.classifier and unpacked.classifier != "jar":
            to_return += ":" + unpacked.classifier

    if hasattr(unpacked, "packaging"):
        if unpacked.packaging and unpacked.packaging != "jar":
            to_return += "@" + unpacked.packaging

    return to_return

_DEFAULT_PURL_REPOS = [
    "https://repo.maven.apache.org/maven2",
    "https://repo.maven.apache.org/maven2/",
    "https://repo1.maven.org",
    "https://repo1.maven.org/",
]

def to_purl(coords, repository):
    to_return = "pkg:maven/"

    unpacked = unpack_coordinates(coords)
    to_return += "{group}:{artifact}@{version}".format(
        artifact = unpacked.artifact,
        group = unpacked.group,
        version = unpacked.version,
    )

    suffix = []
    if unpacked.classifier:
        suffix.append("classifier=" + unpacked.classifier)
    if unpacked.packaging and "jar" != unpacked.packaging:
        suffix.append("type=" + unpacked.packaging)
    if repository and repository not in _DEFAULT_PURL_REPOS:
        # Default repository name is pulled from https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst
        suffix.append("repository=" + repository)

    if len(suffix):
        to_return += "?" + "&".join(suffix)

    return to_return
