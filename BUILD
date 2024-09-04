load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

exports_files([
    "defs.bzl",
    "specs.bzl",
])

licenses(["notice"])  # Apache 2.0

bzl_library(
    name = "implementation",
    srcs = [
        ":defs.bzl",
        ":specs.bzl",
        "//private:artifact_utilities.bzl",
        "//private:constants.bzl",
        "//private:coursier_utilities.bzl",
        "//private:dependency_tree_parser.bzl",
        "//private:java_utilities.bzl",
        "//private:proxy.bzl",
        "//private:versions.bzl",
        "//private/rules:artifact.bzl",
        "//private/rules:coursier.bzl",
        "//private/rules:generate_pin_repository.bzl",
        "//private/rules:has_maven_deps.bzl",
        "//private/rules:java_export.bzl",
        "//private/rules:javadoc.bzl",
        "//private/rules:jvm_import.bzl",
        "//private/rules:maven_bom.bzl",
        "//private/rules:maven_bom_fragment.bzl",
        "//private/rules:maven_install.bzl",
        "//private/rules:maven_project_jar.bzl",
        "//private/rules:maven_publish.bzl",
        "//private/rules:maven_utils.bzl",
        "//private/rules:pin_dependencies.bzl",
        "//private/rules:pom_file.bzl",
        "//private/rules:urls.bzl",
        "//private/rules:v1_lock_file.bzl",
        "//private/rules:v2_lock_file.bzl",
        "//settings:stamp_manifest.bzl",
    ],
    visibility = [
        # This library is only visible to allow others who depend on
        # `rules_jvm_external` to be able to document their code using
        # stardoc.
        "//visibility:public",
    ],
    deps = [
        "@rules_java//java:rules",
    ],
)

alias(
    name = "mirror_coursier",
    actual = "//scripts:mirror_coursier",
)

alias(
    name = "generate_api_reference",
    actual = "//scripts:generate_api_reference",
)
