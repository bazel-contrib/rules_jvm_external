load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

genrule(
    name = "extract_manifest",
    srcs = [
        "@maven//:com_google_guava_guava",
    ],
    outs = [
        "MANIFEST.MF",
    ],
    cmd = "$(JAVABASE)/bin/jar xvf $< META-INF/MANIFEST.MF && cat META-INF/MANIFEST.MF | grep -E '^Bundle-Version|Target-Label' | sort > $@",
    toolchains = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
)

diff_test(
    name = "diff_manifest_test",
    file1 = "MANIFEST.MF.golden",
    file2 = "MANIFEST.MF",
)
