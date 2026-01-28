load("@rules_java//java:java_library.bzl", "java_library")
load("//private/rules:artifact.bzl", "artifact")

java_library(
    name = "resolver",
    srcs = glob(["*.java"]),
    visibility = [
        "//private/tools/java:__subpackages__",
        "//tests/com/github/bazelbuild/rules_jvm_external:__subpackages__",
    ],
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        artifact(
            "com.google.guava:guava",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven:maven-core",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven:maven-resolver-provider",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-api",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

filegroup(
    name = "all_files",
    srcs = glob(["**"]) + [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/cmd:all_files",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/events:all_files",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/gradle:all_files",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/lockfile:all_files",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/maven:all_files",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/netrc:all_files",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/remote:all_files",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/ui:all_files",
    ],
    visibility = ["//private/tools/java/com/github/bazelbuild/rules_jvm_external:__pkg__"],
)
