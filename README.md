# gmaven_rules

This project generates `gmaven.bzl`, a file containing `maven_jar` and `maven_aar`
rules for all artifacts in [https://maven.google.com](https://maven.google.com).

# Support Policy

This project is an **interim solution** during which Google Maven and AAR
support is added to [bazel-deps](https://github.com/johnynek/bazel-deps).

# Usage instructions

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

# Regenerating targets

You can see the full list of generated targets in [`gmaven.bzl`](https://raw.githubusercontent.com/aj-michael/gmaven_rules/master/gmaven.bzl).

To regenerate `gmaven.bzl`, run the following command. It will take about 5 minutes.

```
rm gmaven.bzl && javac GMavenToBazel.java && java GMavenToBazel
```


# Known issues

Some of the artifacts depend on other artifacts that are not present on Google
Maven. These targets do not work as the cross-repository resolution is not
implemented.
