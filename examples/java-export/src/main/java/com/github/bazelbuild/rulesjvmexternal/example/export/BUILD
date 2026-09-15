load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "artifact")
load("@rules_pkg//pkg:tar.bzl", "pkg_tar")

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
