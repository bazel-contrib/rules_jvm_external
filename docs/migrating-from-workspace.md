# Migrating from WORKSPACE

Move dependency resolution from the legacy `maven_install` repository macro to
the `maven` module extension before removing the rules_jvm_external setup from
`WORKSPACE`.

## Add the module dependency

Add the current rules_jvm_external release and create the extension in
`MODULE.bazel`:

```starlark
bazel_dep(name = "rules_jvm_external", version = "7.1")

maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
```

## Translate the installation

A typical WORKSPACE declaration:

```starlark
maven_install(
    name = "maven",
    artifacts = ["junit:junit:4.13.2"],
    repositories = ["https://repo1.maven.org/maven2"],
    maven_install_json = "//:maven_install.json",
)
```

becomes:

```starlark
maven.install(
    name = "maven",
    artifacts = ["junit:junit:4.13.2"],
    repositories = ["https://repo1.maven.org/maven2"],
    lock_file = "//:maven_install.json",
)

use_repo(maven, "maven")
```

The existing lock file can be used at the same path. Repin after completing the
migration so its signature reflects the module-extension inputs.

Common attribute translations are:

| WORKSPACE setting | Bzlmod setting |
|-------------------|-----------------|
| `artifacts`, `boms`, `repositories` | Same attributes on `maven.install` |
| `maven_install_json` | `lock_file` on `maven.install` |
| `excluded_artifacts` | Same attribute on `maven.install` |
| `override_targets` | One `maven.override` tag per coordinate |
| `resolver`, conflict, visibility, fetch, timeout, and credential settings | Same concepts on `maven.install`; verify names in the [extension API](extension-api.md) |
| Additional installations | A unique tag `name`, `lock_file`, and `use_repo` entry for each repository |

The generated repository keeps its name, so ordinary labels such as
`@maven//:junit_junit` do not need to change.

## Translate detailed artifact declarations

Replace artifact specification structs or maps with `maven.artifact` tags:

```starlark
maven.artifact(
    group = "com.google.guava",
    artifact = "guava",
    version = "33.4.8-jre",
    exclusions = ["com.google.j2objc:j2objc-annotations"],
)
```

Use `maven.amend_artifact` when the coordinate is already contributed by an
`install` or `from_toml` tag in the same module:

```starlark
maven.amend_artifact(
    coordinates = "junit:junit",
    testonly = "true",
)
```

See [Declaring Maven artifacts](declaring-artifacts.md) for BOMs, version
catalogs, exclusions, `neverlink`, `testonly`, AARs, sources, and Javadocs.

## Translate overrides

Replace each `override_targets` entry with an override tag. Use `@//` to keep
the replacement label in the main repository:

```starlark
maven.override(
    coordinates = "com.google.guava:guava",
    target = "@//third_party/guava:guava",
)
```

## Create or verify the lock-file target

The lock file must exist and belong to a Bazel package before the extension is
evaluated:

```shell
touch maven_install.json BUILD.bazel
REPIN=1 bazel run @maven//:pin
```

Do not run the pin target first and then rename a generated file. The target
writes the path declared by `lock_file` directly.

## Remove the WORKSPACE setup

After the Bzlmod build and pin target succeed:

1. Remove the rules_jvm_external archive or repository declaration from
   `WORKSPACE`.
2. Remove the load and call for the legacy repository macro.
3. Remove any repository setup that was needed only by that macro.
4. Search `.bazelrc`, scripts, and CI for legacy pin commands and update them to
   `REPIN=1 bazel run @<name>//:pin`.
5. Run a clean build and confirm that every expected generated repository is
   listed in `use_repo`.

If Bazel dependencies contribute Maven artifacts to the same name, read
[Module dependency layering](module-dependency-layering.md) before accepting the
new resolution.
