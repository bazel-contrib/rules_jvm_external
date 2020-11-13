load("@rules_java//java:defs.bzl", "java_binary")

java_binary(
    name = "MavenPublisher",
    srcs = ["MavenPublisher.java"],
    javacopts = [
        "-source",
        "8",
        "-target",
        "8",
    ],
    main_class = "rules.jvm.external.maven.MavenPublisher",
    visibility = ["//visibility:public"],
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
    ],
)

java_binary(
    name = "outdated",
    srcs = ["Outdated.java"],
    javacopts = [
        "-source",
        "8",
        "-target",
        "8",
    ],
    main_class = "rules.jvm.external.maven.Outdated",
    visibility = ["//visibility:public"],
    deps = [
        "@outdated//:org_apache_maven_maven_artifact",
    ],
)
