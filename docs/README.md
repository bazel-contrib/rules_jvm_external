# Documentation

Start with the [repository quickstart](../README.md#quickstart), then use these
topic guides for the rest of the workflow.

## Dependency setup

* [Declaring Maven artifacts](declaring-artifacts.md) covers coordinates, BOMs,
  Gradle version catalogs, exclusions, AARs, and auxiliary artifacts.
* [Dependency resolution](resolution.md) covers Coursier, Maven, Gradle,
  conflicts, and isolated dependency trees.
* [Lock files and pinning](lock-files.md) covers initial pinning, updates,
  multiple lock files, and outdated dependencies.
* [Module dependency layering](module-dependency-layering.md) explains how
  dependencies contributed by Bazel modules are combined and controlled.

## Repositories and downloads

* [Private Maven repositories](private-repositories.md) covers credentials,
  netrc files, and proxies.
* [Mirrors and offline operation](mirrors-offline.md) covers pinned download
  URLs, caches, `m2local`, and disconnected builds.

## Generated targets and tools

* [Generated and customized targets](customizing-targets.md) covers labels,
  overrides, visibility, aliases, and generated metadata.
* [Tooling integration](tooling.md) covers IDE use and Java Gazelle indexes.
* [Publishing Maven artifacts](publishing.md) covers Java and Kotlin export
  rules, POM generation, remote repositories, and signing.

## Upgrading and support

* [Migrating from WORKSPACE](migrating-from-workspace.md) maps the legacy
  repository macro to the Bzlmod extension.
* [Troubleshooting](troubleshooting.md) collects common resolution, lock-file,
  downloader, Java, Android, and network problems.

## API references

* [Module extension API](extension-api.md) is generated from the extension tag
  classes.
* [Rules and macros API](api.md) is generated from the public exports in
  `defs.bzl` and `kt_defs.bzl`.

`bzlmod.md` is retained as an [anchor-preserving migration map](bzlmod.md) for
existing links. New links should point directly to the topic guides above.
