<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
# API Reference

- [Basic functions](#basic-functions)
  - [javadoc](#javadoc)
  - [java_export](#java_export)
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

<a name="#javadoc"></a>

## javadoc

<pre>
javadoc(<a href="#javadoc-name">name</a>, <a href="#javadoc-deps">deps</a>)
</pre>

Generate a javadoc from all the `deps`

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :-------------: | :-------------: | :-------------: | :-------------: | :-------------: |
| name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
| deps |  The java libraries to generate javadocs for.<br><br>          The source jars of each dep will be used to generate the javadocs.           Currently docs for transitive dependencies are not generated.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | required |  |


<a name="#java_export"></a>

## java_export

<pre>
java_export(<a href="#java_export-name">name</a>, <a href="#java_export-maven_coordinates">maven_coordinates</a>, <a href="#java_export-deploy_env">deploy_env</a>, <a href="#java_export-pom_template">pom_template</a>, <a href="#java_export-visibility">visibility</a>, <a href="#java_export-tags">tags</a>, <a href="#java_export-kwargs">kwargs</a>)
</pre>

Extends `java_library` to allow maven artifacts to be uploaded.

This macro can be used as a drop-in replacement for `java_library`, but
also generates an implicit `name.publish` target that can be run to publish
maven artifacts derived from this macro to a maven repository. The publish
rule understands the following variables (declared using `--define` when
using `bazel run`):

  * `maven_repo`: A URL for the repo to use. May be "https" or "file".
  * `maven_user`: The user name to use when uploading to the maven repository.
  * `maven_password`: The password to use when uploading to the maven repository.

This macro also generates a `name-pom` target that creates the `pom.xml` file
associated with the artifacts. The template used is derived from the (optional)
`pom_template` argument, and the following substitutions are performed on
the template file:

  * `{groupId}`: Replaced with the maven coordinates group ID.
  * `{artifactId}`: Replaced with the maven coordinates artifact ID.
  * `{version}`: Replaced by the maven coordinates version.
  * `{type}`: Replaced by the maven coordintes type, if present (defaults to "jar")
  * `{dependencies}`: Replaced by a list of maven dependencies directly relied upon
    by java_library targets within the artifact.

The "edges" of the artifact are found by scanning targets that contribute to
runtime dependencies for the following tags:

  * `maven_coordinates=group:artifact:type:version`: Specifies a dependency of
    this artifact.
  * `maven:compile_only`: Specifies that this dependency should not be listed
    as a dependency of the artifact being generated.

Generated rules:
  * `name`: A `java_library` that other rules can depend upon.
  * `name-docs`: A javadoc jar file.
  * `name-pom`: The pom.xml file.
  * `name.publish`: To be executed by `bazel run` to publish to a maven repo.


**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  A unique name for this target   |  none |
| maven_coordinates |  The maven coordinates for this target.   |  none |
| deploy_env |  A list of labels of java targets to exclude from the generated jar   |  <code>[]</code> |
| pom_template |  The template to be used for the pom.xml file.   |  <code>None</code> |
| visibility |  The visibility of the target   |  <code>None</code> |
| tags |  <p align="center"> - </p>   |  <code>[]</code> |
| kwargs |  These are passed to [<code>java_library</code>](https://docs.bazel.build/versions/master/be/java.html#java_library),   and so may contain any valid parameter for that rule.   |  none |


<a name="#maven_install"></a>

## maven_install

<pre>
maven_install(<a href="#maven_install-name">name</a>, <a href="#maven_install-repositories">repositories</a>, <a href="#maven_install-artifacts">artifacts</a>, <a href="#maven_install-fail_on_missing_checksum">fail_on_missing_checksum</a>, <a href="#maven_install-fetch_sources">fetch_sources</a>, <a href="#maven_install-fetch_javadoc">fetch_javadoc</a>,
              <a href="#maven_install-use_unsafe_shared_cache">use_unsafe_shared_cache</a>, <a href="#maven_install-excluded_artifacts">excluded_artifacts</a>, <a href="#maven_install-generate_compat_repositories">generate_compat_repositories</a>,
              <a href="#maven_install-version_conflict_policy">version_conflict_policy</a>, <a href="#maven_install-maven_install_json">maven_install_json</a>, <a href="#maven_install-override_targets">override_targets</a>, <a href="#maven_install-strict_visibility">strict_visibility</a>,
              <a href="#maven_install-resolve_timeout">resolve_timeout</a>, <a href="#maven_install-jetify">jetify</a>, <a href="#maven_install-jetify_include_list">jetify_include_list</a>, <a href="#maven_install-additional_netrc_lines">additional_netrc_lines</a>,
              <a href="#maven_install-fail_if_repin_required">fail_if_repin_required</a>, <a href="#maven_install-use_starlark_android_rules">use_starlark_android_rules</a>, <a href="#maven_install-aar_import_bzl_label">aar_import_bzl_label</a>)
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
| fetch_javadoc |  Additionally fetch javadoc JARs.   |  <code>False</code> |
| use_unsafe_shared_cache |  Download artifacts into a persistent shared cache on disk. Unsafe as Bazel is   currently unable to detect modifications to the cache.   |  <code>False</code> |
| excluded_artifacts |  A list of Maven artifact coordinates in the form of <code>group:artifact</code> to be   excluded from the transitive dependencies.   |  <code>[]</code> |
| generate_compat_repositories |  Additionally generate repository aliases in a .bzl file for all JAR   artifacts. For example, <code>@maven//:com_google_guava_guava</code> can also be referenced as   <code>@com_google_guava_guava//jar</code>.   |  <code>False</code> |
| version_conflict_policy |  Policy for user-defined vs. transitive dependency version   conflicts.  If "pinned", choose the user's version unconditionally.  If "default", follow   Coursier's default policy.   |  <code>"default"</code> |
| maven_install_json |  A label to a <code>maven_install.json</code> file to use pinned artifacts for generating   build targets. e.g <code>//:maven_install.json</code>.   |  <code>None</code> |
| override_targets |  A mapping of <code>group:artifact</code> to Bazel target labels. All occurrences of the   target label for <code>group:artifact</code> will be an alias to the specified label, therefore overriding   the original generated <code>jvm_import</code> or <code>aar_import</code> target.   |  <code>{}</code> |
| strict_visibility |  Controls visibility of transitive dependencies. If <code>True</code>, transitive dependencies   are private and invisible to user's rules. If <code>False</code>, transitive dependencies are public and   visible to user's rules.   |  <code>False</code> |
| resolve_timeout |  The execution timeout of resolving and fetching artifacts.   |  <code>600</code> |
| jetify |  Runs the AndroidX [Jetifier](https://developer.android.com/studio/command-line/jetifier) tool on artifacts specified in jetify_include_list. If jetify_include_list is not specified, run Jetifier on all artifacts.   |  <code>False</code> |
| jetify_include_list |  List of artifacts that need to be jetified in <code>groupId:artifactId</code> format. By default all artifacts are jetified if <code>jetify</code> is set to True.   |  <code>["*"]</code> |
| additional_netrc_lines |  Additional lines prepended to the netrc file used by <code>http_file</code> (with <code>maven_install_json</code> only).   |  <code>[]</code> |
| fail_if_repin_required |  Whether to fail the build if the required maven artifacts have been changed but not repinned. Requires the <code>maven_install_json</code> to have been set.   |  <code>False</code> |
| use_starlark_android_rules |  Whether to use the native or Starlark version   of the Android rules. Default is False.   |  <code>False</code> |
| aar_import_bzl_label |  The label (as a string) to use to import aar_import   from. This is usually needed only if the top-level workspace file does   not use the typical default repository name to import the Android   Starlark rules. Default is   "@build_bazel_rules_android//rules:rules.bzl".   |  <code>"@build_bazel_rules_android//android:rules.bzl"</code> |


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


