load("//:defs.bzl", "maven_install")

_DEFAULT_REPOSITORIES = [
    "https://repo1.maven.org/maven2",
    "https://maven.google.com",
]

def rules_jvm_external_deps(repositories = _DEFAULT_REPOSITORIES):
    maven_install(
        name = "rules_jvm_external_deps",
        artifacts = [
            "com.google.cloud:google-cloud-storage:1.113.4",
        ],
        maven_install_json = "@rules_jvm_external//:rules_jvm_external_deps_install.json",
        fail_if_repin_required = True,
        repositories = repositories,
    )
