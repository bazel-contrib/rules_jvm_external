load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "pom_file")

java_library(
    name = "my_library",
    srcs = ["java/com/example/bazel/Main.java"],
    tags = ["maven_coordinates=com.example:my_library:1.0.0"],
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_inject_guice",
    ],
)

pom_file(
    name = "my_library_pom",
    target = "//:my_library",
    pom_template = "pom_template.xml",
)
