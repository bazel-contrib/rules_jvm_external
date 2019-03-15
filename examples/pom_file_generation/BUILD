load("@bazel_common//tools/maven:pom_file.bzl", "pom_file")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "my_library",
    srcs = ["java/com/example/bazel/Main.java"],
    deps = [
        artifact("com.google.inject:guice"),
        artifact("com.google.guava:guava"),
    ],
)

pom_file(
    name = "my_library_pom",
    targets = [
        "//:my_library",
    ],
    template_file = "pom_template.xml",
)
