load("//:specs.bzl", "parse")
load("//private:constants.bzl", "DEFAULT_REPOSITORY_NAME")

def artifact(a, repository_name = DEFAULT_REPOSITORY_NAME):
    artifact_obj = _parse_artifact_str(a) if type(a) == "string" else a
    return "@%s//:%s" % (repository_name, _escape(artifact_obj["group"] + ":" + artifact_obj["artifact"]))

def maven_artifact(a):
    return artifact(a, repository_name = DEFAULT_REPOSITORY_NAME)

def _escape(string):
    return string.replace(".", "_").replace("-", "_").replace(":", "_")

def _parse_artifact_str(artifact_str):
    pieces = artifact_str.split(":")
    if len(pieces) == 2:
        return {"group": pieces[0], "artifact": pieces[1]}
    else:
        return parse.parse_maven_coordinate(artifact_str)
