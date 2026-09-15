# Module dependency layering

The extension collects declarations from all tags with the same `name` before resolving them. Each
name is an independent Maven repository namespace. Declarations in one namespace never affect
another namespace.

The root module and its dependencies have different roles during layering. The root contributes
the declarations that belong to the current Bazel project. Every other module is a non-root
contributor. Coordinates are matched by `group:artifact`, with non-default packaging and classifiers
included in the key. For example, a classified JAR layers independently of its unclassified JAR.

## Version precedence

For conflicts between modules, layering selects one complete declaration for each coordinate. The
selected declaration supplies its exclusions, `neverlink`, `testonly`, `force_version`, packaging,
classifier, and other fields. Fields from discarded declarations are not merged into it. Only the
selected declaration from a cross-module conflict reaches the resolver. Under the default
`version_conflict_policy` this means which declaration wins does not depend on the resolver in use.
Duplicate declarations made by the root module normally remain for the repository-level check. If
any module sets `force_version` on the same coordinate at two different versions, layering fails
first.

The surviving declaration is chosen by these rules, in order:

1. A forced version in the root module always wins.
2. Otherwise, a forced declaration beats any unforced one, whatever the versions.
3. Otherwise, the highest version wins, regardless of which module declared it.

On ties and conflicts:

- If two non-root modules force different versions of a coordinate the root does not force, layering
  fails before resolution. The root can settle it by forcing the version itself.
- On a tie (equal versions with the same force status, including the same forced version from more
  than one module) the first module's declaration is kept; the root counts as first.
- A non-root artifact marked `testonly` is dropped.

Within one non-root module, a forced declaration beats its unforced duplicates. Forced declarations
for the same coordinate at different versions fail before selection. Matching forced versions retain
the first complete declaration. Among unforced declarations, the highest version wins and an
equal-version tie retains the first complete declaration. This check uses the same coordinate key as
layering, so non-default packaging and classifiers remain independent.

"Highest" uses the Maven `ComparableVersion` ordering implemented by
[`private/rules/maven_version.bzl`](../private/rules/maven_version.bzl), not lexical string ordering.

`version_conflict_policy = "pinned"` changes this interaction. For the Gradle and Maven resolvers,
root artifacts are force-marked before layering. The duplicate-force check applies to declared
forces before this policy is applied. Maven then marks every versioned root declaration.
Gradle first selects one version for each root `group:artifact`: an unclassified declaration takes
precedence over classified declarations, and Maven `ComparableVersion` order selects among
declarations with the same classification status. Every root declaration for that module at the
selected version is then marked forced, including classified declarations. The root consequently
wins because it now forces the coordinate. For Coursier, layering is unchanged and every
surviving direct version is later passed as a `--force-version` argument. A higher non-root
version can therefore displace the root under Coursier and then be pinned.

The `force_version` flag can be set by an `artifact` tag, an `amend_artifact` tag, or a regular
artifact read by `from_toml`. Coordinates in `install.artifacts` cannot carry the flag. BOMs use the
same extension-layer precedence rules as artifacts, but a BOM can only gain `force_version` through
`amend_artifact`. A TOML BOM is returned before the TOML force field is copied. Resolver requests do
not propagate a BOM force flag, so it affects extension-layer priority only.

## Contributors and configuration

When the root and other modules contribute artifacts to the same namespace, the extension prints a
message such as:

`The maven repository 'multiple_lock_files' has contributions from multiple bzlmod modules, and will be resolved together: ["bzlmod_lock_files", "rules_jvm_external"]`

If those contributions are expected, set `known_contributing_modules` on the root `install` tag.
The warning includes the value to add. Once this attribute is non-empty, only listed modules may
contribute artifacts or BOMs to that namespace. A module that contributes only BOMs triggers the
same contribution warning and can be acknowledged through the same attribute.

After dependencies are layered, scalar `install` attributes from the root module take precedence.
List attributes are combined root-first, while preserving their existing deduplication or
concatenation behaviour.

The default namespace is `maven`. A module intended for use through `bazel_dep` should normally use
its own name, such as the `rules_jvm_external_deps` namespace used by this project. The default is
appropriate when a module deliberately contributes functionality that would otherwise be supplied
as a Maven dependency, or when the project is only used as the root module.

## Diagnostics

Layering keeps the following diagnostics so that unexpected versions can be traced to their
contributing module:

| Condition | Behaviour |
|---|---|
| An unacknowledged non-root module contributes artifacts or BOMs | Always prints the contribution warning. |
| One module declares `force_version` for the same coordinate at different versions | Fails with the module, coordinate, and both versions. |
| Non-root modules force different versions of a coordinate that the root does not force | Fails with the contributing module names and versions, and tells the user how to select a version in the root module. |
| `known_contributing_modules` excludes an artifact or BOM contributor | Prints an `INFO` message when `RJE_VERBOSE` is set. |
| Layering selects a version different from the root version | `duplicate_version_warning` controls whether to warn, fail, or do nothing. Its default is `warn`, which prints during extension evaluation without requiring `REPIN` or `RULES_JVM_EXTERNAL_REPIN`. |
| A non-root-only coordinate is added | Prints an `INFO` message when a repin variable and `RJE_VERBOSE` are both set. |
| Multiple versions reach the repository rule | `duplicate_version_warning` controls whether to warn, fail, or do nothing. Its default is `warn`. |

No version-selection diagnostic is emitted when the root version is selected. This includes a
forced root, an unforced root with the highest version, an equal unforced tie, and an equal-version
forced non-root whose complete declaration survives. Coordinates that the root did not declare
also have no version-selection diagnostic; the contribution warning covers their origin.

The repository-level duplicate check keys declarations by `group:artifact` and optional classifier,
but not packaging. Packaging-distinct declarations at different versions can therefore warn or fail
even though the extension layers them independently. It also checks multiple root declarations.
Layering resolves cross-module conflicts first, so each contributes only its selected
declaration here.

For example, the version-selection warning is:

```
WARNING: For dependency 'com.google.protobuf:protobuf-java' the root @maven repo wants version 3.25.5, but got 4.27.2 from the bazel_worker_java bazel dep. Please update the version in your MODULE.bazel or set `force_version = True`.
```
