load("//:specs.bzl", "parse")
load("//private:constants.bzl", "DEFAULT_REPOSITORY_NAME")
load("//private:coursier_utilities.bzl", "strip_packaging_and_classifier_and_version")

def artifact(a, repository_name = DEFAULT_REPOSITORY_NAME):
    artifact_str = _make_artifact_str(a) if type(a) != "string" else a
    return "@%s//:%s" % (repository_name, _escape(strip_packaging_and_classifier_and_version(artifact_str)))

def maven_artifact(a):
    return artifact(a, repository_name = DEFAULT_REPOSITORY_NAME)

def _escape(string):
    return string.replace(".", "_").replace("-", "_").replace(":", "_")

def _make_artifact_str(artifact_obj):
    # TODO: add support for optional type, classifier and version parts in artifact_obj case
    return artifact_obj["group"] + ":" + artifact_obj["artifact"]
