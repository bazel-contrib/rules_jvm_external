load("//private/rules:artifact.bzl", "artifact")

java_library(
    name = "resolver",
    srcs = glob(["*.java"]),
    javacopts = [
        "--release",
        "8",
        "-Xlint:-options",
    ],
    visibility = [
        "//private/tools/java:__subpackages__",
    ],
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        artifact(
            "com.google.guava:guava",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)
