# Troubleshooting

Start by enabling resolver diagnostics and reproducing the failing fetch or pin:

```shell
RJE_VERBOSE=1 REPIN=1 bazel run @maven//:pin
```

Use the generated repository name in place of `maven` when the installation has
a custom `name`.

## The lock file does not exist

`lock_file` is a Bazel label. Its file and package must exist before the module
extension is evaluated, although the file may be empty:

```shell
touch maven_install.json BUILD.bazel
REPIN=1 bazel run @maven//:pin
```

The pin target writes the declared path directly. Do not pin first and rename a
file afterward. See [Lock files and pinning](lock-files.md).

## The lock file needs repinning

Dependency inputs and their signature changed. Repin with:

```shell
REPIN=1 bazel run @maven//:pin
```

If the message should point to a repository-specific wrapper, set
`repin_instructions` on the root `maven.install` tag. Do not disable
`fail_if_repin_required` merely to accept a stale resolution.

## The lock file has no signature

A lock file created before signatures were added, or edited by hand, may print
one or both of these notes:

```text
NOTE: maven_install.json does not contain a signature of the required artifacts.
NOTE: maven_install.json does not contain a signature entry of the dependency tree.
```

Repin to regenerate the file and both signatures:

```shell
REPIN=1 bazel run @maven//:pin
```

## A generated repository or target is missing

Confirm that the repository is imported from the extension:

```starlark
use_repo(maven, "maven")
```

For a nondefault installation name, use the same name on `maven.install`, in
`use_repo`, and in labels. Inspect available targets with:

```shell
bazel query @maven//:all
```

Artifact labels omit the version and normally contain the group and artifact
IDs. Nonstandard packaging and classifiers other than `sources` or `natives`
may add suffixes. Punctuation becomes underscores.

## A transitive dependency is not visible

When `strict_visibility = True`, code may depend only on direct artifacts unless
`strict_visibility_value` grants broader access. Add the directly used Maven
artifact to the BUILD target's `deps`; this is preferable to depending on a
transitive implementation detail.

## A strict-dependency fix prints a Bzlmod repository name

Some strict-dependency errors still print a Buildozer command that is not
Bzlmod-aware. For example:

```text
buildozer 'add deps @@rules_jvm_external~7.1~maven~maven//:com_google_guava_guava' //client:target
```

