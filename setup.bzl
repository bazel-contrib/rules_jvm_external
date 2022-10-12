load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")
load("@rules_jvm_external_deps//:defs.bzl", "pinned_maven_install")

def rules_jvm_external_setup():
    bazel_skylib_workspace()
    pinned_maven_install()
