load("@rules_shell//shell:sh_test.bzl", "sh_test")
load("//:kt_defs.bzl", "kt_jvm_export")

kt_jvm_export(
    name = "test",
    srcs = [
        "Dependency.kt",
        "Main.kt",
    ],
    maven_coordinates = "com.example:kt:1.0.0",
)

genrule(
    name = "list-deploy-env-classes",
    srcs = [
        ":test-project",
    ],
    outs = ["classes.txt"],
    cmd = "for SRC in $(SRCS); do $(location @bazel_tools//tools/zip:zipper) v $$SRC >> $@; done",
    tools = ["@bazel_tools//tools/zip:zipper"],
)

sh_test(
    name = "check-deploy-env",
    srcs = [
        "check-deploy-env.sh",
    ],
    data = [
        ":classes.txt",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)
