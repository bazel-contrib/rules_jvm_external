load("@bazel_tools//tools/build_defs/pkg:pkg.bzl", "pkg_tar")
load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "export",
    srcs = glob(["*.java"]),
    visibility = [
        "//:__pkg__",
    ],
    deps = [
        "//src/main/proto",
        artifact("com.google.guava:guava"),
    ],
)

pkg_tar(
    name = "tar",
    srcs = glob(["*"]),
    visibility = [
        "//:__pkg__",
    ],
)
