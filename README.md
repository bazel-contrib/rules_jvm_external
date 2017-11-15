A bzl file that contains maven_jar and maven_aar rules for all artifacts in
https://maven.google.com. Not guaranteed to be correct or up to date. Some of
the artifacts depend on artifacts that are not present on
https://maven.google.com. We ignore these and hope not to fail.

To use this from your project, in your `WORKSPACE` file add

```
git_repository(
    name = 'gmaven_rules',
    remote = 'https://github.com/aj-michael/gmaven_rules',
    commit = '<FILL IN A COMMIT HERE>',
)
load('@gmaven_rules//:gmaven.bzl', 'gmaven_rules')
gmaven_rules()
```

To regenerate gmaven.bzl, run

```
rm gmaven.bzl && javac GMavenToBazel.java && java GMavenToBazel
```
