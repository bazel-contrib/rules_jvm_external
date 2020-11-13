load("@rules_java//java:defs.bzl", "java_library", "java_test")
load("//:defs.bzl", "artifact")
load("//private/rules:maven_project_jar.bzl", "maven_project_jar")

java_test(
    name = "PublishShapeTest",
    srcs = ["PublishShapeTest.java"],
    test_class = "com.jvm.external.jvm_export.PublishShapeTest",
    jvm_flags = [
        "-Ddeploy.jar=$(location //private/tools/java/rules/jvm/external/maven:MavenPublisher_deploy.jar)",
    ],
    deps = [
        "//private/tools/java/rules/jvm/external/maven:MavenPublisher_deploy.jar",
        artifact("com.google.guava:guava"),
        artifact("org.hamcrest:hamcrest"),
    ],
    data = [
        "//private/tools/java/rules/jvm/external/maven:MavenPublisher_deploy.jar",
    ],
)

java_test(
    name = "ProjectJarTest",
    srcs = ["ProjectJarTest.java"],
    test_class = "com.jvm.external.jvm_export.ProjectJarTest",
    jvm_flags = [
        "-Dlocation=\"$(location :project)\"",
    ],
    deps = [
        ":project",
        artifact("com.google.guava:guava"),
        artifact("org.hamcrest:hamcrest"),
    ],
    data = [
        ":project",
    ],
)

maven_project_jar(
    name = "project",
    target = ":main",
)

java_library(
    name = "main",
    srcs = ["Main.java"],
    tags = [
        "maven_coordinates=com.example:example:0.0.1",
    ],
    deps = [":dep"],
)

java_library(
    name = "dep",
    srcs = ["Dependency.java"],
    deps = [
        artifact("com.google.guava:guava"),
    ],
)
