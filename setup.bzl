load("@rules_jvm_external_deps//:defs.bzl", "pinned_maven_install")

def rules_jvm_external_setup():
    pinned_maven_install()
