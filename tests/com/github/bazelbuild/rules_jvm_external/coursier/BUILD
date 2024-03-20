load("//:defs.bzl", "artifact")

java_test(
    name = "NebulaFormatTest",
    srcs = ["NebulaFormatTest.java"],
    test_class = "com.github.bazelbuild.rules_jvm_external.coursier.NebulaFormatTest",
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/coursier",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver",
        artifact(
            "com.google.guava:guava",
            repository_name = "exclusion_testing",
        ),
        artifact(
            "junit:junit",
            repository_name = "regression_testing",
        ),
        artifact("org.hamcrest:hamcrest"),
        artifact("org.hamcrest:hamcrest_core"),
    ],
)
