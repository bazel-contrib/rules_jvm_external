java_library(
    name = "byte-streams",
    srcs = ["ByteStreams.java"],
    visibility = [
        "//private/tools/java/rules/jvm/external:__subpackages__",
        "//tests/com/jvm/external:__subpackages__",
    ],
)

java_library(
    name = "hasher",
    srcs = ["Hasher.java"],
    visibility = [
        "//private/tools:__subpackages__",
        "//tests/com/jvm/external:__pkg__",
    ],
)

java_binary(
    name = "hasher-tool",
    visibility = [
        "//scripts:__pkg__",
    ],
    runtime_deps = [
        ":hasher",
    ],
)
