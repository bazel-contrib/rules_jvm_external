licenses(["notice"])

exports_files(["maven_jar_migrator_deps.bzl"])

java_binary(
    name = "maven_jar",
    srcs = glob(["java/**/*.java"]),
    deps = [
        "@maven_jar_migrator//:com_google_guava_guava",
    ],
    main_class = "rules.jvm.external.MavenJarMigrator",
    resources = glob(["java/**/resources/*"]),
)
