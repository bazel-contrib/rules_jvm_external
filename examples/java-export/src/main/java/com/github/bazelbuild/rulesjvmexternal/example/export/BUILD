load("@rules_jvm_external//:defs.bzl", "artifact", "java_export")

java_library(
    name = "export",
    srcs = glob(["Main.java"]),
    deps = [
        "//src/main/proto",
        artifact("com.google.guava:guava"),
    ],
    visibility = [
      "//:__pkg__",
    ],
)
