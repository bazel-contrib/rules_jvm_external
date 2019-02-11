load("@gmaven_rules//:defs.bzl", "artifact")

exports_files(["defs.bzl"])

licenses(["notice"]) # Apache 2.0

java_binary(
    name = "gmaven_to_bazel",
    srcs = ["java/com/google/gmaven/GMavenToBazel.java"],
    main_class = "com.google.gmaven.GMavenToBazel",

android_library(
    name = "my_lib",
    exports = [
        artifact("android.arch.lifecycle:common:1.1.1"),
        artifact("android.arch.lifecycle:viewmodel:1.1.1"),
        artifact("androidx.test.espresso:espresso-web:3.1.1"),
        artifact("junit:junit:4.12"),
        # Buggy, need to use aar_import.deps instead of uber re-export
        # artifact("com.android.support:design:27.0.2"),
    ],
)

android_binary(
    name = "my_app",
    custom_package = "com.example.bazel",
    manifest = "AndroidManifest.xml",
    deps = [":my_lib"],
)
