load("@rules_jvm_external//:defs.bzl", "java_export", "maven_bom")

# To export the file, run:
#
# `bazel run //:example-export.publish --define "maven_repo=file://$(pwd)/repository"`
#
# GPG signing will be enabled if you also define `gpg_sign=true`, `maven_user`, and
# `maven_password`

java_export(
    name = "example-export",
    maven_coordinates = "com.example:bazel-example:0.0.1",
    runtime_deps = [
        "//src/main/java/com/github/bazelbuild/rulesjvmexternal/example/export",
    ],
)

maven_bom(
    name = "bom",
    java_exports = [
        ":example-export",
        "//src/main/java/com/github/bazelbuild/rulesjvmexternal/example/io",
    ],
    maven_coordinates = "com.example:bazel-example-bom:0.0.1",
    dependencies_maven_coordinates = "com.example:bazel-example-dependencies:0.0.1",
)
