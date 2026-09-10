load("@io_bazel_stardoc//stardoc:stardoc.bzl", "stardoc")
load("@rules_shell//shell:sh_binary.bzl", "sh_binary")
load("@rules_shell//shell:sh_test.bzl", "sh_test")

exports_files(
    [
        "includes/main_functions_header.md",
    ],
    visibility = ["//scripts:__pkg__"],
)

stardoc(
    name = "defs",
    out = "defs.md",
    input = "//:defs.bzl",
    symbol_names = [
        "DEFAULT_REPOSITORY_NAME",
        "MavenPublishInfo",
        "artifact",
        "create_jar",
        "javadoc",
        "java_export",
        "java_plugin_artifact",
        "maven_artifact",
        "maven_bom",
        "maven_export",
        "pom_file",
        "read_coordinates",
    ],
    visibility = ["//scripts:__pkg__"],
    deps = [
        "//:implementation",
        "@bazel_skylib//lib:structs",
        "@bazel_skylib//rules:run_binary",
        "@rules_java//java/private:proto_support",
    ],
)

stardoc(
    name = "kt_defs",
    out = "kt_defs.md",
    input = "//:kt_defs.bzl",
    symbol_names = ["kt_jvm_export"],
    visibility = ["//scripts:__pkg__"],
    deps = [
        "//:implementation",
        "@rules_java//java/private:proto_support",
        "@rules_kotlin//kotlin",
        "@rules_kotlin//kotlin/internal",
        "@rules_kotlin//src/main/starlark/core/plugin",
    ],
)

stardoc(
    name = "extension",
    out = "extension.md",
    input = "//:extensions.bzl",
    visibility = ["//scripts:__pkg__"],
    deps = [
        "//:implementation",
    ],
)

sh_binary(
    name = "update",
    srcs = ["update_docs.sh"],
    args = [
        "$(location //scripts:generate_api_reference)",
        "api.md",
        "$(location :extension)",
        "extension-api.md",
    ],
    data = [
        ":extension",
        "//scripts:generate_api_reference",
    ],
)

sh_test(
    name = "generated_docs_test",
    size = "small",
    srcs = ["check_generated_docs.sh"],
    args = [
        "api.md",
        "$(location :api.md)",
        "$(location //scripts:generate_api_reference)",
        "extension-api.md",
        "$(location :extension-api.md)",
        "$(location :extension)",
    ],
    data = [
        ":api.md",
        ":extension",
        ":extension-api.md",
        "//scripts:generate_api_reference",
    ],
)
