# gmaven_rules

This project generates `gmaven.bzl`, a file containing `maven_jar` and `maven_aar`
rules for all artifacts in [https://maven.google.com](https://maven.google.com).

# Support Policy

This project is an interim solution that is supported until Google Maven and
AAR support is added to [bazel-deps](https://github.com/johnynek/bazel-deps).

# Usage instructions

To use this from your project, in your `WORKSPACE` file add

```
# Google Maven Repository
GMAVEN_TAG = "0.1.0"

http_archive(
    name = "gmaven_rules",
    strip_prefix = "gmaven_rules-%s" % GMAVEN_TAG,
    url = "https://github.com/bazelbuild/gmaven_rules/archive/%s.tar.gz" % GMAVEN_TAG,
)

load("@gmaven_rules//:gmaven.bzl", "gmaven_rules")

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

You can see the full list of generated targets in [`gmaven.bzl`](https://raw.githubusercontent.com/aj-michael/gmaven_rules/master/gmaven.bzl).

# Updating gmaven.bzl

To update `gmaven.bzl`, run the following command. It will take about 5 minutes.

```
rm gmaven.bzl && javac GMavenToBazel.java && java GMavenToBazel
```


# Known issues

Currently, cross-repository dependency resolution is not supported. Some of the
artifacts depend on other artifacts that are not present on Google Maven, and
these missing dependencies are silently ignored and may cause failures at
runtime. 
