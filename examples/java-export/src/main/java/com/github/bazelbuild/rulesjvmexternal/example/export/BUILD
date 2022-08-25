load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "export",
    srcs = glob(["*.java"]),
    visibility = [
        "//:__pkg__",
    ],
    deps = [
        "//src/main/proto",
        artifact("com.google.guava:guava"),
    ],
)
