load("@rules_jvm_external//:defs.bzl", "artifact")

android_local_test(
    name = "main_activity_test",
    srcs = ["java/com/example/bazel/MainActivityTest.java"],
    manifest = "AndroidManifest.xml",
    test_class = "com.example.bazel.MainActivityTest",
    deps = [
        "//bazel/robolectric",
        "//src/main:my_lib",
        artifact("org.robolectric:shadows-framework"),
        artifact("org.assertj:assertj-core"),
    ],
)
