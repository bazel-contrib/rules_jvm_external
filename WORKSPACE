workspace(name = "rules_jvm_external")

android_sdk_repository(name = "androidsdk")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Begin Skylib dependencies

BAZEL_SKYLIB_TAG = "0.8.0"

http_archive(
    name = "bazel_skylib",
    sha256 = "2ea8a5ed2b448baf4a6855d3ce049c4c452a6470b1efd1504fdb7c1c134d220a",
    strip_prefix = "bazel-skylib-%s" % BAZEL_SKYLIB_TAG,
    url = "https://github.com/bazelbuild/bazel-skylib/archive/%s.tar.gz" % BAZEL_SKYLIB_TAG,
)

# End Skylib dependencies

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "io_bazel_skydoc",
    remote = "https://github.com/bazelbuild/skydoc.git",
    commit = "e235d7d6dec0241261bdb13d7415f3373920e6fd",
    shallow_since = "1554317371 -0400",
)

# Stardoc also depends on skydoc_repositories, rules_sass, rules_nodejs, but our
# usage of Stardoc (scripts/generate_docs) doesn't require any of these
# dependencies. So, we omit them to keep the WORKSPACE file simpler.
# https://skydoc.bazel.build/docs/getting_started_stardoc.html

# Begin test dependencies

load("//:defs.bzl", "maven_install")
load("//:specs.bzl", "maven")

maven_install(
    artifacts = [
        "com.google.guava:guava:27.0-jre",
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

maven_install(
    name = "exclusion_testing",
    artifacts = [
        maven.artifact(
            group = "com.google.guava",
            artifact = "guava",
            version = "27.0-jre",
            exclusions = [
                maven.exclusion(
                    group = "org.codehaus.mojo",
                    artifact = "animal-sniffer-annotations",
                ),
                "com.google.j2objc:j2objc-annotations",
            ],
        ),
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "global_exclusion_testing",
    artifacts = [
        "com.google.guava:guava:27.0-jre",  # depends on animal-sniffer-annotations and j2objc-annotations
        "com.squareup.okhttp3:okhttp:3.14.1",  # depends on animal-sniffer-annotations
        "com.diffplug.durian:durian-core:1.2.0",  # depends on animal-sniffer-annotations and j2objc-annotations
    ],
    excluded_artifacts = [
        maven.exclusion(
            group = "org.codehaus.mojo",
            artifact = "animal-sniffer-annotations",
        ),
        "com.google.j2objc:j2objc-annotations",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
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
        "org.jetbrains.kotlin:kotlin-test:1.3.21",
        # https://github.com/bazelbuild/rules_jvm_external/issues/101
        "com.digitalasset:damlc:jar:osx:100.12.1",
        # https://github.com/bazelbuild/rules_jvm_external/issues/116
        "org.eclipse.jetty.orbit:javax.servlet:3.0.0.v201112011016",
        # https://github.com/bazelbuild/rules_jvm_external/issues/92#issuecomment-478430167
        maven.artifact(
            "com.squareup",
            "javapoet",
            "1.11.1",
            neverlink = True,
        ),
        # https://github.com/bazelbuild/rules_jvm_external/issues/98
        "com.github.fommil.netlib:all:1.1.2",
        "nz.ac.waikato.cms.weka:weka-stable:3.8.1",
        # https://github.com/bazelbuild/rules_jvm_external/issues/111
        "com.android.support:appcompat-v7:aar:28.0.0",
        "com.google.android.gms:play-services-base:16.1.0",
        # https://github.com/bazelbuild/rules_jvm_external/issues/119#issuecomment-484278260
        "org.apache.flink:flink-test-utils_2.12:1.8.0",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://digitalassetsdk.bintray.com/DigitalAssetSDK",
        "https://maven.google.com",
    ],
    generate_compat_repositories = True,
)

load("@regression_testing//:compat.bzl", "compat_repositories")
compat_repositories()

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

# End test dependencies
