# Using rules_jvm_external with bzlmod

The Bzlmod guide has been split into topic pages. This file retains the old
section headings so existing links continue to reach a useful destination.

Use the [documentation index](README.md) for the complete navigation.

## Installation

Installation and the canonical lock-file setup are now in the
[quickstart](../README.md#quickstart). It declares the lock file before pinning
and applies to Coursier, Maven, and Gradle.

## Extension and tag documentation

The generated tag-class reference is now the
[module extension API](extension-api.md).

## Declaring dependencies in files

Gradle version catalogs are documented under
[Gradle version catalogs](declaring-artifacts.md#gradle-version-catalogs).

#### Extensions to the Gradle version catalog format

The supported additional catalog fields and examples moved to
[Gradle version catalogs](declaring-artifacts.md#gradle-version-catalogs).

### Declaring BOMs from external files

The `is_bom` and `bom_modules` forms moved to
[Maven BOMs](declaring-artifacts.md#maven-boms).

## Artifact exclusion

Per-artifact and global exclusions moved to
[Exclusions](declaring-artifacts.md#exclusions).

## Modifying artifact declarations

The `maven.amend_artifact` workflow moved to
[Amending an existing declaration](declaring-artifacts.md#amending-an-existing-declaration).

## Module dependency layering

The full explanation of cross-module contributions, root precedence, warnings,
and conflict handling moved to
[Module dependency layering](module-dependency-layering.md).

## Known issues

Current diagnostic guidance is in [Troubleshooting](troubleshooting.md).
