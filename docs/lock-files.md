# Lock files and pinning

Pin dependencies to make builds repeatable, avoid dependency resolution during
normal builds, and integrate downloaded artifacts with Bazel's checksum-based
cache. The checked-in lock file records resolved versions, artifact URLs, and
SHA-256 checksums.

## Create the first lock file

Declare the lock file on `maven.install` before running the pin target:

```starlark
maven.install(
    artifacts = [
        "junit:junit:4.13.2",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    lock_file = "//:maven_install.json",
)
```

The file must exist and belong to a Bazel package, but it may be empty before
the first pin. See
[#242](https://github.com/bazel-contrib/rules_jvm_external/issues/242) for the
failure caused by a missing root BUILD file:

```shell
touch maven_install.json BUILD.bazel
```

Resolve the dependencies and write the declared path:

```shell
REPIN=1 bazel run @maven//:pin
```

Commit `MODULE.bazel`, the BUILD file, and `maven_install.json` together.

This order applies to every resolver. Coursier is the only resolver that permits
an omitted lock file, but build-time resolution is slower, does not verify
artifacts against a checked-in checksum, and cannot share those downloads across
Bazel workspaces as effectively.

## Update the lock file

Repin whenever an input to dependency resolution changes, including artifacts,
BOMs, repositories, exclusions, source or Javadoc fetching, and resolver
configuration:

```shell
REPIN=1 bazel run @maven//:pin
```

`RULES_JVM_EXTERNAL_REPIN=1` limits the environment signal to
rules_jvm_external when other repository rules also observe `REPIN`:

```shell
RULES_JVM_EXTERNAL_REPIN=1 bazel run @maven//:pin
```

After a lock file is configured, it is the source of truth for normal builds.
Edits to `MODULE.bazel` do not change generated targets until the file is
repinned.

## Require repinning

`fail_if_repin_required` is true by default. If the declared dependency inputs
and the lock-file signature disagree, the build fails and tells the user to
repin.

Projects with a wrapper or policy-specific update command can replace the
default message:

```starlark
maven.install(
    # ...
    repin_instructions = "Run ./tools/update_dependencies",
)
```

Only the root module's `fail_if_repin_required` and `repin_instructions`
settings are used.

## Custom lock-file locations

`lock_file` accepts any file label:

```starlark
maven.install(
    name = "server_dependencies",
    artifacts = ["com.google.guava:guava:33.4.8-jre"],
    lock_file = "//third_party/jvm:server_install.json",
)
```

Create the file and its package before pinning:

```shell
touch third_party/jvm/server_install.json third_party/jvm/BUILD.bazel
```

The pin target writes the declared file directly.

## Multiple lock files

Each named installation has its own generated repository and lock file:

```starlark
maven.install(
    name = "foo",
    artifacts = [
        # ...
    ],
    lock_file = "//:foo_maven_install.json",
)

maven.install(
    name = "bar",
    artifacts = [
        # ...
    ],
    lock_file = "//:bar_maven_install.json",
)

use_repo(maven, "foo", "bar")
```

Pin them independently:

```shell
REPIN=1 bazel run @foo//:pin
REPIN=1 bazel run @bar//:pin
```

Within one generated repository name, all contributing tags must agree on a
single lock file. See [Module dependency layering](module-dependency-layering.md)
for contributions from non-root Bazel modules.

## Find outdated dependencies

Run the generated `outdated` target to list direct dependencies with newer
available versions:

```shell
bazel run @maven//:outdated
```

Update the declarations deliberately, then repin and review the resulting lock
file diff.

## List direct dependencies

Run the generated `direct_deps` target to list every direct dependency with its
resolved version:

```shell
bazel run @maven//:direct_deps
```

Each line is a coordinate in [Gradle notation](declaring-artifacts.md), with
the version selected during resolution. This includes versions that were not
written on the declaration itself, such as a version supplied by a BOM.

## Caching and offline builds

Pinned artifacts are downloaded through Bazel's downloader and cached by their
SHA-256 checksums. See [Mirrors and offline operation](mirrors-offline.md) for
cache warming, `m2local`, URL rewriting, and the supported boundary of offline
operation.
