load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("//:defs.bzl", "artifact")

build_test(
    name = "override_duplicates",
    targets = [":main"],
)

java_library(
    name = "main",
    srcs = ["Main.java"],
    deps = ["@regression_testing//:com_netflix_ndbench_ndbench_core"],
)