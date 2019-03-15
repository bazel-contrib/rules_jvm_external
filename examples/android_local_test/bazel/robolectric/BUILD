load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "robolectric",
    data = [":robolectric-deps.properties"],
    exports = [
        artifact("androidx.test.ext:junit"),
        artifact("org.robolectric:robolectric"),
        artifact("org.robolectric:android-all"),
    ],
    visibility = ["//src/test:__subpackages__"],
)

filegroup(
    name = "android_all_jars",
    srcs = [
        artifact("org.robolectric:android-all"),
    ],
)

genrule(
    name = "gen_properties",
    srcs = [":android_all_jars"],
    outs = ["robolectric-deps.properties"],
    cmd = "$(location :gen_deps) $(locations :android_all_jars) > $@",
    tools = [":gen_deps"],
)

py_binary(
    name = "gen_deps",
    srcs = ["gen_deps.py"],
)
