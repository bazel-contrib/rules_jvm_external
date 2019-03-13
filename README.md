# rules_jvm_external

Transitive Maven artifact resolver as a repository rule.

> If you're looking for documentation on gmaven_rules, [go here](#deprecated-gmaven_rules).

## Features

* WORKSPACE configuration
* JAR, AAR, source JARs
* Custom Maven repositories
* Private Maven repositories with HTTP Basic Authentication
* Artifact version resolution with Coursier
* Reuse artifacts from a central cache
* Versionless target labels for simpler dependency management
* Ability to declare multiple sets of versioned artifacts
* Supported on Windows, macOS, Linux

## Usage

List the top-level Maven artifacts and servers in the WORKSPACE:

```python
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "1.0"
RULES_JVM_EXTERNAL_SHA = "48e0f1aab74fabba98feb8825459ef08dcc75618d381dff63ec9d4dd9860deaa"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "junit:junit:4.12",
        "androidx.test.espresso:espresso-core:3.1.1",
    ],
    repositories = [
        # Private repositories are supported through HTTP Basic auth
        "http://username:password@localhost:8081/artifactory/my-repository",
        "https://bintray.com/bintray/jcenter",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
    # Fetch srcjars. Defaults to False.
    fetch_sources = True,
)
```

and use them directly in the BUILD file by specifying the versionless target alias label:

```python
load("@rules_jvm_external//:defs.bzl", "artifact")

android_library(
    name = "test_deps",
    exports = [
        "@maven//:androidx_test_espresso_espresso_core",
        # or artifact("androidx.test.espresso:espresso-core"),
        "@maven//:junit_junit",
        # or artifact("junit:junit"),
    ],
)
```

