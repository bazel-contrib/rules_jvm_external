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