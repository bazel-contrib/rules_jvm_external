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

java_test(
    name = "LockFileConverterTest",
    size = "small",
    srcs = ["LockFileConverterTest.java"],
    test_class = "com.github.bazelbuild.rules_jvm_external.coursier.LockFileConverterTest",
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/coursier",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver",
        artifact(
            "com.google.code.gson:gson",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "junit:junit",
            repository_name = "regression_testing_coursier",
        ),
    ],
)
