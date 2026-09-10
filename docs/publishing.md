# Publishing Maven artifacts

`java_export` wraps `java_library` and generates the POM, JAR, source, Javadoc,
and publisher targets needed to publish a Maven artifact:

```starlark
load("@rules_jvm_external//:defs.bzl", "java_export")

java_export(
    name = "exported_lib",
    maven_coordinates = "com.example:project:0.0.1",
    pom_template = "pom.tmpl",  # Optional.
    srcs = glob(["*.java"]),
    deps = [
        "//user_project/utils",
        "@maven//:com_google_guava_guava",
    ],
)
```

The implicit `exported_lib.publish` target uploads the artifact. To publish to a
local Maven repository:

```shell
MAVEN_REPO="file://$HOME/.m2/repository" \
  bazel run //user_project:exported_lib.publish
```

`maven_export` creates the same publishing targets for an existing library or
archive. See the [rules and macros API](api.md) for its complete parameters.

## Kotlin artifacts

`kt_jvm_export` provides the same publishing workflow around
`kt_jvm_library`:

```starlark
load("@rules_jvm_external//:kt_defs.bzl", "kt_jvm_export")

kt_jvm_export(
    name = "exported_kt_lib",
    maven_coordinates = "com.example:project:0.0.1",
    srcs = glob(["*.kt"]),
)
```

## Remote repositories

Set `MAVEN_REPO` to an HTTPS repository and provide credentials as environment
variables:

```shell
MAVEN_REPO="https://oss.sonatype.org/service/local/staging/deploy/maven2" \
MAVEN_USER="example_user" \
MAVEN_PASSWORD="secret" \
  bazel run //user_project:exported_lib.publish
```

The publisher also accepts repository URLs for supported cloud storage
services:

```shell
MAVEN_REPO="gs://example-bucket/repository" \
  bazel run //user_project:exported_lib.publish

MAVEN_REPO="s3://example-bucket/repository" \
  bazel run //user_project:exported_lib.publish

MAVEN_REPO="artifactregistry://us-west1-maven.pkg.dev/project/repository" \
  bazel run //user_project:exported_lib.publish
```

The equivalent lowercase values may be supplied through `--define`, but
environment variables avoid exposing passwords in command lines.

## Signing

Set `GPG_SIGN=true` to sign with the current default GPG key. The `gpg` binary
must be installed:

```shell
GPG_SIGN=true MAVEN_REPO="https://repo.example.com/releases" \
  bazel run --stamp //user_project:exported_lib.publish
```

The publisher also supports in-memory PGP keys through
`USE_IN_MEMORY_PGP_KEYS`, `PGP_SIGNING_KEY`, and `PGP_SIGNING_PWD`.

## Generated POMs

Publishing targets derive first-order Maven dependencies from
`maven_coordinates` tags and generate a POM. Supply `pom_template` to customize
the base XML. The following placeholders are replaced:

* `{groupId}`
* `{artifactId}`
* `{version}`
* `{type}`
* `{scope}`
* `{dependencies}`

Use `pom_file` directly when only POM generation is needed. The
[`examples/pom_file_generation`](../examples/pom_file_generation/) example
shows a complete setup.
