# Tooling integration

Generated Maven targets are ordinary Bazel targets, so IDEs and build-file tools
should consume their labels from the generated repository rather than reading a
local Maven cache.

## IDE source attachment

Enable source fetching before pinning:

```starlark
maven.install(
    artifacts = [
        # ...
    ],
    fetch_sources = True,
    lock_file = "//:maven_install.json",
)
```

Then repin so source JARs are recorded in the lock file:

```shell
REPIN=1 bazel run @maven//:pin
```

Generated `jvm_import` targets expose the source JAR to Bazel-aware IDE
integrations. If sources do not appear, confirm that the IDE imported the Bazel
target using the artifact and that the source URL is present in the lock file.

Use `fetch_javadoc = True` in the same way when local Javadoc artifacts are
needed. Both settings change resolution inputs and require repinning.

## Inspect generated targets

Use Bazel query to examine the generated repository:

```shell
bazel query @maven//:all --output=build
```

The generated targets carry `maven_coordinates` tags that other rules can use to
recover the original coordinates. The generated `@maven//:defs.bzl` also exports
`maven_artifacts` for tools that need the complete resolved set.

## Java Gazelle dependency index

The Java extension in
[`bazel-contrib/rules_jvm`](https://github.com/bazel-contrib/rules_jvm/blob/main/java/gazelle/README.md)
can use a package-to-artifact index generated during pinning by the Maven or
Gradle resolver.

Create empty lock and index files in a Bazel package:

```shell
touch maven_install.json maven_index.json BUILD.bazel
```

Declare both file labels and select Maven or Gradle:

```starlark
maven.install(
    artifacts = [
        # ...
    ],
    resolver = "maven",
    lock_file = "//:maven_install.json",
    index_file = "//:maven_index.json",
)
```

Pin to update both files:

```shell
REPIN=1 bazel run @maven//:pin
```

Configure the Java Gazelle extension in the root BUILD file to read those paths:

```starlark
# gazelle:java_maven_install_file maven_install.json
# gazelle:maven_index_file maven_index.json
# gazelle:java_maven_repository_name maven
```

The Coursier pin path does not currently write `index_file`. Select the Maven or
Gradle resolver when the Java Gazelle package index is required.

Commit the index alongside the lock file and update both in the same repin.
