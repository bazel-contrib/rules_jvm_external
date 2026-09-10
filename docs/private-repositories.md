# Private Maven repositories

List repository URLs in lookup order on `maven.install`:

```starlark
maven.install(
    artifacts = ["com.example:internal-library:1.0"],
    repositories = [
        "https://artifacts.example.com/maven",
        "https://repo1.maven.org/maven2",
    ],
    lock_file = "//:maven_install.json",
)
```

Credentials are needed both while resolving dependencies and while Bazel's
downloader fetches pinned URLs. Prefer netrc or resolver-specific credential
configuration so secrets do not appear in `MODULE.bazel` or the checked-in lock
file.

## netrc credentials

Pinned downloads made through `http_file` automatically use the user's
`~/.netrc`. Add an entry for the repository host using the standard netrc
machine, login, and password fields.

To make Coursier use credentials from the same file during resolution, set:

```starlark
maven.install(
    # ...
    use_credentials_from_home_netrc_file = True,
)
```

The Maven resolver reads `$HOME/.netrc` during resolution. Resolver-specific
behavior may differ, so test both repinning and a clean pinned build.

With a lock file, `additional_netrc_lines` can add entries derived outside the
workspace. Do not commit literal secrets. Supply sensitive values through
repository policy or generated configuration.

## Coursier credentials

Coursier supports `COURSIER_CREDENTIALS` as inline configuration or an absolute
path to a credentials file. See the
[Coursier credentials documentation](https://get-coursier.io/docs/other-credentials.html#property-file)
for the accepted formats.

HTTP Basic Authentication can also be embedded in a repository URL:

```text
https://username:password@artifacts.example.com/maven
```

URLs are recorded in the lock file, so this form should not be used for secrets
when the lock file is checked into source control.

## Proxies

As with other Bazel repository rules, the standard `http_proxy`, `https_proxy`,
and `no_proxy` environment variables, and their uppercase counterparts, are
supported. Dependency resolution uses the proxy settings understood by the
selected resolver and the Java runtime. Bazel's downloader follows Bazel's
proxy and URL rewriting configuration when fetching pinned artifacts.

If a proxy terminates TLS with a private certificate authority, pass a trust
store to Java-based resolver processes through `.bazelrc`:

```text
build --repo_env=JDK_JAVA_OPTIONS=-Djavax.net.ssl.trustStore=/path/to/cacerts
```

Configure Bazel's downloader separately when it also needs the private
certificate authority. Verify the setup by repinning and then building from an
empty Bazel repository cache.

For repository mirrors and disconnected builds, see
[Mirrors and offline operation](mirrors-offline.md).
