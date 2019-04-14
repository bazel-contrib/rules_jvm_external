load("@bazel_skylib//rules:build_test.bzl", "build_test")

# Regression tests for particular artifacts that revealed problems for this
# project.

build_test(
    name = "all_artifacts",
    targets = [
        "@regression_testing//:org_pantsbuild_jarjar_1_6_6",
        "@regression_testing//:com_digitalasset_damlc_osx_100_12_1",
    ],
)