Do not copy the command without reviewing its repository label. This known
issue is tracked in
[#827](https://github.com/bazel-contrib/rules_jvm_external/issues/827).
Canonical repository spelling varies by Bazel version, so the separators may
differ from this example.

## Resolution chooses an unexpected version

Check all direct declarations, BOMs, `force_version` values, and contributions
from Bazel dependency modules. The selected version can depend on the resolver.

* Use [Dependency resolution](resolution.md#version-conflicts) for direct versus
  transitive conflicts.
* Use [Module dependency layering](module-dependency-layering.md) when another
  Bazel module contributes the coordinate.
* Repin with `RJE_VERBOSE=1` to show filtered contributors and added non-root
  artifacts.

Do not acknowledge a module in `known_contributing_modules` until its complete
artifact and BOM contribution has been reviewed.

## Duplicate artifacts warn or fail

Remove duplicate direct declarations when possible. Otherwise,
`duplicate_version_warning` controls the behavior:

* `"warn"` reports duplicates and continues.
* `"error"` fails before resolution.
* `"none"` suppresses the check.

Classifier and packaging differences can affect extension layering differently
from the repository-level duplicate check. Inspect the complete coordinates
before suppressing a warning.

## Checksums are missing

Repositories should publish SHA-1 or MD5 checksum files during resolution. If a
repository cannot provide them, set `fail_on_missing_checksum = False` and
review the SHA-256 values written to the lock file carefully.

For pinned builds, a checksum mismatch usually means the upstream file changed
without its URL changing, a proxy or mirror returned different content, or the
cache is corrupt. Compare the lock-file URL and SHA-256 with the repository
before repinning.

## Private repository access fails

Resolution and pinned download are separate network paths. A credential setup
that lets Coursier resolve metadata may still leave Bazel's downloader unable to
fetch the pinned JAR.

Verify both paths using [Private Maven repositories](private-repositories.md):

1. Repin with resolver credentials configured.
2. Build with the lock file and Bazel downloader credentials configured.

Avoid credentials embedded in URLs because those URLs can be recorded in the
lock file.

## A mirror or offline build misses artifacts

Warm the same `--repository_cache` used by the offline environment and fetch the
complete generated repository:

```shell
bazel fetch --repository_cache=/path/to/repository-cache @maven//...
```

Run with `--nofetch` to expose missing cache entries. Remember that Bazel
modules, toolchains, and other external repositories must also be prepared. See
[Mirrors and offline operation](mirrors-offline.md).

## Empty source or Javadoc JARs appear

Some repositories publish zero-byte auxiliary artifacts. Set
`ignore_empty_files = True` to treat empty JARs as missing, then repin.

## Java processes fail under a nonstandard JDK

Use a supported OpenJDK when the system default JDK is a nonstandard
implementation. The wrong default JDK may produce a Robolectric failure
similar to:

```text
java.lang.NullPointerException
    at java.base/jdk.internal.reflect.UnsafeFieldAccessorImpl.ensureObj(UnsafeFieldAccessorImpl.java:58)
    at java.base/jdk.internal.reflect.UnsafeObjectFieldAccessorImpl.get(UnsafeObjectFieldAccessorImpl.java:36)
    at java.base/java.lang.reflect.Field.get(Field.java:418)
    at org.robolectric.shadows.ShadowActivityThread$_ActivityThread_$$Reflector0.getActivities(Unknown Source)
    at org.robolectric.shadows.ShadowActivityThread.reset(ShadowActivityThread.java:277)
    at org.robolectric.Shadows.reset(Shadows.java:2499)
    at org.robolectric.android.internal.AndroidTestEnvironment.resetState(AndroidTestEnvironment.java:640)
    at org.robolectric.RobolectricTestRunner.lambda$finallyAfterTest$0(RobolectricTestRunner.java:361)
    at org.robolectric.util.PerfStatsCollector.measure(PerfStatsCollector.java:86)
    at org.robolectric.RobolectricTestRunner.finallyAfterTest(RobolectricTestRunner.java:359)
    at org.robolectric.internal.SandboxTestRunner$2.lambda$evaluate$2(SandboxTestRunner.java:296)
    at org.robolectric.internal.bytecode.Sandbox.lambda$runOnMainThread$0(Sandbox.java:99)
    at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
    at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1130)
    at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:630)
    at java.base/java.lang.Thread.run(Thread.java:830)
```

It may also produce a native-library failure similar to:

```text
java.lang.UnsatisfiedLinkError: libstdc++.so.6: cannot open shared object file: No such file or directory
    at java.base/java.lang.ClassLoader$NativeLibrary.load0(Native Method)
    at java.base/java.lang.ClassLoader$NativeLibrary.load(ClassLoader.java:2444)
    at java.base/java.lang.ClassLoader$NativeLibrary.loadLibrary(ClassLoader.java:2500)
    at java.base/java.lang.ClassLoader.loadLibrary0(ClassLoader.java:2716)
    at java.base/java.lang.ClassLoader.loadLibrary(ClassLoader.java:2629)
    at java.base/java.lang.Runtime.load0(Runtime.java:769)
    at java.base/java.lang.System.load(System.java:1840)
    at org.conscrypt.NativeLibraryUtil.loadLibrary(NativeLibraryUtil.java:52)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.base/java.lang.reflect.Method.invoke(Method.java:566)
    ...
```

## Java resolver processes need JVM options

Pass JVM options through `JDK_JAVA_OPTIONS` as a repository environment
variable. For example:

```text
build --repo_env=JDK_JAVA_OPTIONS=-Djavax.net.ssl.trustStore=/path/to/cacerts
```

Coursier-specific JVM options belong in the space-separated `COURSIER_OPTS`
environment variable.

## IPv6 connectivity

In an IPv4/IPv6 dual-stack environment, configure both Bazel's downloader and
Coursier:

```text
startup --host_jvm_args=-Djava.net.preferIPv6Addresses=true
build --repo_env=COURSIER_OPTS=-Djava.net.preferIPv6Addresses=true
```

See Bazel's [IPv6 guidance](https://bazel.build/external/faq#how-do-i-make-bazel-prefer-ipv6-in-dual-stack-ipv4ipv6-setups).

## Android AAR imports fail

Confirm that the Android rules are available and that the artifact is resolved
with `aar` packaging. Set `use_starlark_android_rules = True` when native Android
rules are unavailable. If the rules use a nonstandard repository or file, set
`aar_import_bzl_label` to the label that exports `aar_import`.
