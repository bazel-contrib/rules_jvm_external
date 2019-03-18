load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library", "kt_jvm_test")
load("@rules_jvm_external//:defs.bzl", "artifact")

kt_android_library(
    name = "lib",
    srcs = glob(["java/com/example/bazel/*.kt"]),
    manifest = "AndroidManifest.xml",
    custom_package = "com.example.bazel",
    resource_files = glob(["res/**"]),
    deps = [
        "@maven//:androidx_appcompat_appcompat",
        "@maven//:androidx_core_core",
        "@maven//:androidx_core_core_ktx",
        "@maven//:androidx_drawerlayout_drawerlayout",
        "@maven//:androidx_fragment_fragment",
        "@maven//:androidx_lifecycle_lifecycle_common",
        "@maven//:androidx_lifecycle_lifecycle_viewmodel",
    ],
    visibility = ["//:__pkg__"]
)

kt_jvm_test(
    name = "inline_function_test",
    srcs = ["javatests/com/example/bazel/InlineFunctionTest.kt"],
    test_class = "com.example.bazel.InlineFunctionTest",
    deps = [
        "@maven//:junit_junit",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
    ]
)
