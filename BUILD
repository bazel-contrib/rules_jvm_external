load("@io_bazel_stardoc//stardoc:stardoc.bzl", "stardoc")
load("@bazel_skylib//:bzl_library.bzl", "bzl_library")
load("//:private/versions.bzl", "COURSIER_CLI_HTTP_FILE_NAME")

exports_files(["defs.bzl"])

licenses(["notice"])  # Apache 2.0

stardoc(
    name = "defs",
    out = "defs.md",
    input = "defs.bzl",
    symbol_names = ["maven_install"],
    deps = ["//:implementation"],
)

stardoc(
    name = "specs",
    out = "specs.md",
    input = "specs.bzl",
    symbol_names = [
        "maven.artifact",
        "maven.repository",
        "maven.exclusion",
    ],
    deps = ["//:implementation"],
)

bzl_library(
    name = "implementation",
    srcs = [
        ":coursier.bzl",
        ":defs.bzl",
        ":specs.bzl",
        "//:private/coursier_utilities.bzl",
        "//:private/dependency_tree_parser.bzl",
        "//:private/proxy.bzl",
        "//:private/versions.bzl",
        "//third_party/bazel_json/lib:json_parser.bzl",
    ],
)

genrule(
    name = "generate_api_reference",
    srcs = [
        "//:docs/includes/main_functions_header.md",
        "defs.md",
        "//:docs/includes/spec_functions_header.md",
        "specs.md",
    ],
    outs = ["api.md"],
    cmd = """cat \
    $(location //:docs/includes/main_functions_header.md) \
    $(location //:defs.md) \
    $(location //:docs/includes/spec_functions_header.md) \
    $(location //:specs.md) > $@""",
)

sh_binary(
    name = "mirror_coursier",
    srcs = [":scripts/mirror_coursier.sh"],
    data = ["@" + COURSIER_CLI_HTTP_FILE_NAME + "//file"],
)
