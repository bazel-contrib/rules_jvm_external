load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")
load("@rules_jvm_external_deps//:defs.bzl", "pinned_maven_install")
load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")

def rules_jvm_external_setup():
    rules_java_dependencies()
    rules_java_toolchains()

    bazel_skylib_workspace()
    pinned_maven_install()
