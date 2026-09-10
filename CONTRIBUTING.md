# Contributing

Want to contribute? Great! First, read this page (including the small print at the end).

## Before you contribute

**Before we can use your code, you must sign the
[Google Individual Contributor License Agreement](https://cla.developers.google.com/about/google-individual?csw=1)
(CLA)**, which you can do online.

The CLA is necessary mainly because you own the copyright to your changes,
even after your contribution becomes part of our codebase, so we need your
permission to use and distribute your code. We also need to be sure of
various other things — for instance that you'll tell us if you know that
your code infringes on other people's patents. You don't have to sign
the CLA until after you've submitted your code for review and a member has
approved it, but you must do it before we can put your code into our codebase.

Before you start working on a larger contribution, you should get in touch
with us first. Use the issue tracker to explain your idea so we can help and
possibly guide you.

## Development setup

### Verbose output

Set `RJE_VERBOSE` to print additional dependency-resolution diagnostics:

```shell
RJE_VERBOSE=1 bazel run @maven//:pin
```

### Tests

An Android SDK is required to run the complete test suite. Install it with
[Android Studio](https://developer.android.com/studio) or a system package
manager, then run:

```shell
bazel test //...
```

On macOS with [Homebrew](https://brew.sh), the following installs the required
SDK components and configures the environment:

```shell
brew install android-commandlinetools
export ANDROID_HOME="$(brew --prefix)/share/android-commandlinetools"
sdkmanager "build-tools;33.0.1" "cmdline-tools;latest" "ndk;21.4.7075529" "platform-tools" "platforms;android-33"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/21.4.7075529"
```

Add the environment exports to your shell configuration if they should persist.

### Generated API documentation

The checked-in API references are generated with
[Stardoc](https://github.com/bazelbuild/stardoc). Update
every generated reference with:

```shell
bazel run //docs:update
```

Run the drift test before sending a change for review:

```shell
bazel test //docs:generated_docs_test
```

The test prints a separate diff for each stale generated file.

## Code reviews and other contributions

**All submissions, including submissions by project members, require review.**
Please follow the instructions in [the contributors documentation](https://bazel.build/contributing.html).

## The small print

Contributions made by corporations are covered by a different agreement than
the one above, the
[Software Grant and Corporate Contributor License Agreement](https://cla.developers.google.com/about/google-corporate).
