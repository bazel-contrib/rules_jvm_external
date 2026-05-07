load("@rules_java//java:java_test.bzl", "java_test")
load("//:defs.bzl", "artifact")

java_test(
    name = "BomResolverTest",
    size = "medium",
    srcs = ["BomResolverTest.java"],
    test_class = "com.github.bazelbuild.rules_jvm_external.resolver.bom.BomResolverTest",
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/bom",
        "//tests/com/github/bazelbuild/rules_jvm_external/resolver",
        artifact(
            "junit:junit",
            repository_name = "regression_testing_coursier",
        ),
    ],
)
