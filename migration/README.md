Add to WORKSPACE:

```python
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//migration:maven_jar_migrator_deps.bzl", "MAVEN_JAR_MIGRATOR_DEPS")

maven_install(
    name = "maven_jar_migrator",
    artifacts = MAVEN_JAR_MIGRATOR_DEPS,
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://jcenter.bintray.com",
    ],
)```

Run command in root of project workspace: 

```
$ bazel run @rules_jvm_external//scripts/migration:maven_jar
```