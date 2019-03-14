load("@bazel_common//tools/maven:pom_file.bzl", "pom_file")
load("@rules_jvm_external//:defs.bzl", "artifact")

pom_file(
    name = "guice_pom",
    targets = [
        artifact("com.google.inject:guice"),
    ],
    template_file = "pom_template.xml",
)

java_binary(
    name = "my_binary",
    srcs = ["java/com/example/bazel/Main.java"],
    main_class = "com.example.bazel",
    deps = [
        artifact("com.google.inject:guice"),
        artifact("com.google.guava:guava"),
    ],
)

pom_file(
    name = "my_binary_pom",
    targets = [
      "//:my_binary"
    ],
    template_file = "pom_template.xml",
)
