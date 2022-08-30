load("//:defs.bzl", "artifact")

java_test(
    name = "AddJarManifestEntryTest",
    srcs = ["AddJarManifestEntryTest.java"],
    test_class = "com.jvm.external.jar.AddJarManifestEntryTest",
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
        "//private/tools/java/rules/jvm/external/jar:AddJarManifestEntry",
        artifact("com.google.guava:guava"),
        artifact(
            "junit:junit",
            repository_name = "regression_testing",
        ),
    ],
)

java_test(
    name = "ListPackagesTest",
    srcs = ["ListPackagesTest.java"],
    data = [
        "@gson_for_test//file",
        "@hamcrest_core_for_test//file",
        "@hamcrest_core_srcs_for_test//file",
        "@junit_platform_commons_for_test//file",
    ],
    test_class = "com.jvm.external.jar.ListPackagesTest",
    deps = [
        "//private/tools/java/rules/jvm/external/jar:ListPackages-lib",
        "@bazel_tools//tools/java/runfiles",
        artifact(
            "junit:junit",
            repository_name = "regression_testing",
        ),
        artifact("org.hamcrest:hamcrest"),
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
        artifact(
            "junit:junit",
            repository_name = "regression_testing",
        ),
    ],
)
