# `maven_jar` migration tool

`maven_jar` will be removed from Bazel 2.0 onwards. See [this issue](https://github.com/bazelbuild/bazel/issues/6799) for more information.

This tool helps to automatically migrate your project from `maven_jar` to `maven_install` provided by `rules_jvm_external`.

These PRs were generated using this tool:

* https://github.com/google/copybara/pull/96
* https://github.com/kythe/kythe/pull/4180
* https://github.com/bazelbuild/rules_k8s/pull/463

## Prerequisites

* [Buildozer](https://github.com/bazelbuild/buildtools/releases). Ensure that the `buildozer` is available in your `PATH`.

## Usage

First, add the latest version of `rules_jvm_external` to your WORKSPACE by
following the instructions on the
[releases](https://github.com/bazelbuild/rules_jvm_external/releases) page.

Then, add the following to your WORKSPACE to load the migrator's
dependencies:

```python
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//migration:maven_jar_migrator_deps.bzl", "maven_jar_migrator_repositories")
maven_jar_migrator_repositories()
```

Next, run this command in root of project workspace to generate the
`maven_install` WORKSPACE snippet:

```
$ bazel run @rules_jvm_external//migration:maven_jar
```

This command will also run the buildozer commands to migrate your project to
use the new `maven_install` labels.

If the snippet looks good, concatenate it to the end of your WORKSPACE file:

```
$ bazel run @rules_jvm_external//migration:maven_jar >> WORKSPACE
```

Finally, if the build continues to succeed, you can remove
`maven_jar_migrator_repositories` from your WORKSPACE file.