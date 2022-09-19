load("//private:versions.bzl", "COURSIER_CLI_HTTP_FILE_NAME")
load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary")

sh_binary(
    name = "mirror_coursier",
    srcs = [":mirror_coursier.sh"],
    data = ["@" + COURSIER_CLI_HTTP_FILE_NAME + "//file"],
    visibility = ["//:__pkg__"],
)

genrule(
    name = "generate_base_docs",
    srcs = [
        "//:docs/includes/main_functions_header.md",
        "//:defs.md",
        "//:docs/includes/spec_functions_header.md",
        "//:specs.md",
    ],
    outs = ["generate_base_docs.md"],
    cmd = """cat \
    $(location //:docs/includes/main_functions_header.md) \
    $(location //:defs.md) \
    $(location //:docs/includes/spec_functions_header.md) \
    $(location //:specs.md) > $@""",
)

genrule(
    name = "generate_api_reference",
    srcs = [":generate_base_docs"],
    outs = ["api.md"],
    cmd = "cp $(location :generate_base_docs) $@; chmod +w $@; $(location :doctoc) --title '# API Reference' $@",
    tools = [":doctoc"],
    visibility = ["//:__pkg__"],
)

nodejs_binary(
    name = "doctoc",
    data = ["@npm//doctoc"],
    entry_point = {"@npm//:node_modules/doctoc": "doctoc.js"},
)
