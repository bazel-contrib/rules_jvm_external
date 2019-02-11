load("@gmaven_rules//:coursier.bzl", "coursier_fetch")

def maven_install(repositories = [], artifacts = {}):
    for requested_artifact in artifacts.items():
        (fqn, sha256) = requested_artifact
        print(artifact(fqn), fqn)
        coursier_fetch(
            name = _repository_name_from_fqn(fqn),
            fqn = fqn,
            repositories = repositories,
        )

def artifact(fqn):
    # The top level artifact label is the same name as the repository name
    repository_name = _repository_name_from_fqn(fqn)
    return "@%s//:%s" % (repository_name, repository_name)

def _repository_name_from_fqn(fqn):
    parts = fqn.split(":")
    packaging = "jar"

    if len(parts) == 3:
        group_id, artifact_id, version = parts
    elif len(parts) == 4:
        group_id, artifact_id, packaging, version = parts
    elif len(parts) == 5:
        _, _, _, classifier, _ = parts
        fail("Classifiers are currently not supported. Please remove it from the coordinate: %s" % classifier)
    else:
        fail("Invalid qualified name for artifact: %s" % fqn)

    return "%s_%s_%s" % (_escape(group_id), _escape(artifact_id), _escape(version))

def _escape(string):
    return string.replace(".", "_").replace("-", "_")
