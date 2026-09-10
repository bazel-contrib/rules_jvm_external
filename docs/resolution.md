# Dependency resolution

rules_jvm_external can resolve Maven dependencies with Coursier, Maven, or
Gradle. All three resolvers support Maven BOMs. Select a resolver on the root
module's `maven.install` tag:

```starlark
maven.install(
    artifacts = [
        # ...
    ],
    resolver = "maven",
    lock_file = "//:maven_install.json",
)
```

The root module's setting takes precedence when several Bazel modules
contribute to one generated Maven repository. See
[Module dependency layering](module-dependency-layering.md) for that merge
model.

## Common settings

All resolvers understand `RJE_VERBOSE`. The Maven and Gradle resolvers also
understand `RJE_MAX_THREADS`:

| Environment variable | Meaning |
|----------------------|---------|
| `RJE_VERBOSE` | Set to `1` for additional diagnostic output on standard error. |
| `RJE_MAX_THREADS` | Maven and Gradle only. Maximum download thread count. The default is the lower of the processor count and 5. |

`resolve_timeout` controls the resolution and fetch timeout in seconds. The
default is 600. Increase it for large dependency graphs or slow repositories:

```starlark
maven.install(
    # ...
    resolve_timeout = 900,
)
```

Set `JDK_JAVA_OPTIONS` through `--repo_env` to pass JVM options to Java-based
resolvers. For example, a `.bazelrc` can select a custom trust store:

```text
build --repo_env=JDK_JAVA_OPTIONS=-Djavax.net.ssl.trustStore=/path/to/cacerts
```

## Coursier

[Coursier](https://get-coursier.io) is the default resolver. It is the only
resolver that supports build-time resolution without a lock file, although a
lock file is strongly recommended. Coursier is also used by tools such as sbt.

Set `COURSIER_CREDENTIALS` to inline credentials or an absolute path as
described in the
[Coursier credentials documentation](https://get-coursier.io/docs/other-credentials#inline).
Set `COURSIER_OPTS` to a space-separated list of JVM options:

```shell
COURSIER_OPTS="-Xms1g -Xmx4g"
```

Use `additional_coursier_options` to pass command-line options to Coursier. Use
`resolver_extra_dependencies` to add custom `MetadataService` or
`DownloadService` SPI implementations to the resolver classpath.

## Maven

The Maven resolver requires a lock file. The file may be empty when
bootstrapping the repository.

| Environment variable | Meaning |
|----------------------|---------|
| `RJE_ASSUME_PRESENT` | Skip remote existence checks and assume dependencies are present. |
| `RJE_UNSAFE_CACHE` | Maven uses the shared `$HOME/.m2/repository` by default. Set to `0` or `false` for an isolated cache. |

The shared local Maven repository can be used during resolution, but local paths
are not written to the lock file unless `m2local` appears in `repositories`.
The resolver also reads credentials from `$HOME/.netrc`.

## Gradle

The Gradle resolver requires a lock file, which may initially be empty.

| Environment variable | Meaning |
|----------------------|---------|
| `RJE_ASSUME_PRESENT` | Skip remote existence checks and assume dependencies are present. |
| `RJE_UNSAFE_CACHE` | Gradle uses the shared `$HOME/.gradle` caches by default. Set to `0` or `false` for an isolated cache. |

## Version conflicts

`version_conflict_policy` controls conflicts between direct and transitive
versions:

* `default` follows the selected resolver's normal conflict policy. For
  Coursier, see its [version-handling documentation](https://get-coursier.io/docs/other-version-handling).
* `pinned` selects direct versions unconditionally. With the Maven and Gradle
  resolvers, this applies only to artifacts contributed by the root module.

For a worked example, `google-cloud-storage:1.66.0` transitively selects
Guava 26.0-android under the default policy:

```starlark
maven.install(
    name = "pinning",
    artifacts = [
        "com.google.cloud:google-cloud-storage:1.66.0",
    ],
    lock_file = "//:pinning_install.json",
)

use_repo(maven, "pinning")
```

After pinning, the versioned alias is:

```text
@pinning//:com_google_guava_guava_26_0_android
```

Adding the higher direct version 27.0-android selects that version:

```starlark
maven.install(
    name = "pinning",
    artifacts = [
        "com.google.cloud:google-cloud-storage:1.66.0",
        "com.google.guava:guava:27.0-android",
    ],
    lock_file = "//:pinning_install.json",
)
```

```text
@pinning//:com_google_guava_guava_27_0_android
```

Adding the lower direct version 25.0-android still selects 26.0-android under
the default policy:

```starlark
maven.install(
    name = "pinning",
    artifacts = [
        "com.google.cloud:google-cloud-storage:1.66.0",
        "com.google.guava:guava:25.0-android",
    ],
    lock_file = "//:pinning_install.json",
)
```

```text
@pinning//:com_google_guava_guava_26_0_android
```

Setting the policy to `pinned` selects the lower direct version instead:

```starlark
maven.install(
    name = "pinning",
    artifacts = [
        "com.google.cloud:google-cloud-storage:1.66.0",
        "com.google.guava:guava:25.0-android",
    ],
    version_conflict_policy = "pinned",
    lock_file = "//:pinning_install.json",
)
```

```text
@pinning//:com_google_guava_guava_25_0_android
```

Use `force_version` when only one artifact must remain at the declared version:

```starlark
maven.install(
    artifacts = ["xyz.rogfam:littleproxy:2.1.0"],
    lock_file = "//:maven_install.json",
)

maven.artifact(
    group = "com.google.guava",
    artifact = "guava",
    version = "23.3-jre",
    force_version = True,
)
```

After pinning, Guava 23.3-jre is selected even when a transitive dependency
requests a newer version. An incompatible forced version can make resolution
fail. Prefer updating the root declaration to the highest compatible version
when possible.

## Duplicate declarations

Duplicate artifacts produce a warning by default. Set
`duplicate_version_warning` to `"error"` to fail or `"none"` to suppress the
warning:

```starlark
maven.install(
    # ...
    duplicate_version_warning = "error",
)
```

## Isolated dependency trees

Use distinct tag names for components that need incompatible dependency trees.
Each name produces an independent external repository and requires its own lock
file:

```starlark
maven.install(
    name = "server_app",
    artifacts = ["com.google.guava:guava:33.4.8-jre"],
    lock_file = "//:server_maven_install.json",
)

maven.install(
    name = "android_app",
    artifacts = ["com.google.guava:guava:33.4.8-android"],
    lock_file = "//:android_maven_install.json",
)

use_repo(maven, "server_app", "android_app")
```

The targets are then addressed as `@server_app//:com_google_guava_guava` and
`@android_app//:com_google_guava_guava`.

## Checksums and empty artifacts

Resolution normally fails when an artifact has no SHA-1 or MD5 checksum in its
repository. Set `fail_on_missing_checksum = False` only when the repository
cannot supply checksums.

Set `ignore_empty_files = True` to treat empty JARs as missing. This can be
useful when repositories publish empty source JARs.
