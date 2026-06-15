load("@rules_java//java:java_test.bzl", "java_test")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_test(
    name = "ResolverConfigTest",
    srcs = ["ResolverConfigTest.java"],
    test_class = "com.github.bazelbuild.rules_jvm_external.resolver.cmd.ResolverConfigTest",
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/cmd",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/ui",
        artifact(
            "junit:junit",
            repository_name = "regression_testing_coursier",
        ),
    ],
)
