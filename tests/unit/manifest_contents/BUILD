load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

genrule(
    name = "extract_manifest",
    srcs = [
        "@maven//:com_google_guava_guava",
    ],
    outs = [
        "MANIFEST.MF",
    ],
    cmd = "unzip -p $< META-INF/MANIFEST.MF > $@",
)

diff_test(
    name = "diff_manifest_test",
    file1 = "MANIFEST.MF.golden",
    file2 = "MANIFEST.MF",
)
