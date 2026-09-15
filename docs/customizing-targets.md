# Generated and customized targets

rules_jvm_external generates one versionless target for each resolved artifact.
For example:

* `junit:junit:4.13.2` becomes `@maven//:junit_junit`.
* `org.hamcrest:hamcrest-library:2.2` becomes
  `@maven//:org_hamcrest_hamcrest_library`.

Use `bazel query @maven//:all --output=build` to inspect the generated
`jvm_import`, `aar_import`, alias, and helper targets.

For a pinned `junit:junit:4.13.2` installation, the output includes targets
shaped like:

```starlark
alias(
    name = "junit_junit_4_13_2",
    actual = "@maven//:junit_junit",
)

jvm_import(
    name = "junit_junit",
    jar = "@maven//:junit/junit/4.13.2/junit-4.13.2.jar",
    deps = ["@maven//:org_hamcrest_hamcrest_core"],
    maven_coordinates = "junit:junit:4.13.2",
)
```

Generated targets carry their original coordinates as `maven_coordinates`
metadata. Publishing and POM tools use it to recover dependency coordinates. See the
[`pom_file_generation`](../examples/pom_file_generation/) example for a complete
POM setup.

## Declare direct dependencies explicitly

Generated imports depend on their Maven transitive dependencies, but their
compile classes are not exported transitively. A target using Hamcrest classes
should list Hamcrest directly even if JUnit also depends on it:

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

This keeps BUILD files correct when the transitive graph changes.

## Artifact label helpers

The `artifact` macro translates Maven coordinates into a versionless target
label:

```starlark
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "example",
    deps = [artifact("junit:junit")],
)
```

The returned label names the generated alias for the corresponding `jvm_import`
or `aar_import` target, including the transitive dependencies from its POM.

Pass `repository_name` when the installation has a nondefault name. Full
`groupId:artifactId:[packaging:[classifier:]]version` coordinates are also
accepted.

Literal labels are easier for tools such as Buildozer to rewrite, because a
macro hides the target label from syntax-aware tooling.

`java_plugin_artifact` returns a generated `java_plugin` label for an annotation
processor:

```starlark
load("@rules_jvm_external//:defs.bzl", "java_plugin_artifact")

java_library(
    name = "some_lib",
    srcs = ["SrcUsingAuto.java"],
    plugins = [
        java_plugin_artifact(
            "com.google.auto.value:auto-value",
            "com.google.auto.value.processor.AutoValueProcessor",
        ),
    ],
)
```

## Override an artifact target

Redirect a generated artifact label to a target in the main repository with
`maven.override`:

```starlark
maven.override(
    coordinates = "com.google.guava:guava",
    target = "@//third_party/guava:guava",
)
```

The `@//` prefix resolves the label in the main repository rather than the
generated Maven repository. The originally resolved artifact remains available
with an `original_` prefix, such as
`@maven//:original_com_google_guava_guava`.

This override is useful when a generated target needs extra dependencies, such
as a default implementation for an interface, without changing every target
that consumes the artifact.

Use the `visibility` attribute on `maven.override` to control the generated
alias target's visibility.

## Hide transitive dependencies

Transitive artifacts are visible by default. This can cause surprises when an
update removes a transitive dependency from the graph. Set `strict_visibility`
to require BUILD authors to declare every directly referenced artifact:

```starlark
maven.install(
    # ...
    strict_visibility = True,
)
```

Transitive targets become private by default. Use `strict_visibility_value` to
grant them to selected packages instead:

```starlark
maven.install(
    # ...
    strict_visibility = True,
    strict_visibility_value = ["//third_party/jvm:__subpackages__"],
)
```

## Compatibility repository aliases

Rules such as `maven_jar` and `jvm_import_external` historically generated
labels in the `@group_artifact//jar` form, while `java_import_external` used
`@group_artifact//:group_artifact`. Some Bazel projects still expect these
forms. Set `generate_compat_repositories` to create aliases in addition to the
normal `@maven//:group_artifact` labels:

```starlark
maven.install(
    # ...
    generate_compat_repositories = True,
)
```

For example, `@maven//:com_google_guava_guava` is also available as
`@com_google_guava_guava//jar`,
`@com_google_guava_guava//:com_google_guava_guava`, and
`@com_google_guava_guava`.

## Inspect every resolved artifact

The generated `@maven//:defs.bzl` exports `maven_artifacts`, a list of complete
resolved coordinates:

```starlark
load("@maven//:defs.bzl", "maven_artifacts")
load("@rules_jvm_external//:defs.bzl", "artifact")
load("@rules_jvm_external//:specs.bzl", "parse")

all_jar_coordinates = [
    coordinate
    for coordinate in maven_artifacts
    if parse.parse_maven_coordinate(coordinate).get("packaging", "jar") == "jar"
]

all_jar_targets = [artifact(coordinate) for coordinate in all_jar_coordinates]
```

Prefer explicit direct dependencies for ordinary BUILD targets. The complete
list is intended for tooling and aggregate operations.
