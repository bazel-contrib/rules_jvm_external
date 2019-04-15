workspace(name = "rules_jvm_external")

android_sdk_repository(name = "androidsdk")

local_repository(
    name = "rules_jvm_external",
    path = ".",
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Begin test dependencies

load("//:defs.bzl", "maven_install")
load("//:specs.bzl", "maven")

maven_install(
    artifacts = [
        "org.hamcrest:hamcrest-core:2.1",
    ],
    repositories = [
        "https://jcenter.bintray.com/",
    ],
)

maven_install(
    name = "unsafe_shared_cache",
    artifacts = [
        "com.google.guava:guava:27.0-jre",
    ],
    fetch_sources = True,
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    use_unsafe_shared_cache = True,
)

# These artifacts helped discover limitations by the Maven resolver. Each
# artifact listed here *must have* an accompanying issue. We build_test these
# targets to ensure that they remain supported by the rule.
maven_install(
    name = "regression_testing",
    artifacts = [
        # https://github.com/bazelbuild/rules_jvm_external/issues/74
        "org.pantsbuild:jarjar:1.6.6",
        # https://github.com/bazelbuild/rules_jvm_external/issues/59
        "junit:junit:4.12",
        # https://github.com/bazelbuild/rules_jvm_external/issues/101
        "com.digitalasset:damlc:jar:osx:100.12.1",
        "org.jetbrains.kotlin:kotlin-test:1.3.21",
        # For artifact exclusion testing
        maven.artifact(
            group = "com.google.guava",
            artifact = "guava",
            version = "27.0-jre",
            exclusions = [
                maven.exclusion(group = "org.codehaus.mojo", artifact = "animal-sniffer-annotations"),
                "com.google.j2objc:j2objc-annotations",
            ],
        ),
        # https://github.com/bazelbuild/rules_jvm_external/issues/92#issuecomment-478430167 
        maven.artifact("com.squareup", "javapoet", "1.11.1", neverlink = True)
        # https://github.com/bazelbuild/rules_jvm_external/issues/98
        "com.github.fommil.netlib:all:1.1.2",
        "nz.ac.waikato.cms.weka:weka-stable:3.8.1",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://digitalassetsdk.bintray.com/DigitalAssetSDK",
    ],
)

RULES_KOTLIN_VERSION = "da1232eda2ef90d4375e2d1677b32c7ddf09e8a1"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = "0bbb0e5e536f0c775f37bded59d4f8cfb8556e6c3d926fcc0f58bf3489bff470",
    strip_prefix = "rules_kotlin-%s" % RULES_KOTLIN_VERSION,
    url = "https://github.com/bazelbuild/rules_kotlin/archive/%s.tar.gz" % RULES_KOTLIN_VERSION,
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

kotlin_repositories()

kt_register_toolchains()

BAZEL_SKYLIB_TAG = "0.7.0"

http_archive(
    name = "bazel_skylib",
    sha256 = "2c62d8cd4ab1e65c08647eb4afe38f51591f43f7f0885e7769832fa137633dcb",
    strip_prefix = "bazel-skylib-%s" % BAZEL_SKYLIB_TAG,
    url = "https://github.com/bazelbuild/bazel-skylib/archive/%s.tar.gz" % BAZEL_SKYLIB_TAG,
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

# End test dependencies
