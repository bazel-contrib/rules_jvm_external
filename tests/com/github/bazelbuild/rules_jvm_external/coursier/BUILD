load("@rules_java//java:java_test.bzl", "java_test")
load("//:defs.bzl", "artifact")

java_test(
    name = "CoordinateResolverTest",
    size = "small",
    srcs = ["CoordinateResolverTest.java"],
    test_class = "com.github.bazelbuild.rules_jvm_external.coursier.CoordinateResolverTest",
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/coursier",
        artifact(
            "junit:junit",
            repository_name = "regression_testing_coursier",
        ),
        artifact("org.hamcrest:hamcrest"),
        artifact("org.hamcrest:hamcrest_core"),
    ],
)
