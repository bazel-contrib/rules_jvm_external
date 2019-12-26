load("@bazel_skylib//rules:build_test.bzl", "build_test")

build_test(
    name = "jetified_artifacts",
    targets = [
        "@jetify_test//:com_google_guava_guava",
        "@jetify_test//:com_android_support_appcompat_v7",
    ],
)

genrule(
    name = "jetified_support_library_classes",
    srcs = ["@jetify_test//:jetified_com_android_support_appcompat_v7"],
    outs = ["jetified_classes.jar"],
    cmd = "unzip -p $< classes.jar > $@",
)

sh_test(
    name = "test_jetified_classes_jar",
    size = "small",
    srcs = ["jetifier_test.sh"],
    data = [":jetified_classes.jar"],
    deps = ["@bazel_tools//tools/bash/runfiles"],
)