The `artifact` macro translates the artifact's `group-id:artifact:id` to the
label of the versionless target. This target is an
[alias](https://docs.bazel.build/versions/master/be/general.html#alias) that
points to the `java_import`/`aar_import` target in the `@maven` repository,
which includes the transitive dependencies specified in the top level artifact's
POM file.

For the `junit:junit` example, the following targets will be generated:

```python
alias(
  name = "junit_junit",
  actual = "@maven//:junit_junit_4_12",
)

java_import(
  name = "junit_junit_4_12",
  jars = ["@maven//:https/repo1.maven.org/maven2/junit/junit/4.12/junit-4.12.jar"],
  srcjar = "@maven//:https/repo1.maven.org/maven2/junit/junit/4.12/junit-4.12-sources.jar",
  deps = ["@maven//:org_hamcrest_hamcrest_core_1_3"],
  tags = ["maven_coordinates=junit:junit:4.12"],
)

java_import(
  name = "org_hamcrest_hamcrest_core_1_3",
  jars = ["@maven//:https/repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"],
  srcjar = "@maven//:https/repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3-sources.jar",
  deps = [],
  tags = ["maven_coordinates=org.hamcrest:hamcrest.library:1.3"],
)
```

## Advanced usage

### Using a persistent artifact cache

To download artifacts into a shared and persistent directory in your home
directory, specify `use_unsafe_shared_cache = True` in `maven_install`:

```python
maven_install(
    name = "maven",
    artifacts = [
        # ...
    ],
    repositories = [
        # ...
    ],
    use_unsafe_shared_cache = True,
)
```

This is **not safe** as Bazel is currently not able to detect changes in the
shared cache. For example, if an artifact is deleted from the shared cache,
Bazel will not re-run the repository rule automatically.

The default value of `use_unsafe_shared_cache` is `False`. This means that Bazel
will create independent caches for each `maven_install` repository, located at
`$(bazel info output_base)/external/@repository_name/v1`.

### Multiple `maven_install` declarations for isolated artifact version trees

If your WORKSPACE contains several projects that use different versions of the
same artifact, you can specify multiple `maven_install` declarations in the
WORKSPACE, with a unique repository name for each of them.

For example, if you want to use the JRE version of Guava for a server app, and
the Android version for an Android app, you can specify two `maven_install`
declarations:

```python
maven_install(
    name = "server_app",
    artifacts = [
        "com.google.guava:guava:27.0-jre",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "android_app",
    artifacts = [
        "com.google.guava:guava:27.0-android",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)
```

This way, `rules_jvm_external` will invoke coursier to resolve artifact versions for
both repositories independent of each other. Coursier will fail if it encounters
version conflicts that it cannot resolve. The two Guava targets can then be used
in BUILD files like so:

```python
load("@rules_jvm_external//:defs.bzl", "artifact")

java_binary(
    name = "my_server_app",
    srcs = ...
    deps = [
        # a versionless alias to @server_app//:com_google_guava_guava_27_0_jre
        "@server_app//:com_google_guava_guava",
        # or artifact("com.google.guava:guava", repository_name = "server_app")
    ]
)

android_binary(
    name = "my_android_app",
    srcs = ...
    deps = [
        # a versionless alias to @android_app//:com_google_guava_guava_27_0_android
        "@android_app//:com_google_guava_guava",
        # or artifact("com.google.guava:guava", repository_name = "android_app")
    ]
)
```

### Detailed dependency information specifications

Although you can always give a dependency as a Maven coordinate string,
occasionally special handling is required in the form of additional directives
to properly situate the artifact in the dependency tree. For example, a given
artifact may need to have one of its dependencies excluded to prevent a
conflict.

This situation is provided for by allowing the artifact to be specified as a map
containing all of the required information. This map can express more
information than the coordinate strings can, so internally the coordinate
strings are parsed into the artifact map with default values for the additional
items. To assist in generating the maps, you can pull in the file `specs.bzl`
alongside `defs.bzl` and import the `maven` struct, which provides several
helper functions to assist in creating these maps. An example:

```python
load("@rules_jvm_external//:defs.bzl", "artifact")
load("@rules_jvm_external//:specs.bzl", "maven")

maven_install(
    artifacts = [
        maven.artifact(
            group = "com.google.guava",
            artifact = "guava",
            version = "27.0-android",
            exclusions = [
                ...
            ]
        ),
        "junit:junit:4.12",
        ...
    ],
    repositories = [
        maven.repository(
            "https://some.private.maven.re/po",
            user = "bob",
            password = "l0bl4w"
        ),
        "https://repo1.maven.org/maven2",
        ...
    ],
)
```

### Artifact exclusion

If you want to exclude an artifact from the transitive closure of a top level
artifact, specify its `group-id:artifact-id` in the `exclusions` attribute of
the `maven.artifact` helper:

```python
load("@rules_jvm_external//:specs.bzl", "maven")

maven_install(
    artifacts = [
        maven.artifact(
            group = "com.google.guava",
            artifact = "guava",
            version = "27.0-jre",
            exclusions = [
                maven.exclusion(
                    group = "org.codehaus.mojo",
                    artifact = "animal-sniffer-annotations"
                ),
                "com.google.j2objc:j2objc-annotations",
            ]
        ),
        # ...
    ],
    repositories = [
        # ...
    ],
)
```

You can specify the exclusion using either the `maven.exclusion` helper or the
`group-id:artifact-id` string directly.

## Demo

You can find demos in the [`examples/`](./examples/) directory.

---

# (DEPRECATED) gmaven_rules

This repository also hosts the previous implementation of gmaven_rules, a set of
repository rules to provide support for easily depending on common Android
libraries in Bazel. The previous implementation is now deprecated.

If you were previously depending on `gmaven_rules` using an `http_archive` in
the WORKSPACE function, you may see this error:

```
ERROR: error loading package '': Encountered error while reading extension file 'gmaven.bzl': no such package '@gmaven_rules//': 
java.io.IOException: Prefix gmaven_rules-20180625-1 was given, but not found in the archive
```

To fix this, replace this snippet:

```python
# Google Maven Repository
GMAVEN_TAG = "20181212-2"

http_archive(
    name = "gmaven_rules",
    strip_prefix = "gmaven_rules-%s" % GMAVEN_TAG,
    url = "https://github.com/bazelbuild/gmaven_rules/archive/%s.tar.gz" % GMAVEN_TAG,
)
```

with this snippet:

```python
RULES_JVM_EXTERNAL_TAG = "1.0"
RULES_JVM_EXTERNAL_SHA = "48e0f1aab74fabba98feb8825459ef08dcc75618d381dff63ec9d4dd9860deaa"

http_archive(
    name = "gmaven_rules",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)
```

## Using gmaven_rules

The core of it is `gmaven.bzl`, a file containing external repository targets
for all artifacts in [Google Maven Repository](https://maven.google.com) plus
their dependencies, and the supporting tools for generating it.

We no longer recommend using the previous implementation due to its limitations,
such as not having support for cross-repository dependency resolution. Some of
the artifacts depend on other artifacts that are not present on Google Maven,
and these missing dependencies are silently ignored and may cause failures at
runtime.

However, if you are still depending on it, please see the
[releases](https://github.com/bazelbuild/rules_jvm_external/releases/latest) page for
instructions on using the latest snapshot.

To update `gmaven.bzl`, run the following command. It will take about 3 minutes.

```
bazel run //:gmaven_to_bazel && cp bazel-bin/gmaven_to_bazel.runfiles/__main__/gmaven.bzl .
```
