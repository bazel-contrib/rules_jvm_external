load("//:defs.bzl", "artifact")

java_test(
    name = "OutdatedTest",
    srcs = ["OutdatedTest.java"],
    test_class = "com.jvm.external.maven.OutdatedTest",
    deps = [
        "//private/tools/java/rules/jvm/external/maven:outdated",
        "@outdated//:org_apache_maven_maven_artifact",
        artifact("com.google.guava:guava"),
    ],
)
