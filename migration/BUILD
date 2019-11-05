licenses(["notice"])

exports_files(["maven_jar_migrator_deps.bzl"])

java_binary(
    name = "maven_jar",
    srcs = ["java/rules/jvm/external/MavenJarMigrator.java"],
    main_class = "rules.jvm.external.MavenJarMigrator",
    resources = ["java/rules/jvm/external/resources/workspace_template.txt"],
    deps = [
        "@maven_jar_migrator//:com_google_guava_guava",
    ],
)
