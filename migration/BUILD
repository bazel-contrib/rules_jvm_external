licenses(["notice"])

exports_files(["maven_jar_migrator_deps.bzl"])

java_binary(
    name = "maven_jar",
    srcs = ["java/com/github/bazelbuild/rules_jvm_external/MavenJarMigrator.java"],
    main_class = "com.github.bazelbuild.rules_jvm_external.MavenJarMigrator",
    resources = ["java/com/github/bazelbuild/rules_jvm_external/resources/workspace_template.txt"],
    deps = [
        "@maven_jar_migrator//:com_google_guava_guava",
    ],
)
