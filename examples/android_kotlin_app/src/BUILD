load("@rules_kotlin//kotlin:android.bzl", "kt_android_library")

kt_android_library(
    name = "lib",
    srcs = glob(["java/com/example/bazel/*.kt"]),
    custom_package = "com.example.bazel",
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    visibility = ["//:__pkg__"],
    deps = [
        "@maven//:androidx_activity_activity",
    ],
)
