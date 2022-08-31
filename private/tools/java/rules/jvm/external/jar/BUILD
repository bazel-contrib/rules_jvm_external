load("@rules_jvm_external//:defs.bzl", "artifact")

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

java_library(
    name = "ListPackages-lib",
    srcs = ["ListPackages.java"],
    visibility = [
        "//tests/com/jvm/external/jar:__pkg__",
    ],
    deps = [
        artifact(
            "com.google.code.gson:gson",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

java_binary(
    name = "ListPackages",
    main_class = "rules.jvm.external.jar.ListPackages",
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":ListPackages-lib",
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
