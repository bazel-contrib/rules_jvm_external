load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "plugin",
    srcs = glob(["*.java"]),
    visibility = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle:__subpackages__",
        "//tests:__subpackages__",
    ],
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle/models",
        "@gradle",
        artifact(
            "org.gradle:gradle-tooling-api",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

# Use java_binary to create a deploy jar with all transitive deps.
# This avoids java_single_jar which uses Bazel private APIs that are
# restricted in Bazel 7.
java_binary(
    name = "plugin_deploy_jar",
    main_class = "does.not.Exist",
    visibility = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle:__subpackages__",
        "//tests:__subpackages__",
    ],
    runtime_deps = [":plugin"],
)
