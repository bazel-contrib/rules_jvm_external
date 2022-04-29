java_binary(
    name = "AddJarManifestEntry",
    srcs = ["AddJarManifestEntry.java"],
    main_class = "rules.jvm.external.jar.AddJarManifestEntry",
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
    ],
)

java_binary(
    name = "MergeJars",
    srcs = [
        "DuplicateEntryStrategy.java",
        "MergeJars.java",
    ],
    main_class = "rules.jvm.external.jar.MergeJars",
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//private/tools/java/rules/jvm/external:byte-streams",
    ],
)
