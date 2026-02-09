load("@bazel_tools//tools/build_defs/repo:utils.bzl", "parse_netrc")
load("//private/lib:coordinates.bzl", "unpack_coordinates")

#
# Parsing tools
#

def _parse_exclusion_spec_list(exclusion_specs):
    """
    Given a string (g:a), returns an exclusion map
    """
    exclusions = []
    for exclusion_spec in exclusion_specs:
        if type(exclusion_spec) == "string":
            pieces = exclusion_spec.split(":")
            if len(pieces) == 2:
                exclusion_spec = {"group": pieces[0], "artifact": pieces[1]}
            else:
                fail(("Invalid exclusion: %s. Exclusions are specified as " +
                      "group-id:artifact-id, without the version, packaging or " +
                      "classifier.") % exclusion_spec)
        exclusions.append(exclusion_spec)
    return exclusions

def _parse_maven_coordinate_string(mvn_coord):
    """
    Given a string containing a standard Maven coordinate (g:a:[p:[c:]]v) or gradle external dependency (g:a:v:c@p), returns a maven artifact map (see above).
    """
    unpacked = unpack_coordinates(mvn_coord)

    # It would be nice to use `bazel_skylib//lib:structs.bzl` for this, but we avoid
    # loading it here to keep this file lightweight and free of external dependencies.
    return {
        key: getattr(unpacked, key)
        for key in dir(unpacked)
        if key != "to_json" and key != "to_proto"
    }

def _parse_repository_spec_list(repository_specs):
    """
    Given a list containing either strings or repository maps (see above), returns a list containing repository maps.
    """
    repos = []
    for repo in repository_specs:
        if type(repo) == "string":
            repos.append({"repo_url": repo})
        else:
            repos.append(repo)
    return repos

def _parse_artifact_spec_list(artifact_specs):
    """
    Given a list containing either strings or artifact maps (see above), returns a list containing artifact maps.
    """
    artifacts = []
    for artifact in artifact_specs:
        if type(artifact) == "string":
            artifacts.append(_parse_maven_coordinate_string(artifact))
        else:
            if "version" not in artifact:
                artifact["version"] = ""
            artifacts.append(artifact)
    return artifacts

parse = struct(
    parse_maven_coordinate = _parse_maven_coordinate_string,
    parse_repository_spec_list = _parse_repository_spec_list,
    parse_artifact_spec_list = _parse_artifact_spec_list,
    parse_exclusion_spec_list = _parse_exclusion_spec_list,
)

#
# JSON serialization
#

def _repository_credentials_spec_to_json(credentials_spec):
    """
    Given a repository credential spec or None, returns the json serialization of the object, or None if the object wasn't given.

    """
    if credentials_spec != None:
        return "{ \"user\": \"" + credentials_spec["user"] + "\", \"password\": \"" + credentials_spec["password"] + "\" }"
    else:
        return None

def _repository_spec_to_json(repository_spec):
    """
    Given a repository spec, returns the json serialization of the object.
    """
    maybe_credentials_json = _repository_credentials_spec_to_json(repository_spec.get("credentials"))
    if maybe_credentials_json != None:
        return "{ \"repo_url\": \"" + repository_spec["repo_url"] + "\", \"credentials\": " + maybe_credentials_json + " }"
    else:
        return "{ \"repo_url\": \"" + repository_spec["repo_url"] + "\" }"

def _exclusion_spec_to_json(exclusion_spec):
    """
    Given an artifact exclusion spec, returns the json serialization of the object.
    """
    return "{ \"group\": \"" + exclusion_spec["group"] + "\", \"artifact\": \"" + exclusion_spec["artifact"] + "\" }"

def _exclusion_spec_list_to_json(exclusion_spec):
    """
    Given a list of artifact exclusion spec, returns its json serialization.
    """
    return "[" + ", ".join([_exclusion_spec_to_json(spec) for spec in exclusion_spec]) + "]"

def _override_license_types_spec_to_json(override_license_types_spec):
    """
    Given an override license types spec, returns the json serialization of the object.
    """
    license_type_strings = []
    for license_type in override_license_types_spec:
        license_type_strings.append("\"" + license_type + "\"")
    return ("[" + ", ".join(license_type_strings) + "]")

