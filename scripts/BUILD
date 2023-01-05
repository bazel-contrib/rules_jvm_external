load("//private:versions.bzl", "COURSIER_CLI_HTTP_FILE_NAME")
load("//private/rules:artifact.bzl", "artifact")
load("@build_bazel_rules_nodejs//:index.bzl", "nodejs_binary")

genrule(
    name = "buildifier-bin",
    srcs = select({
        "@bazel_tools//src/conditions:darwin_arm64": ["@buildifier-macos-arm64//file"],
        "@bazel_tools//src/conditions:darwin_x86_64": ["@buildifier-macos-x86_64//file"],
        "@bazel_tools//src/conditions:linux_aarch64": ["@buildifier-linux-arm64//file"],
        "@bazel_tools//src/conditions:linux_x86_64": ["@buildifier-linux-x86_64//file"],
    }),
    outs = ["buildifier"],
    cmd = "cp $< $@",
)

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

java_binary(
    name = "google-java-format",
    jvm_flags = [
        "--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    ],
    main_class = "com.google.googlejavaformat.java.Main",
    runtime_deps = [
        artifact(
            "com.google.googlejavaformat:google-java-format",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

genrule(
    name = "format",
    outs = ["run-formats.sh"],
    cmd = """cat >$@ << EOL
#!/usr/bin/env bash
set -eufo pipefail

cwd="\\$${BUILD_WORKSPACE_DIRECTORY:=\\$$(pwd)}"

# Format java code
find "\\$$cwd" -type f -name "*.java" |  xargs "$(location :google-java-format)" --replace

# Run buildifier
$(location :buildifier) --lint fix --mode fix -r "\\$$cwd"

EOL
""",
    executable = True,
    tools = [
        ":buildifier",
        ":google-java-format",
    ],
)

genrule(
    name = "refresh-prebuilts",
    srcs = [
        "//private/tools/java/rules/jvm/external:hasher-tool_deploy.jar",
        "//private/tools/java/rules/jvm/external/jar:ListPackages_deploy.jar",
        "//private/tools/java/rules/jvm/external/maven:outdated_deploy.jar",
    ],
    outs = ["refresh-prebuilts.sh"],
    cmd = """cat >$@ <<EOF
#!/usr/bin/env bash
set -eufo pipefail

root="\\$$BUILD_WORKSPACE_DIRECTORY/private/tools/prebuilt"
cp "$(location //private/tools/java/rules/jvm/external:hasher-tool_deploy.jar)" "\\$$root/hasher_deploy.jar"
cp "$(location //private/tools/java/rules/jvm/external/jar:ListPackages_deploy.jar)" "\\$$root/list_packages_deploy.jar"
cp "$(location //private/tools/java/rules/jvm/external/maven:outdated_deploy.jar)" "\\$$root/outdated_deploy.jar"

EOF
""",
    executable = True,
)
