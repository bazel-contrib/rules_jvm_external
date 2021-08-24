load("//third_party/bazel_skylib/lib:versions.bzl", "versions")
load("//third_party/bazel_json/lib:json_parser.bzl", "json_parse")

def json_decode(s):
    if  versions.is_at_least("4.0.0", native.bazel_version):
        return json.decode(s)
    else:
        return json_parse(s)
