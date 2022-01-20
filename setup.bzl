load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")
load("@rules_jvm_external_deps//:defs.bzl", "pinned_maven_install")

def rules_jvm_external_setup():
    pinned_maven_install()
    kotlin_repositories()
    kt_register_toolchains()
