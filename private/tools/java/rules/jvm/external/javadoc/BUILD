java_binary(
    name = "javadoc",
    srcs = glob(["*.java"]),
    javacopts = [
        "-source",
        "8",
        "-target",
        "8",
    ],
    main_class = "rules.jvm.external.javadoc.JavadocJarMaker",
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
        "//private/tools/java/rules/jvm/external/zip",
    ],
)
