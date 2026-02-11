load("@rules_android//android:rules.bzl", "android_binary", "android_library")

android_library(
    name = "my_lib",
    exports = [
        "@maven//:android_arch_lifecycle_common",
        "@maven//:android_arch_lifecycle_viewmodel",
        "@maven//:androidx_test_espresso_espresso_web",
        "@maven//:com_android_support_design",
        "@maven//:junit_junit",
    ],
)

android_binary(
    name = "my_app",
    custom_package = "com.example.bazel",
    manifest = "AndroidManifest.xml",
    deps = [":my_lib"],
)
