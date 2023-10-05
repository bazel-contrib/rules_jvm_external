java_library(
    name = "rules_jvm_external",
    srcs = glob(["*.java"]),
    javacopts = [
        "--release",
        "11",
        "-Xlint:-options",
    ],
    visibility = [
        "//private/tools/java:__subpackages__",
        "//tests/com:__subpackages__",
    ],
)

java_library(
    name = "hasher",
    srcs = ["Hasher.java"],
    visibility = [
        "//private/tools:__subpackages__",
        "//tests/com/github/bazelbuild/rules_jvm_external:__pkg__",
    ],
)

java_binary(
    name = "hasher-tool",
    main_class = "com.github.bazelbuild.rules_jvm_external.Hasher",
    visibility = [
        "//scripts:__pkg__",
    ],
    runtime_deps = [
        ":hasher",
    ],
)