def _artifact_spec_to_json(artifact_spec):
    """
    Given an artifact spec, returns the json serialization of the object.
    """
    maybe_exclusion_specs_jsons = []
    for spec in _parse_exclusion_spec_list(artifact_spec.get("exclusions") or []):
        maybe_exclusion_specs_jsons.append(_exclusion_spec_to_json(spec))
    exclusion_specs_json = (("[" + ", ".join(maybe_exclusion_specs_jsons) + "]") if len(maybe_exclusion_specs_jsons) > 0 else None)

    required = "{ \"group\": \"" + artifact_spec["group"] + \
               "\", \"artifact\": \"" + artifact_spec["artifact"] + \
               "\", \"version\": \"" + artifact_spec["version"] + "\""

    with_packaging = required + ((", \"packaging\": \"" + artifact_spec["packaging"] + "\"") if artifact_spec.get("packaging") != None else "")
    with_classifier = with_packaging + ((", \"classifier\": \"" + artifact_spec["classifier"] + "\"") if artifact_spec.get("classifier") != None else "")
    with_override_license_types = with_classifier + ((", " + _override_license_types_spec_to_json(artifact_spec["override_license_types"])) if artifact_spec.get("override_license_types") != None else "")
    with_exclusions = with_override_license_types + ((", \"exclusions\": " + exclusion_specs_json) if artifact_spec.get("exclusions") != None else "")
    with_neverlink = with_exclusions + ((", \"neverlink\": " + str(artifact_spec.get("neverlink")).lower()) if artifact_spec.get("neverlink") != None else "")
    with_testonly = with_neverlink + ((", \"testonly\": " + str(artifact_spec.get("testonly")).lower()) if artifact_spec.get("testonly") != None else "")
    with_forced_version = with_testonly + ((", \"force_version\": true") if artifact_spec.get("force_version") != None else "")

    return with_forced_version + " }"

json = struct(
    write_repository_credentials_spec = _repository_credentials_spec_to_json,
    write_repository_spec = _repository_spec_to_json,
    write_exclusion_spec = _exclusion_spec_to_json,
    write_exclusion_spec_list = _exclusion_spec_list_to_json,
    write_override_license_types_spec = _override_license_types_spec_to_json,
    write_artifact_spec = _artifact_spec_to_json,
)

#
# Accessors
#

#
# Coursier expects artifacts to be defined in the form `group:artifact:version`, but it also supports some attributes:
# `url`, `ext`, `classifier`, `exclude` and `type`.
# In contrast with group, artifact and version, the attributes are a key=value comma-separated string appended at the end,
# For example: `coursier fetch group:artifact:version,classifier=xxx,url=yyy`
#
def _artifact_to_coord(artifact):
    classifier = (",classifier=" + artifact["classifier"]) if artifact.get("classifier") != None else ""
    type = (",type=" + artifact["packaging"]) if artifact.get("packaging") not in (None, "jar") else ""
    return artifact["group"] + ":" + artifact["artifact"] + ":" + artifact["version"] + classifier + type

#
# Returns a string "{hostname} {user}:{password}" for a repository_spec
#
def _repository_credentials(repository_spec):
    if "credentials" not in repository_spec:
        fail("Asked to generate credentials for a repository that does not need it")

    (_, remainder) = repository_spec["repo_url"].split("//")
    hostname = remainder.split("/")[0]

    credentials = repository_spec["credentials"]

    return _coursier_credential(hostname, credentials["user"], credentials["password"])

def _coursier_credential(hostname, username, password):
    """format a coursier credential

    See https://get-coursier.io/docs/other-credentials.html#inline for docs on this format

    Args:
        hostname: the host string
        username: the user string
        password: the pass string
    Returns:
        the credential string
    """
    return hostname + " " + username + ":" + password

def _netrc_credentials(netrc_content):
    """return a list of credentials from netrc content

    Args:
        netrc_content: a Dict<string,Dict<string,string>> from parse_netrc.
    Returns:
        a List<string> for the coursier cli --credentials option.
    """
    credentials = []
    for machine, props in parse_netrc(netrc_content).items():
        if machine and "login" in props and "password" in props:
            credential = _coursier_credential(machine, props["login"], props["password"])
            credentials.append(credential)
    return credentials

utils = struct(
    artifact_coordinate = _artifact_to_coord,
    netrc_credentials = _netrc_credentials,
    parse_netrc = parse_netrc,
    repo_credentials = _repository_credentials,
)
