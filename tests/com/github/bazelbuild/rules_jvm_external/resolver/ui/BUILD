load("@rules_java//java:java_test.bzl", "java_test")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_test(
    name = "AnsiConsoleListenerTest",
    size = "small",
    srcs = ["AnsiConsoleListenerTest.java"],
    test_class = "com.github.bazelbuild.rules_jvm_external.resolver.ui.AnsiConsoleListenerTest",
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/ui",
        artifact(
            "junit:junit",
            repository_name = "regression_testing_coursier",
        ),
    ],
)
