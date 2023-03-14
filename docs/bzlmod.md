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
maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
maven.install(
    artifacts = [
        # This line is an example coordinate, you'd copy-paste your actual dependencies here
        # from your build.gradle or pom.xml file.
        "org.seleniumhq.selenium:selenium-java:4.4.0",
    ],
)
use_repo(maven, "maven")
```

Now you can run the `@maven//:pin` program to create a JSON lockfile of the transitive dependencies,
in a format that rules_jvm_external can use later. You'll check this file into the repository.

```sh
$ bazel run @maven//:pin
```

Ignore the instructions printed at the end of the output from this command, as they aren't updated
for bzlmod yet. See [#836](https://github.com/bazelbuild/rules_jvm_external/issues/836)

Due to [#835](https://github.com/bazelbuild/rules_jvm_external/issues/835) this creates a file with
a longer name than it should, so we rename it:

```sh
$ mv rules_jvm_external~4.5~maven~maven_install.json maven_install.json
```

Now that this file exists, we can update the `MODULE.bazel` to reflect that we pinned the dependencies.

Add a `lock_file` attribute to the `maven.install()` call like so:
```starlark
maven.install(
    ...
    lock_file = "//:maven_install.json",
)
```

Finally, update the `use_repo` call to also expose the `unpinned_maven` repository used to update the dependencies:

```starlark
use_repo(maven, "maven", "unpinned_maven")
```

Now you'll be able to use the same `@unpinned_maven//:pin` operation described in the
[workspace instructions](/README.md#updating-maven_installjson).

## Known issues

- Some error messages print instructions that don't apply under bzlmod, e.g. https://github.com/bazelbuild/rules_jvm_external/issues/827
- The `java_grpc_library` rule [isn't available](https://github.com/bazelbuild/bazel-central-registry/issues/353)
- Java Gazelle extension [isn't available](https://github.com/bazel-contrib/rules_jvm/issues/123)
