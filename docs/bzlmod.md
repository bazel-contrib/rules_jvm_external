# Using rules_jvm_external with bzlmod

Bzlmod is the new package manager for Bazel modules, included in Bazel 6.0.
It allows a significantly shorter setup than the `WORKSPACE` file used prior to bzlmod.

Note: this support is new as of early 2023, so expect some brokenness and missing features.
Please do file issues for missing bzlmod support.

See the `/examples/bzlmod` folder in this repository for a complete, tested example.

## Installation

First, you must enable bzlmod.
Note, the Bazel team plans to enable it by default starting in version 7.0.
The simplest way is by adding this line to your `.bazelrc`:

```
common --enable_bzlmod
```

Now, create a `MODULE.bazel` file in the root of your workspace,
setting the `version` to the latest one available on https://registry.bazel.build/modules/rules_jvm_external:

```starlark
bazel_dep(name = "rules_jvm_external", version = "...")
your_maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
your_maven.install(
    # NB: Do *ALWAYS* specify a name, and NEVER use the (default) name = "maven";
    # this is because it's not "namespaced" (or "scoped") like you might think it
    # really should be... use the default name *WILL* cause conflicts with other modules;
    # notably grpc-java! See e.g. https://github.com/bazel-contrib/rules_jvm_external/issues/916
    # and https://github.com/bazel-contrib/rules_jvm_external/issues/708 for more background.
    name = "YOUR_maven",
    artifacts = [
        # This line is an example coordinate, you'd copy-paste your actual dependencies here
        # from your build.gradle or pom.xml file.
        "org.seleniumhq.selenium:selenium-java:4.4.0",
    ],
)

# You can split off individual artifacts to define artifact-specific options (this example sets `neverlink`).
# The `your_maven.install` and `your_maven.artifact` tags will be merged automatically.
your_maven.artifact(
    # TODO? name = "YOUR_maven",
    artifact = "javapoet",
    group = "com.squareup",
    neverlink = True,
    version = "1.11.1",
)

use_repo(your_maven, "YOUR_maven")
```

Now you can run the `@maven//:pin` program to create a JSON lockfile of the transitive dependencies,
in a format that rules_jvm_external can use later. You'll check this file into the repository.

```sh
$ bazel run @your_maven//:pin
```

Ignore the instructions printed at the end of the output from this command, as they aren't updated
for bzlmod yet. See [#836](https://github.com/bazelbuild/rules_jvm_external/issues/836)

Due to [#835](https://github.com/bazelbuild/rules_jvm_external/issues/835) this creates a file with
a longer name than it should, so we rename it:

```sh
$ mv rules_jvm_external~4.5~maven~maven_install.json maven_install.json
```

Now that this file exists, we can update the `MODULE.bazel` to reflect that we pinned the dependencies.

Add a `lock_file` attribute to the `your_maven.install()` call like so:

```starlark
your_maven.install(
    ...
    lock_file = "//:maven_install.json",
)
```

Now you'll be able to use the same `REPIN=1 bazel run @maven//:pin` operation described in the
[workspace instructions](/README.md#updating-maven_installjson) to update the dependencies.

## Dependencing on Artifacts

Now in `BUILD` files you can:

```starlark

java_library(
    (...)
    deps = [
        "@YOUR_maven//:com_squareup_javapoet",
```

Note how you need to use `@YOUR_maven` instead of just `@maven`, and replace `:` with `_`, and omit the version.

## Artifact exclusion

The non-bzlmod instructions for how to configure `exclusions` [from the README](../README.md#artifact-exclusion)
don't work as shown for bzlmod; it's not possible to "inline" them as shown (it will cause an `ERROR: in tag at
<root>/MODULE.bazel:22:14, error converting value for attribute artifacts: expected value of type 'string' for
element 9 of artifacts, but got None (NoneType)`). Split it like this instead:

```starlark
# https://github.com/grpc/grpc-java/issues/10576
your_maven.artifact(
    artifact = "grpc-core",
    exclusions = ["io.grpc:grpc-util"],
    group = "io.grpc",
    version = "1.58.0",  # Keep version in sync with below!
)
your_maven.install(
    artifacts = [
        "junit:junit:4.13.2",
        ...
```

## Known issues

- Some error messages print instructions that don't apply under bzlmod, e.g. https://github.com/bazelbuild/rules_jvm_external/issues/827
