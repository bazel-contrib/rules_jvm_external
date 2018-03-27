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

You can then reference the generated library targets from your `BUILD` files like:

```
load("@gmaven_rules//:defs.bzl", "gmaven_artifact")
android_library(
    ...
    deps = [
        gmaven_artifact("com.android.support:design:aar:27.0.2"),
        gmaven_artifact("com.android.support:support_annotations:jar:27.0.2"),
        gmaven_artifact("com.android.support.test.espresso:espresso_core:aar:3.0.1"),
    ],
)
```

You can see the full list of generated targets in [gmaven.bzl](https://raw.githubusercontent.com/aj-michael/gmaven_rules/master/gmaven.bzl).

To regenerate gmaven.bzl, run the following command. It will take about 5 minutes.

```
rm gmaven.bzl && javac GMavenToBazel.java && java GMavenToBazel
```
