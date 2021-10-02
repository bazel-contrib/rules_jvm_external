load("@bazel_skylib//rules:build_test.bzl", "build_test")
load(":aar_import_test.bzl", "aar_import_test_suite")

build_test(
    name = "starlark_aar_import_test",
    targets = [
        "@starlark_aar_import_test//:com_android_support_appcompat_v7_28_0_0",
    ],
)

build_test(
    name = "starlark_aar_import_with_jetify_test",
    targets = [
        "@starlark_aar_import_with_jetify_test//:com_android_support_appcompat_v7_28_0_0",
    ],
)

aar_import_test_suite(
    name = "starlark_aar_import_with_srcjar_tests",
)
