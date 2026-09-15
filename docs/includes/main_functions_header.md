# Rules and macros API

These symbols are exported from `@rules_jvm_external//:defs.bzl` and
`@rules_jvm_external//:kt_defs.bzl`.

Load the symbols you need at the top of your BUILD file. For example:

```python
load("@rules_jvm_external//:defs.bzl", "java_export", "maven_bom")
```

## DEFAULT_REPOSITORY_NAME

<pre>
load("@rules_jvm_external//:defs.bzl", "DEFAULT_REPOSITORY_NAME")
</pre>

The default generated Maven repository name, `"maven"`.
