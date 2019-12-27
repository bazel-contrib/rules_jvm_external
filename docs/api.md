<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
# API Reference

- [Basic functions](#basic-functions)
  - [maven_install](#maven_install)
- [Maven specification functions](#maven-specification-functions)
  - [maven.repository](#mavenrepository)
  - [maven.artifact](#mavenartifact)
  - [maven.exclusion](#mavenexclusion)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Basic functions

These are the basic functions to get started.

To use these functions, load them at the top of your BUILD file. For example:

```python
load("@rules_jvm_external//:defs.bzl", "maven_install", "artifact")
```
<!-- Generated with Stardoc: http://skydoc.bazel.build -->

<a name="#maven_install"></a>

## maven_install

<pre>
maven_install(<a href="#maven_install-name">name</a>, <a href="#maven_install-repositories">repositories</a>, <a href="#maven_install-artifacts">artifacts</a>, <a href="#maven_install-fail_on_missing_checksum">fail_on_missing_checksum</a>, <a href="#maven_install-fetch_sources">fetch_sources</a>,
              <a href="#maven_install-use_unsafe_shared_cache">use_unsafe_shared_cache</a>, <a href="#maven_install-excluded_artifacts">excluded_artifacts</a>, <a href="#maven_install-generate_compat_repositories">generate_compat_repositories</a>,
              <a href="#maven_install-version_conflict_policy">version_conflict_policy</a>, <a href="#maven_install-maven_install_json">maven_install_json</a>, <a href="#maven_install-override_targets">override_targets</a>, <a href="#maven_install-strict_visibility">strict_visibility</a>,
              <a href="#maven_install-resolve_timeout">resolve_timeout</a>, <a href="#maven_install-jetify">jetify</a>)
</pre>

Resolves and fetches artifacts transitively from Maven repositories.

This macro runs a repository rule that invokes the Coursier CLI to resolve
and fetch Maven artifacts transitively.


**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  A unique name for this Bazel external repository.   |  <code>"maven"</code> |
| repositories |  A list of Maven repository URLs, specified in lookup order.<br><br>  Supports URLs with HTTP Basic Authentication, e.g. "https://username:password@example.com".   |  <code>[]</code> |
| artifacts |  A list of Maven artifact coordinates in the form of <code>group:artifact:version</code>.   |  <code>[]</code> |
| fail_on_missing_checksum |  <p align="center"> - </p>   |  <code>True</code> |
| fetch_sources |  Additionally fetch source JARs.   |  <code>False</code> |
| use_unsafe_shared_cache |  Download artifacts into a persistent shared cache on disk. Unsafe as Bazel is   currently unable to detect modifications to the cache.   |  <code>False</code> |
| excluded_artifacts |  A list of Maven artifact coordinates in the form of <code>group:artifact</code> to be   excluded from the transitive dependencies.   |  <code>[]</code> |
| generate_compat_repositories |  Additionally generate repository aliases in a .bzl file for all JAR   artifacts. For example, <code>@maven//:com_google_guava_guava</code> can also be referenced as   <code>@com_google_guava_guava//jar</code>.   |  <code>False</code> |
| version_conflict_policy |  Policy for user-defined vs. transitive dependency version   conflicts.  If "pinned", choose the user's version unconditionally.  If "default", follow   Coursier's default policy.   |  <code>"default"</code> |
| maven_install_json |  A label to a <code>maven_install.json</code> file to use pinned artifacts for generating   build targets. e.g <code>//:maven_install.json</code>.   |  <code>None</code> |
| override_targets |  A mapping of <code>group:artifact</code> to Bazel target labels. All occurrences of the   target label for <code>group:artifact</code> will be an alias to the specified label, therefore overriding   the original generated <code>jvm_import</code> or <code>aar_import</code> target.   |  <code>{}</code> |
| strict_visibility |  Controls visibility of transitive dependencies. If <code>True</code>, transitive dependencies   are private and invisible to user's rules. If <code>False</code>, transitive dependencies are public and   visible to user's rules.   |  <code>False</code> |
| resolve_timeout |  The execution timeout of resolving and fetching artifacts.   |  <code>600</code> |
| jetify |  Runs the AndroidX jetifier tool on all artifacts.   |  <code>False</code> |


# Maven specification functions

These are helper functions to specify more information about Maven artifacts and
repositories in `maven_install`.

To use these functions, load the `maven` struct at the top of your BUILD file:

```python
load("@rules_jvm_external//:specs.bzl", "maven")
```
<!-- Generated with Stardoc: http://skydoc.bazel.build -->

<a name="#maven.repository"></a>

## maven.repository

<pre>
maven.repository(<a href="#maven.repository-url">url</a>, <a href="#maven.repository-user">user</a>, <a href="#maven.repository-password">password</a>)
</pre>

Generates the data map for a Maven repository specifier given the available information.

If both a user and password are given as arguments, it will include the
access credentials in the repository spec. If one or both are missing, it
will just generate the repository url.


**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| url |  A string containing the repository url (ex: "https://maven.google.com/").   |  none |
| user |  A username for this Maven repository, if it requires authentication (ex: "johndoe").   |  <code>None</code> |
| password |  A password for this Maven repository, if it requires authentication (ex: "example-password").   |  <code>None</code> |


<a name="#maven.artifact"></a>

## maven.artifact

<pre>
maven.artifact(<a href="#maven.artifact-group">group</a>, <a href="#maven.artifact-artifact">artifact</a>, <a href="#maven.artifact-version">version</a>, <a href="#maven.artifact-packaging">packaging</a>, <a href="#maven.artifact-classifier">classifier</a>, <a href="#maven.artifact-override_license_types">override_license_types</a>, <a href="#maven.artifact-exclusions">exclusions</a>,
               <a href="#maven.artifact-neverlink">neverlink</a>, <a href="#maven.artifact-testonly">testonly</a>)
</pre>

Generates the data map for a Maven artifact given the available information about its coordinates.

**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| group |  The Maven artifact coordinate group name (ex: "com.google.guava").   |  none |
| artifact |  The Maven artifact coordinate artifact name (ex: "guava").   |  none |
| version |  The Maven artifact coordinate version name (ex: "27.0-jre").   |  none |
| packaging |  The Maven packaging specifier (ex: "jar").   |  <code>None</code> |
| classifier |  The Maven artifact classifier (ex: "javadoc").   |  <code>None</code> |
| override_license_types |  An array of Bazel license type strings to use for this artifact's rules (overrides autodetection) (ex: ["notify"]).   |  <code>None</code> |
| exclusions |  An array of exclusion objects to create exclusion specifiers for this artifact (ex: maven.exclusion("junit", "junit")).   |  <code>None</code> |
| neverlink |  Determines if this artifact should be part of the runtime classpath.   |  <code>None</code> |
| testonly |  Determines whether this artifact is available for targets not marked as <code>testonly = True</code>.   |  <code>None</code> |


<a name="#maven.exclusion"></a>

## maven.exclusion

<pre>
maven.exclusion(<a href="#maven.exclusion-group">group</a>, <a href="#maven.exclusion-artifact">artifact</a>)
</pre>

Generates the data map for a Maven artifact exclusion.

**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| group |  The Maven group name of the dependency to exclude, e.g. "com.google.guava".   |  none |
| artifact |  The Maven artifact name of the dependency to exclude, e.g. "guava".   |  none |


