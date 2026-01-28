load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "implementation",
    srcs = glob(["*.bzl"]),
    visibility = ["//:__subpackages__"],
)

filegroup(
    name = "all_files",
    srcs = glob(["**"]) + [
        "//private/extensions:all_files",
        "//private/lib:all_files",
        "//private/rules:all_files",
        "//private/templates:all_files",
        "//private/tools:all_files",
    ],
    visibility = ["//:__pkg__"],
)
