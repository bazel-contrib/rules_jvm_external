Add to WORKSPACE:

```python
load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@rules_jvm_external//migration:maven_jar_migrator_deps.bzl", "maven_jar_migrator_repositories")
maven_jar_migrator_repositories()
```

Run command in root of project workspace to generate the `maven_install` WORKSPACE snippet: 

```
$ bazel run @rules_jvm_external//migration:maven_jar
```

If the snippet looks good, concatenate it to the end of your WORKSPACE file:

```
$ bazel run @rules_jvm_external//migration:maven_jar >> WORKSPACE
```

Finally, if the build continues to succeed, you can remove
`maven_jar_migrator_repositories` from your WORKSPACE file.
