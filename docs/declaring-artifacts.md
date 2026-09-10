# Declaring Maven artifacts

Declare direct dependencies with the `artifacts` attribute of `maven.install`:

```starlark
maven.install(
    artifacts = [
        "com.google.guava:guava:33.4.8-jre",
        "junit:junit:4.13.2",
    ],
    lock_file = "//:maven_install.json",
)
```

Coordinates normally use `groupId:artifactId:version` format. The full syntax
is Gradle's
[external dependency notation](https://docs.gradle.org/current/javadoc/org/gradle/api/artifacts/dsl/DependencyHandler.html),
`group:artifact[:version][:classifier][@packaging]`. The legacy
`group:artifact[:packaging[:classifier]]:version` form is also accepted. The
generated target label omits the version. For example,
`com.google.guava:guava:33.4.8-jre` is available as
`@maven//:com_google_guava_guava`.

See the [module extension API](extension-api.md) for every tag and attribute.

## Artifact-specific options

Use `maven.artifact` when an artifact needs options that cannot be expressed by
a coordinate string. Tags with the same `name` contribute to the same generated
repository:

```starlark
maven.artifact(
    group = "com.google.guava",
    artifact = "guava",
    version = "33.4.8-jre",
    exclusions = [
        "com.google.j2objc:j2objc-annotations",
        "org.codehaus.mojo:animal-sniffer-annotations",
    ],
)
```

Coordinate strings are parsed into the same group, artifact, version,
packaging, and classifier fields represented by this tag. The tag form adds
options such as exclusions and Bazel target flags that a string cannot carry.

An artifact tag also supports `packaging`, `classifier`, `force_version`,
`neverlink`, and `testonly`.

## Amending an existing declaration

Dependencies may be supplied by `maven.install` or a version catalog in the
same module. Use `maven.amend_artifact` to add artifact-specific options without
redeclaring the dependency:

```starlark
maven.amend_artifact(
    coordinates = "io.grpc:grpc-core",
    exclusions = ["io.grpc:grpc-util"],
    testonly = "true",
)
```

Only `groupId:artifactId` are used when matching an existing artifact. The
boolean-like attributes accept `"true"`, `"false"`, `"on"`, or `"off"`; leave
them empty to preserve the existing value.

## Gradle version catalogs

Dependencies in a Gradle
[`libs.versions.toml`](https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog)
file can be imported with the `from_toml` tag:

```starlark
maven.from_toml(
    libs_versions_toml = "//gradle:libs.versions.toml",
)
```

A version catalog may look like this:

```toml
[versions]
junitJupiter = "5.12.2"

[libraries]
guava = { module = "com.google.guava:guava", version = "33.4.8-jre" }
junitApi = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junitJupiter" }
```

rules_jvm_external supports additional fields beyond the standard Gradle
version catalog format. Specify their values as quoted strings in the inline
table:

| Field | Example | Description |
|-------|---------|-------------|
| `classifier` | `classifier = "all"` | Maven classifier for the artifact |
| `exclusions` | `exclusions = "['com.example:unwanted']"` | JSON-encoded list of `groupId:artifactId` exclusions |
| `force_version` | `force_version = "true"` | Forces this version during resolution |
| `is_bom` | `is_bom = "true"` | Treats this entry as a BOM |
| `package` | `package = "aar"` | Packaging type; the default is `jar` |

For example:

```toml
[libraries]
guavaBom = { module = "com.google.guava:guava-bom", version = "33.4.8-jre", is_bom = "true" }
clickhouse = { module = "com.clickhouse:clickhouse-jdbc", version = "0.9.2", classifier = "all", force_version = "true" }
misk = { module = "com.squareup.misk:misk-core", version = "1.0.0", exclusions = "['*:*']" }
```

## Maven BOMs

Use `boms` to import dependency management from a Maven BOM. Versions managed by
the BOM may be omitted from artifact coordinates:

```starlark
maven.install(
    boms = [
        "org.seleniumhq.selenium:selenium-bom:4.18.1",
    ],
    artifacts = [
        # The BOM manages this artifact's version, so it may be omitted here.
        "org.seleniumhq.selenium:selenium-java",
    ],
    lock_file = "//:maven_install.json",
)
```

For version catalogs, set `is_bom = "true"` on the library or list the module in
`bom_modules`:

```starlark
maven.from_toml(
    libs_versions_toml = "//gradle:libs.versions.toml",
    bom_modules = ["com.google.guava:guava-bom"],
)
```

All resolvers support BOMs. See [Dependency resolution](resolution.md) for
resolver selection and lock-file requirements.

## Exclusions

The `artifacts` attribute accepts coordinate strings only. An inline artifact
map with an `exclusions` field fails under Bzlmod with an error such as:

```text
error converting value for attribute artifacts: expected value of type 'string' ... got None (NoneType)
```

Use a separate `maven.artifact` tag instead. Remove the corresponding
coordinate string from `maven.install`; if a coordinate must be repeated in
more than one declaration, keep its version in sync. Use
`maven.amend_artifact` when the coordinate is already declared by another tag in
the same module.

Artifact-specific exclusions remove dependencies from one transitive closure:

```starlark
maven.artifact(
    group = "io.grpc",
    artifact = "grpc-core",
    version = "1.71.0",
    exclusions = ["io.grpc:grpc-util"],
)
```

Global exclusions apply to the complete resolution:

```starlark
maven.install(
    artifacts = [
        # ...
    ],
    excluded_artifacts = [
        "com.google.guava:guava",
    ],
    lock_file = "//:maven_install.json",
)
```

Exclusion coordinates use `groupId:artifactId` format.

## Compile-only and test-only artifacts

Set `neverlink` for an artifact that should be available at compile time but not
at runtime:

```starlark
maven.amend_artifact(
    coordinates = "com.squareup:javapoet",
    neverlink = "true",
)
```

Set `testonly` for an artifact that should be available only to test-only Bazel
targets:

```starlark
maven.amend_artifact(
    coordinates = "junit:junit",
    testonly = "true",
)
```

## AARs and Android rules

Artifacts with `aar` packaging generate `aar_import` targets instead of
`jvm_import` targets. The packaging may come from the POM or be set explicitly:

```starlark
maven.artifact(
    group = "androidx.appcompat",
    artifact = "appcompat",
    version = "1.7.1",
    packaging = "aar",
)
```

Use `use_starlark_android_rules` to select the Starlark Android rules. If the
Android rules are loaded from a nonstandard location, set
`aar_import_bzl_label` to the label that exports `aar_import`.

## Sources and Javadocs

Set `fetch_sources` or `fetch_javadoc` on `maven.install` to fetch auxiliary
artifacts alongside the main JARs:

```starlark
maven.install(
    artifacts = ["com.google.guava:guava:33.4.8-jre"],
    fetch_sources = True,
    fetch_javadoc = True,
    lock_file = "//:maven_install.json",
)
```

Repin after changing either setting so the lock file records the additional
artifacts.
