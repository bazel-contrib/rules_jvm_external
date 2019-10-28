exports_files(["maven_jar_migrator_deps.bzl"])

sh_binary(
    name = "maven_jar_sh",
    srcs = [":migrate_from_maven_jar.sh"],
)

java_binary(
    name = "maven_jar",
    srcs = [
        "MavenJarMigrator.java",
    ],
    deps = [
        "@maven_jar_migrator//:com_google_guava_guava",
    ],
    main_class = "rules.jvm.external.MavenJarMigrator",
)
