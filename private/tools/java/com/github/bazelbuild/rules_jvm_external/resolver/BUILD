load("@rules_java//java:java_library.bzl", "java_library")
load("//private/rules:artifact.bzl", "artifact")


java_library(
    name = "resolver",
    srcs = [
        "Conflict.java",
        "DependencyInfo.java",
        "PackagingMappings.java",
        "ResolutionResult.java",
        "ResolvedArtifact.java",
        "Resolver.java",
        "SpiLoader.java",
    ],
    visibility = [
        "//private/tools/java:__subpackages__",
        "//tests/com/github/bazelbuild/rules_jvm_external:__subpackages__",
    ],
    deps = [
        "//resolver:resolver",
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
