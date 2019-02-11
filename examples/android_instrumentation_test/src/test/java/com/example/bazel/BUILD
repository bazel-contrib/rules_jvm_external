load("@gmaven_rules//:defs.bzl", "library", "artifact")

android_library(
    name = "greeter_test_lib",
    srcs = ["GreeterTest.java"],
    custom_package = "com.example.bazel.test",
    visibility = ["//src/test:__subpackages__"],
    deps = [
        ":test_deps",
        "//src/main/java/com/example/bazel:greeter_activity",
    ],
)

android_library(
    name = "test_deps",
    exports = [
        library("androidx.test.espresso:espresso-core"),
        artifact("androidx.test.ext:junit"),
    ],
)
