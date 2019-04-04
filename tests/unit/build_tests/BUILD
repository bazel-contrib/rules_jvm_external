load("@bazel_skylib//rules:build_test.bzl", "build_test")

# Regression tests for particular artifacts that revealed problems for this
# project.

build_test(
    name = "all_artifacts",
    targets = [
        "@regression_testing//:org_pantsbuild_jarjar_1_6_6",
        ":neverlink_app",
    ],
)

android_binary(
    name = "neverlink_app",
    manifest = "AndroidManifest.xml",
    custom_package = "com.example.bazel",
    deps = ["@regression_testing//:com_squareup_javapoet"], # Javapoet is a compile time dependency.
)
