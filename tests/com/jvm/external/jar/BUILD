load("//:defs.bzl", "artifact")

java_test(
    name = "AddJarManifestEntryTest",
    srcs = ["AddJarManifestEntryTest.java"],
    test_class = "com.jvm.external.jar.AddJarManifestEntryTest",
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
        "//private/tools/java/rules/jvm/external/jar:AddJarManifestEntry",
        artifact("com.google.guava:guava"),
    ],
)

java_test(
    name = "MergeJarsTest",
    srcs = ["MergeJarsTest.java"],
    test_class = "com.jvm.external.jar.MergeJarsTest",
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
        "//private/tools/java/rules/jvm/external/jar:MergeJars",
        "//private/tools/java/rules/jvm/external/zip",
        artifact("com.google.guava:guava"),
    ],
)
