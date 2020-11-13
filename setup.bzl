load("@rules_java//java:repositories.bzl", "rules_java_dependencies", "rules_java_toolchains")

def rules_jvm_external_setup():
    rules_java_dependencies()
    rules_java_toolchains()
