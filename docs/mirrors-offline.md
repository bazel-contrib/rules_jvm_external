# Mirrors and offline operation

Pinned Maven artifacts are exposed to Bazel as `http_file` repositories with
the URLs and SHA-256 checksums recorded in the lock file. This boundary lets
Bazel's downloader cache, URL rewriter, proxy, and authentication support apply
to normal builds.

## Rewrite artifact URLs to a mirror

Use Bazel's downloader configuration to redirect URLs recorded in the lock file
without editing the file. For example, `downloader.cfg` can mirror Maven Central:

```text
rewrite repo1.maven.org/maven2/(.*) artifacts.example.com/maven-central/$1
```

Enable the configuration in `.bazelrc`:

```text
common --downloader_config=/path/to/downloader.cfg
```

Add blocking or allow-list rules only after confirming that the mirror contains
every required artifact. Rewrites are applied by Bazel while downloading pinned
files; the resolver still needs equivalent repository access when repinning.

For a fully internal setup, use internal repository URLs in `maven.install` so
resolution and the resulting lock file agree. Repin after changing repository
URLs.

## Warm the repository cache

Use a shared repository cache and fetch the complete generated repository while
network access is available:

```shell
bazel fetch --repository_cache=/path/to/repository-cache @maven//...
```

Use the same cache for the offline build:

```shell
bazel build \
  --repository_cache=/path/to/repository-cache \
  --nofetch \
  //...
```

The repository cache is keyed by checksums, so different Bazel workspaces can
reuse the same pinned artifact. `--nofetch` makes an uncached repository fetch
fail instead of accessing the network.

Fetching `@maven//...` warms the Maven artifacts only. A disconnected build must
also prepare Bazel modules, toolchains, and other external repositories. Follow
Bazel's
[offline build guidance](https://bazel.build/external/faq#how-do-i-prepare-and-run-an-offline-build)
for the complete workspace.

## Local Maven repository fallback

Include `m2local` in `repositories` to allow resolution from the user's local
Maven repository:

```starlark
maven.install(
    artifacts = [
        # ...
    ],
    repositories = [
        "m2local",
        "https://repo1.maven.org/maven2",
    ],
    lock_file = "//:maven_install.json",
)
```

When a pinned artifact has no remote URL and the lock file records `m2local`,
rules_jvm_external derives a file URL from the current user's Maven repository.
Local paths are not written when remote URLs are available, which keeps the lock
file portable between machines.

`m2local` is a machine-local fallback, not an artifact mirror or a vendored
dependency store. Populate it consistently before relying on it offline.

## Coursier bootstrap downloads

The Coursier resolver itself is bootstrapped from the URLs and checksum in
`private/versions.bzl`. If those URLs are inaccessible, set `COURSIER_URL` while
preserving the expected checksum:

```shell
REPIN=1 bazel run \
  --repo_env=COURSIER_URL=https://artifacts.example.com/tools/coursier.jar \
  @maven//:pin
```

To select a different Coursier binary, set both `COURSIER_URL` and
`COURSIER_SHA256`. rules_jvm_external is tested only with its pinned Coursier
version, so a different binary is unsupported and may be incompatible.

## Vendoring boundary

rules_jvm_external does not provide a dedicated feature that copies Maven
artifacts into a source-controlled vendor directory. Its supported artifact
inputs are repository URLs, `m2local`, and the Bazel repository cache described
above.

Bazel has a separate `bazel vendor` command for external repositories, but
rules_jvm_external does not maintain or validate a Maven-specific vendored
layout. Treat use of that command as a Bazel workflow rather than a
rules_jvm_external feature.
