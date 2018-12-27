load("@rules_maven//:defs.bzl", "artifact")

android_library(
    name = "my_lib",
    exports = [
        artifact("android.arch.lifecycle:common:1.1.1"),
        artifact("android.arch.lifecycle:viewmodel:1.1.1"),
        artifact("androidx.test.espresso:espresso-web:3.1.1"),
        artifact("junit:junit:4.12"),
        artifact("com.android.support:design:27.0.2"),
    ],
)

android_binary(
    name = "my_app",
    custom_package = "com.example.bazel",
    manifest = "AndroidManifest.xml",
    deps = [":my_lib"],
)
