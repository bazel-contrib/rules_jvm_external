#load("@gmaven_rules//:defs.bzl", "gmaven_artifact")
load("@rules_maven//:defs.bzl", "artifact")

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
        artifact("androidx.test.espresso:espresso-core:3.1.1"),
        artifact("androidx.test:runner:1.1.1"),
        artifact("androidx.test.ext:junit:1.1.0"),
        artifact("junit:junit:4.12"),
        artifact("com.google.inject:guice:4.0"),
        artifact("org.hamcrest:java-hamcrest:2.0.0.0"),
    ],
)
