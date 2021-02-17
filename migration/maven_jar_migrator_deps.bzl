load("@rules_jvm_external//:defs.bzl", "maven_install")

def maven_jar_migrator_repositories():
    maven_install(
        name = "maven_jar_migrator",
        artifacts = [
            "com.google.guava:guava:28.0-jre",
        ],
        repositories = [
            "https://repo1.maven.org/maven2",
        ],
    )
