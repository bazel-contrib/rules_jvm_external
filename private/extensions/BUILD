load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "implementation",
    srcs = glob(["*.bzl"]),
    visibility = ["//:__subpackages__"],
    deps = [
        "@toml.bzl//:toml",
    ],
)
