load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

# Minimise the risk of accidentally depending on something that's not already loaded
load("//private/rules:maven_install.bzl", "maven_install")

_DEFAULT_REPOSITORIES = [
    "https://repo1.maven.org/maven2",
    "https://maven.google.com",
]

def rules_jvm_external_deps(repositories = _DEFAULT_REPOSITORIES):
    maybe(
        http_archive,
        name = "rules_java",
        urls = [
            "https://github.com/bazelbuild/rules_java/releases/download/6.3.1/rules_java-6.3.1.tar.gz",
        ],
        sha256 = "117a1227cdaf813a20a1bba78a9f2d8fb30841000c33e2f2d2a640bd224c9282",
    )

    maybe(
        http_archive,
        name = "bazel_skylib",
        sha256 = "66ffd9315665bfaafc96b52278f57c7e2dd09f5ede279ea6d39b2be471e7e3aa",
        urls = [
            "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.4.2/bazel-skylib-1.4.2.tar.gz",
            "https://github.com/bazelbuild/bazel-skylib/releases/download/1.4.2/bazel-skylib-1.4.2.tar.gz",
        ],
    )

    maven_install(
        name = "rules_jvm_external_deps",
        artifacts = [
            "com.google.auth:google-auth-library-credentials:1.19.0",
            "com.google.auth:google-auth-library-oauth2-http:1.19.0",
            "com.google.cloud:google-cloud-core:2.22.0",
            "com.google.cloud:google-cloud-storage:2.26.1",
            "com.google.code.gson:gson:2.10.1",
            "com.google.googlejavaformat:google-java-format:1.17.0",
            "com.google.guava:guava:32.1.2-jre",
            "org.apache.maven:maven-artifact:3.9.4",
            "software.amazon.awssdk:s3:2.20.128",
        ],
        maven_install_json = "@rules_jvm_external//:rules_jvm_external_deps_install.json",
        fail_if_repin_required = True,
        strict_visibility = True,
        repositories = repositories,
    )
