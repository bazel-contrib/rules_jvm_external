load("@gmaven_rules//:defs.bzl", "artifact")

android_library(
    name = "my_lib",
    exports = [
        artifact("android.arch.lifecycle:common"),
        artifact("android.arch.lifecycle:viewmodel"),
        artifact("androidx.test.espresso:espresso-web"),
        artifact("junit:junit"),
        artifact("com.android.support:design"),
    ],
)

android_binary(
    name = "my_app",
    custom_package = "com.example.bazel",
    manifest = "AndroidManifest.xml",
    deps = [":my_lib"],
)
