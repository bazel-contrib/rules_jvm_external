load("@rules_java//java:defs.bzl", "java_library")
load("//:defs.bzl", "java_export")

# the apps-docs target previously failed with this setup.
# now it succeeds.
# todo: how to wrap this in a test target?
java_export(
    name = "app",
    srcs = ["EntryPoint.java"],
    maven_coordinates = "com.example:app:0.0.1",
    deps = [
        ":compile_only_lib",
    ],
)

java_library(
    name = "compile_only_lib",
    srcs = ["CompileOnlyLibrary.java"],
    neverlink = True,
    tags = [
        "maven:compile-only",
        "maven_coordinates=com.example:compile-lib:0.0.1",
    ],
    visibility = ["//visibility:public"],
    deps = [
        # This is a neverlink dep.
        "@regression_testing_coursier//:com_squareup_javapoet",
    ],
)
