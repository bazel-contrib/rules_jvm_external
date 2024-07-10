alias(
    name = "selenium-java",
    actual = "@maven//:org_seleniumhq_selenium_selenium_java",
)

sh_binary(
    name = "refreeze",
    srcs = ["refreeze.sh"],
    args = [
        "$(rlocationpath @to_freeze//:freeze)",
    ],
    data = [
        "@to_freeze//:freeze",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)
