load("//:defs.bzl", "artifact")

java_test(
    name = "OutdatedTest",
    srcs = ["OutdatedTest.java"],
    data = [
        "//tests/com/jvm/external/maven/resources",
    ],
    resources = [
        "//tests/com/jvm/external/maven/resources",
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
            repository_name = "rules_jvm_external_deps",
        ),
        artifact("org.hamcrest:hamcrest"),
    ],
)
