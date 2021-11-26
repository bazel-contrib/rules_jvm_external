load("//:defs.bzl", "maven_install")

_DEFAULT_REPOSITORIES = [
    "https://repo1.maven.org/maven2",
    "https://maven.google.com",
]

_JUNIT_PLATFORM_VERSION = "1.7.0"
_JUPITER_VERSION = "5.7.0"

_JUNIT_RUNNER_DEPS = [
    "org.junit.jupiter:junit-jupiter-engine:%s" % _JUPITER_VERSION,
    "org.junit.platform:junit-platform-launcher:%s" % _JUNIT_PLATFORM_VERSION,
    "org.junit.platform:junit-platform-reporting:%s" % _JUNIT_PLATFORM_VERSION,
]

_JUNIT_DEPS = [
    "org.junit.jupiter:junit-jupiter-api:%s" % _JUPITER_VERSION,
    "org.junit.jupiter:junit-jupiter-engine:%s" % _JUPITER_VERSION,
    "org.junit.platform:junit-platform-commons:%s" % _JUNIT_PLATFORM_VERSION,
    "org.junit.platform:junit-platform-engine:%s" % _JUNIT_PLATFORM_VERSION,
] + _JUNIT_RUNNER_DEPS

def rules_jvm_external_deps(repositories = _DEFAULT_REPOSITORIES):
    maven_install(
        name = "rules_jvm_external_deps",
        artifacts = [
            "com.google.cloud:google-cloud-storage:1.113.4",
        ] + _JUNIT_DEPS,
        maven_install_json = "@rules_jvm_external//:rules_jvm_external_deps_install.json",
        fail_if_repin_required = True,
        repositories = repositories,
    )
