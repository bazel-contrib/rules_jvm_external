java_binary(
    name = "merge_jars",
    srcs = glob(["*.java"]),
    javacopts = [
        "-source",
        "8",
        "-target",
        "8",
    ],
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
    ],
    main_class = "rules.jvm.external.jar.MergeJars",
    visibility = [
        "//visibility:public",
    ],
)
