load("//:defs.bzl", "artifact", "java_export")
load("//private/rules:maven_project_jar.bzl", "maven_project_jar")

java_test(
    name = "PublishShapeTest",
    srcs = ["PublishShapeTest.java"],
    data = [
        "//private/tools/java/rules/jvm/external/maven:MavenPublisher_deploy.jar",
    ],
    jvm_flags = [
        "-Ddeploy.jar=$(location //private/tools/java/rules/jvm/external/maven:MavenPublisher_deploy.jar)",
    ],
    test_class = "com.jvm.external.jvm_export.PublishShapeTest",
    deps = [
        "//private/tools/java/rules/jvm/external/maven:MavenPublisher_deploy.jar",
        artifact("com.google.guava:guava"),
        artifact("org.hamcrest:hamcrest"),
    ],
)

java_test(
    name = "ProjectJarTest",
    srcs = ["ProjectJarTest.java"],
    data = [
        ":project",
    ],
    jvm_flags = [
        "-Dlocation=\"$(location :project)\"",
    ],
    test_class = "com.jvm.external.jvm_export.ProjectJarTest",
    deps = [
        ":project",
        artifact("com.google.guava:guava"),
        artifact("org.hamcrest:hamcrest"),
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

java_export(
    name = "deploy-env",
    srcs = [
        "Main.java",
    ],
    deploy_env = [
        ":dep",
    ],
    maven_coordinates = "com.example:lib:1.0.0",
    deps = [
        ":dep",
    ],
)

genrule(
    name = "list-deploy-env-classes",
    srcs = [
        ":deploy-env-project",
    ],
    outs = ["classes.txt"],
    cmd = "for SRC in $(SRCS); do jar tf $$SRC >> $@; done",
)

sh_test(
    name = "check-deploy-env",
    srcs = [
        "check-deploy-env.sh",
    ],
    data = [
        ":classes.txt",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)
