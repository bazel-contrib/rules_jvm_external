load("@rules_jvm_external//:defs.bzl", "artifact")

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
        artifact(
            "com.google.cloud:google-cloud-storage",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "com.google.cloud:google-cloud-core",
            repository_name = "rules_jvm_external_deps",
        ),
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
