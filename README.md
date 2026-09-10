# rules_jvm_external

Transitive Maven artifact resolution and publishing rules for Bazel.

* Main build: [![Build
  status](https://badge.buildkite.com/26d895f5525652e57915a607d0ecd3fc945c8280a0bdff83d9.svg?branch=master)](https://buildkite.com/bazel/rules-jvm-external)
* Examples build: [![Build
  status](https://badge.buildkite.com/d1e93c6c5321c9f7d24c71d849f00542f4ac5c9eed763eca2d.svg)](https://buildkite.com/bazel/rules-jvm-external-examples)

## Features

* `MODULE.bazel` configuration with Bzlmod
* Artifact version resolution with Coursier, Maven, or Gradle
* JAR, AAR, source JAR, and Javadoc JAR imports
* Maven repository publishing for built JARs
* Reproducible lock files with SHA-256 checksums
* Private and custom Maven repositories
* Bazel downloader and cache integration
* Versionless target labels
* Multiple isolated dependency repositories
* Windows, macOS, and Linux support

## Quickstart

Create an empty lock file and ensure its package has a BUILD file:

```shell
touch maven_install.json BUILD.bazel
```

Add `rules_jvm_external` and your dependencies to `MODULE.bazel`:

```starlark
bazel_dep(name = "rules_jvm_external", version = "7.1")

maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")

maven.install(
    artifacts = [
        "junit:junit:4.13.2",
        "org.hamcrest:hamcrest-library:2.2",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    lock_file = "//:maven_install.json",
)

use_repo(maven, "maven")
```

Resolve the dependencies and write the lock file:

```shell
REPIN=1 bazel run @maven//:pin
```

Check `MODULE.bazel`, `BUILD.bazel`, and `maven_install.json` into source
control. Repin after changing the artifacts, BOMs, repositories, or other
resolution inputs.

Reference artifacts in BUILD files with versionless labels:

```starlark
java_test(
    name = "example_test",
    srcs = ["ExampleTest.java"],
    deps = [
        "@maven//:junit_junit",
        "@maven//:org_hamcrest_hamcrest_library",
    ],
)
```

The default label for `foo.bar:baz-qux:1.2.3` is
`@maven//:foo_bar_baz_qux`. The version is omitted, non-alphanumeric characters
become underscores, and targets live in the top-level package of the generated
repository. Nonstandard packaging and classifiers other than `sources` or
`natives` may add suffixes.

## How dependency resolution works

The module extension collects artifact declarations, resolves their transitive
dependencies, and creates an external Bazel repository. JARs become
`jvm_import` targets, AARs become `aar_import` targets, and each artifact gets a
stable versionless alias.

The checked-in lock file separates resolution from ordinary builds. Repinning
records the selected versions, source URLs, and checksums. Subsequent builds
download those exact files through Bazel's downloader and reuse its shared
cache.

Coursier is the default resolver. All resolvers support Maven BOMs. The Maven
and Gradle resolvers require a lock file.
Resolver selection, conflict policies, forced versions, and environment
variables are covered in [Dependency resolution](docs/resolution.md).

Tags from Bazel dependencies can contribute artifacts to the same generated
repository. The root module controls which modules may contribute and which
versions win. Read [Module dependency layering](docs/module-dependency-layering.md)
when publishing a Bazel module or diagnosing an unexpected transitive version.

## Common operations

| Task | Command or setting |
|------|--------------------|
| Repin after changing resolution inputs | `REPIN=1 bazel run @maven//:pin` |
| List direct dependencies with newer versions | `bazel run @maven//:outdated` |
| Inspect generated targets | `bazel query @maven//:all --output=build` |
| Fetch all pinned dependencies | `bazel fetch @maven//...` |
| Show verbose resolver diagnostics | Set `RJE_VERBOSE=1` |
| Use a nondefault generated repository | Set the same `name` on tags and pass it to `use_repo` |

## Private repositories

Repository URLs are tried in declaration order. HTTP Basic Authentication is
supported, but credentials embedded in URLs can be written into the lock file.
Prefer a `~/.netrc` file or resolver-specific credential configuration:

```starlark
maven.install(
    artifacts = ["com.example:internal-library:1.0"],
    repositories = [
        "https://artifacts.example.com/maven",
        "https://repo1.maven.org/maven2",
    ],
    use_credentials_from_home_netrc_file = True,
    lock_file = "//:maven_install.json",
)
```

See [Private Maven repositories](docs/private-repositories.md) for Coursier
credentials, netrc behavior, and proxies.

## Documentation

The [documentation index](docs/README.md) covers the complete user workflow:

* [Declaring artifacts, BOMs, exclusions, and artifact options](docs/declaring-artifacts.md)
* [Choosing and configuring dependency resolution](docs/resolution.md)
* [Creating and updating lock files](docs/lock-files.md)
* [Understanding module dependency layering](docs/module-dependency-layering.md)
* [Authenticating to private repositories](docs/private-repositories.md)
* [Using mirrors, caches, and offline builds](docs/mirrors-offline.md)
* [Customizing generated targets](docs/customizing-targets.md)
* [Publishing artifacts](docs/publishing.md)
* [Migrating from WORKSPACE](docs/migrating-from-workspace.md)
* [Integrating IDE and Gazelle tooling](docs/tooling.md)
* [Troubleshooting common problems](docs/troubleshooting.md)

API references are generated from the source:

* [Module extension API](docs/extension-api.md)
* [Rules and macros API](docs/api.md)

## Bazel compatibility

This project aims to support the current Bazel LTS release and the two previous
major releases. It supports only the last release of a non-LTS major version.

* Bazel versions from `7.x` through `9.x` require rules_jvm_external `7.x`.
* Bazel versions from `5.x` through `7.x` require rules_jvm_external `6.x`.
* Bazel versions from `4.x` through `5.4` require rules_jvm_external `5.x`.
* Bazel versions before `4.0.0` require rules_jvm_external `4.2` or earlier.

## Examples

Runnable examples are in the [`examples/`](examples/) directory. You can also
find public projects using rules_jvm_external with this
[GitHub code search](https://github.com/search?q=rules_jvm_external+path%3A**%2FMODULE.bazel&type=code).

The [latest release](https://github.com/bazel-contrib/rules_jvm_external/releases/latest)
is available on GitHub and in the
[Bazel Central Registry](https://registry.bazel.build/modules/rules_jvm_external).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contributor setup, tests, and API
documentation generation.
