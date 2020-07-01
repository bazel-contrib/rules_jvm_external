load("//:defs.bzl", "artifact")

java_test(
    name = "MergeJarsTest",
    test_class = "com.jvm.external.jar.MergeJarsTest",
    srcs = ["MergeJarsTest.java"],
    deps = [
        "//private/tools/java/rules/jvm/external/jar:MergeJars",
        "//private/tools/java/rules/jvm/external/zip",
        artifact("com.google.guava:guava"),
    ]
)
