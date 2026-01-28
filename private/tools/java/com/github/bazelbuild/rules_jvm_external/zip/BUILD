load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "zip",
    srcs = glob(["*.java"]),
    visibility = [
        "//visibility:public",
    ],
)

filegroup(
    name = "all_files",
    srcs = glob(["**"]),
    visibility = ["//private/tools/java/com/github/bazelbuild/rules_jvm_external:__pkg__"],
)
