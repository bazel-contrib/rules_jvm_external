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
