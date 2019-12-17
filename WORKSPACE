workspace(name = "rules_jvm_external")

android_sdk_repository(name = "androidsdk")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")
load(
    "//:private/versions.bzl",
    "COURSIER_CLI_GITHUB_ASSET_URL",
    "COURSIER_CLI_HTTP_FILE_NAME",
    "COURSIER_CLI_SHA256",
)

http_file(
    name = COURSIER_CLI_HTTP_FILE_NAME,
    sha256 = COURSIER_CLI_SHA256,
    urls = [COURSIER_CLI_GITHUB_ASSET_URL],
)

# Begin Skylib dependencies

http_archive(
    name = "bazel_skylib",
    sha256 = "97e70364e9249702246c0e9444bccdc4b847bed1eb03c5a3ece4f83dfe6abc44",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.0.2/bazel-skylib-1.0.2.tar.gz",
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.0.2/bazel-skylib-1.0.2.tar.gz",
    ],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

# End Skylib dependencies

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

http_archive(
    name = "io_bazel_stardoc",
    sha256 = "4a355dccc713458071f441f3dafd7452b3111c53cde554d0847b9a82d657149e",
    strip_prefix = "stardoc-4378e9b6bb2831de7143580594782f538f461180",
    url = "https://github.com/bazelbuild/stardoc/archive/4378e9b6bb2831de7143580594782f538f461180.zip",
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
    maven_install_json = "@rules_jvm_external//:maven_install.json",
    repositories = [
        "https://jcenter.bintray.com/",
    ],
)

load("@maven//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

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
    name = "unsafe_shared_cache_with_pinning",
    artifacts = [
        "com.google.guava:guava:27.0-jre",
    ],
    fetch_sources = True,
    maven_install_json = "//tests/custom_maven_install:unsafe_shared_cache_with_pinning_install.json",
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    use_unsafe_shared_cache = True,
)

load("@unsafe_shared_cache_with_pinning//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

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
        # https://github.com/bazelbuild/rules_jvm_external/issues/170
        "ch.epfl.scala:compiler-interface:1.3.0-M4+20-c8a2f9bd",
        # https://github.com/bazelbuild/rules_jvm_external/issues/172
        "org.openjfx:javafx-base:11.0.1",
        # https://github.com/bazelbuild/rules_jvm_external/issues/178
        "io.kubernetes:client-java:4.0.0-beta1",
        # https://github.com/bazelbuild/rules_jvm_external/issues/199
        "com.google.ar.sceneform.ux:sceneform-ux:1.10.0",
        # https://github.com/bazelbuild/rules_jvm_external/issues/119#issuecomment-504704752
        "com.github.oshi:oshi-parent:3.4.0",
        "com.github.spinalhdl:spinalhdl-core_2.11:1.3.6",
        "com.github.spinalhdl:spinalhdl-lib_2.11:1.3.6",
        # https://github.com/bazelbuild/rules_jvm_external/issues/201
        "org.apache.kafka:kafka_2.11:2.1.1",
        "io.confluent:kafka-avro-serializer:5.0.1",
        # https://github.com/bazelbuild/rules_jvm_external/issues/309
        "io.quarkus.http:quarkus-http-servlet:3.0.0.Beta1",
    ],
    generate_compat_repositories = True,
    maven_install_json = "//tests/custom_maven_install:regression_testing_install.json",
    override_targets = {
        "com.google.ar.sceneform:rendering": "@//tests/integration/override_targets:sceneform_rendering",
    },
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://digitalassetsdk.bintray.com/DigitalAssetSDK",
        "https://maven.google.com",
        "https://packages.confluent.io/maven/",
    ],
)

load("@regression_testing//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

load("@regression_testing//:compat.bzl", "compat_repositories")

compat_repositories()

maven_install(
    name = "policy_pinned_testing",
    artifacts = [
        # https://github.com/bazelbuild/rules_jvm_external/issues/107
        "com.google.cloud:google-cloud-storage:1.66.0",
        "com.google.guava:guava:25.0-android",
    ],
    maven_install_json = "//tests/custom_maven_install:policy_pinned_testing_install.json",
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
    ],
    version_conflict_policy = "pinned",
)

load(
    "@policy_pinned_testing//:defs.bzl",
    _policy_pinned_maven_install = "pinned_maven_install",
)

_policy_pinned_maven_install()

maven_install(
    name = "strict_visibility_testing",
    artifacts = [
        # https://github.com/bazelbuild/rules_jvm_external/issues/94
        "org.apache.tomcat:tomcat-catalina:9.0.24",
        # https://github.com/bazelbuild/rules_jvm_external/issues/255
        maven.artifact(
            group = "org.eclipse.jetty",
            artifact = "jetty-http",
            version = "9.4.20.v20190813",
            classifier = "tests",
        ),
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    strict_visibility = True,
)

maven_install(
    name = "maven_install_in_custom_location",
    artifacts = ["com.google.guava:guava:27.0-jre"],
    maven_install_json = "@rules_jvm_external//tests/custom_maven_install:maven_install.json",
    repositories = ["https://repo1.maven.org/maven2"],
)

load("@maven_install_in_custom_location//:defs.bzl", "pinned_maven_install")

pinned_maven_install()

RULES_KOTLIN_VERSION = "8ca948548159f288450516a09248dcfb9e957804"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = "22d7d3155b95f79e461451f565353bf0098d8a6ec2696a06edf9549bb15ab8ba",
    strip_prefix = "rules_kotlin-%s" % RULES_KOTLIN_VERSION,
    url = "https://github.com/bazelbuild/rules_kotlin/archive/%s.tar.gz" % RULES_KOTLIN_VERSION,
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

kotlin_repositories()

kt_register_toolchains()

# End test dependencies

http_archive(
    name = "bazel_toolchains",
    sha256 = "04b10647f76983c9fb4cc8d6eb763ec90107882818a9c6bef70bdadb0fdf8df9",
    strip_prefix = "bazel-toolchains-1.2.4",
    urls = [
        "https://github.com/bazelbuild/bazel-toolchains/releases/download/1.2.4/bazel-toolchains-1.2.4.tar.gz",
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/archive/1.2.4.tar.gz",
    ],
)

load("@bazel_toolchains//rules:rbe_repo.bzl", "rbe_autoconfig")

# Creates a default toolchain config for RBE.
# Use this as is if you are using the rbe_ubuntu16_04 container,
# otherwise refer to RBE docs.
rbe_autoconfig(name = "buildkite_config")

load("//migration:maven_jar_migrator_deps.bzl", "maven_jar_migrator_repositories")

maven_jar_migrator_repositories()
