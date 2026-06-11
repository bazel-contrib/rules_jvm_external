load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "resolver",
    srcs = [
        "src/main/java/com/github/bazelbuild/rules_jvm_external/resolver/Artifact.java",
        "src/main/java/com/github/bazelbuild/rules_jvm_external/resolver/DependencyMetadata.java",
        "src/main/java/com/github/bazelbuild/rules_jvm_external/resolver/DownloadService.java",
        "src/main/java/com/github/bazelbuild/rules_jvm_external/resolver/MetadataService.java",
        "src/main/java/com/github/bazelbuild/rules_jvm_external/resolver/ResolutionRequest.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external:coordinates",
        artifact(
            "com.google.guava:guava",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
    exports = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external:coordinates",
    ],
)
