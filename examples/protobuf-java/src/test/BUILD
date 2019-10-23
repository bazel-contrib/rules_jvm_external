load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

genrule(
    name = "make_person_json",
    tools = ["//src/main:example"],
    cmd = "$(location //src/main:example) > $@",
    outs = ["actual.json"],
)

diff_test(
    name = "diff_json_test",
    file1 = "golden.json",
    file2 = "actual.json",
)
