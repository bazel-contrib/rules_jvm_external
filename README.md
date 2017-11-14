A bzl file that contains maven_jar and maven_aar rules for all artifacts in
https://maven.google.com. Not guaranteed to be up to date.

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

Note that this requires a Bazel binary containing the changes in
https://github.com/bazelbuild/bazel/commit/431b6436373c9feb5d03e488ff72f822bbe55b2d
