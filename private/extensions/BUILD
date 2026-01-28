load("@bazel_skylib//:bzl_library.bzl", "bzl_library")

bzl_library(
    name = "implementation",
    srcs = glob(["*.bzl"]),
    visibility = ["//:__subpackages__"],
)

filegroup(
    name = "all_files",
    srcs = glob(["**"]),
    visibility = ["//private:__pkg__"],
)
