load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "bom",
    srcs = ["BomResolver.java"],
    visibility = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external:__subpackages__",
        "//tests/com/github/bazelbuild/rules_jvm_external:__subpackages__",
    ],
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/netrc",
        artifact(
            "org.apache.maven:maven-resolver-provider",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-api",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-impl",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-connector-basic",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-spi",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-transport-file",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-transport-http",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven.resolver:maven-resolver-util",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

java_binary(
    name = "BomResolverMain",
    srcs = ["BomResolverMain.java"],
    main_class = "com.github.bazelbuild.rules_jvm_external.resolver.bom.BomResolverMain",
    visibility = ["//visibility:public"],
    deps = [
        ":bom",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        artifact(
            "com.google.code.gson:gson",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)
