load("//:defs.bzl", "artifact")

java_test(
    name = "VersionTest",
    srcs = ["VersionTest.java"],
    test_class = "rules.jvm.external.javadoc.VersionTest",
    deps = [
        "//private/tools/java/rules/jvm/external/javadoc",
        artifact(
            "junit:junit",
            repository_name = "regression_testing",
        ),
        artifact("org.hamcrest:hamcrest"),
        artifact("org.hamcrest:hamcrest-core"),
    ],
)
