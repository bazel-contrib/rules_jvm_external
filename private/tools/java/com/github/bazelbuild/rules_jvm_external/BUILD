load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "coordinates",
    srcs = ["Coordinates.java"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "rules_jvm_external",
    srcs = [
        "ByteStreams.java",
        "Hasher.java",
    ],
    visibility = [
        "//private/tools/java:__subpackages__",
        "//resolver:__subpackages__",
        "//tests/com:__subpackages__",
    ],
    exports = [
        ":coordinates",
    ],
    deps = [
        ":coordinates",
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
