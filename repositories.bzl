load("//:defs.bzl", "maven_install")

def rules_jvm_external_deps():
    maven_install(
        name = "rules_jvm_external_deps",
        artifacts = [
        ],
        maven_install_json = "@rules_jvm_external//:rules_jvm_external_deps_install.json",
        repositories = [
            "https://repo1.maven.org/maven2",
            "https://jcenter.bintray.com/",
            "https://maven.google.com",
        ],
    )
