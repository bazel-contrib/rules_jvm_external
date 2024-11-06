load("@rules_java//java:defs.bzl", "java_library")
load("//:defs.bzl", "artifact")

java_library(
    name = "resolver",
    testonly = True,
    srcs = glob(
        ["*.java"],
        exclude = ["*Test.java"],
    ),
    visibility = [
        "//tests/com/github/bazelbuild/rules_jvm_external:__subpackages__",
    ],
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/cmd",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/events",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/netrc",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/remote",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/ui",
        artifact(
            "com.google.code.gson:gson",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "com.google.guava:guava",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "junit:junit",
            repository_name = "regression_testing_coursier",
        ),
        artifact(
            "org.apache.maven:maven-model",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

java_test(
    name = "ArtifactsHashTest",
    srcs = ["ArtifactsHashTest.java"],
    data = [
        "//tests/custom_maven_install:artifacts_hash_no_deps_install.json",
        "//tests/custom_maven_install:artifacts_hash_with_deps_from_maven_install.json",
        "//tests/custom_maven_install:artifacts_hash_with_deps_install.json",
    ],
    test_class = "com.github.bazelbuild.rules_jvm_external.resolver.ArtifactsHashTest",
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/lockfile",
        "@bazel_tools//tools/java/runfiles",
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
