load("//:defs.bzl", "artifact")

java_test(
    name = "OutdatedTest",
    srcs = ["OutdatedTest.java"],
    resources = [
        ":maven-metadata-javax-inject.xml",
        ":maven-metadata-multiple-versions.xml",
    ],
    test_class = "com.jvm.external.maven.OutdatedTest",
    deps = [
        "//private/tools/java/rules/jvm/external/maven:outdated",
        artifact("com.google.guava:guava"),
        artifact(
            "junit:junit",
            repository_name = "regression_testing",
        ),
        artifact(
            "org.apache.maven:maven-artifact",
            repository_name = "outdated",
        ),
        artifact("org.hamcrest:hamcrest"),
    ],
)
