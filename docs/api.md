<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
# API Reference

- [Basic functions](#basic-functions)
  - [maven_install](#maven_install)
    - [Parameters](#parameters)
- [Maven specification functions](#maven-specification-functions)
  - [maven.repository](#mavenrepository)
    - [Parameters](#parameters-1)
  - [maven.artifact](#mavenartifact)
    - [Parameters](#parameters-2)
  - [maven.exclusion](#mavenexclusion)
    - [Parameters](#parameters-3)

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
maven_install(<a href="#maven_install-name">name</a>, <a href="#maven_install-repositories">repositories</a>, <a href="#maven_install-artifacts">artifacts</a>, <a href="#maven_install-fail_on_missing_checksum">fail_on_missing_checksum</a>, <a href="#maven_install-fetch_sources">fetch_sources</a>, <a href="#maven_install-use_unsafe_shared_cache">use_unsafe_shared_cache</a>, <a href="#maven_install-excluded_artifacts">excluded_artifacts</a>, <a href="#maven_install-generate_compat_repositories">generate_compat_repositories</a>, <a href="#maven_install-version_conflict_policy">version_conflict_policy</a>, <a href="#maven_install-maven_install_json">maven_install_json</a>, <a href="#maven_install-override_targets">override_targets</a>, <a href="#maven_install-strict_visibility">strict_visibility</a>)
</pre>

Resolves and fetches artifacts transitively from Maven repositories.

This macro runs a repository rule that invokes the Coursier CLI to resolve
and fetch Maven artifacts transitively.


### Parameters

<table class="params-table">
  <colgroup>
    <col class="col-param" />
    <col class="col-description" />
  </colgroup>
  <tbody>
    <tr id="maven_install-name">
      <td><code>name</code></td>
      <td>
        optional. default is <code>"maven"</code>
        <p>
          A unique name for this Bazel external repository.
        </p>
      </td>
    </tr>
    <tr id="maven_install-repositories">
      <td><code>repositories</code></td>
      <td>
        optional. default is <code>[]</code>
        <p>
          A list of Maven repository URLs, specified in lookup order.

  Supports URLs with HTTP Basic Authentication, e.g. "https://username:password@example.com".
        </p>
      </td>
    </tr>
    <tr id="maven_install-artifacts">
      <td><code>artifacts</code></td>
      <td>
        optional. default is <code>[]</code>
        <p>
          A list of Maven artifact coordinates in the form of `group:artifact:version`.
        </p>
      </td>
    </tr>
    <tr id="maven_install-fail_on_missing_checksum">
      <td><code>fail_on_missing_checksum</code></td>
      <td>
        optional. default is <code>True</code>
      </td>
    </tr>
    <tr id="maven_install-fetch_sources">
      <td><code>fetch_sources</code></td>
      <td>
        optional. default is <code>False</code>
        <p>
          Additionally fetch source JARs.
        </p>
      </td>
    </tr>
    <tr id="maven_install-use_unsafe_shared_cache">
      <td><code>use_unsafe_shared_cache</code></td>
      <td>
        optional. default is <code>False</code>
        <p>
          Download artifacts into a persistent shared cache on disk. Unsafe as Bazel is
  currently unable to detect modifications to the cache.
        </p>
      </td>
    </tr>
    <tr id="maven_install-excluded_artifacts">
      <td><code>excluded_artifacts</code></td>
      <td>
        optional. default is <code>[]</code>
        <p>
          A list of Maven artifact coordinates in the form of `group:artifact` to be
  excluded from the transitive dependencies.
        </p>
      </td>
    </tr>
    <tr id="maven_install-generate_compat_repositories">
      <td><code>generate_compat_repositories</code></td>
      <td>
        optional. default is <code>False</code>
        <p>
          Additionally generate repository aliases in a .bzl file for all JAR
  artifacts. For example, `@maven//:com_google_guava_guava` can also be referenced as
  `@com_google_guava_guava//jar`.
        </p>
      </td>
    </tr>
    <tr id="maven_install-version_conflict_policy">
      <td><code>version_conflict_policy</code></td>
      <td>
        optional. default is <code>"default"</code>
        <p>
          Policy for user-defined vs. transitive dependency version
  conflicts.  If "pinned", choose the user's version unconditionally.  If "default", follow
  Coursier's default policy.
        </p>
      </td>
    </tr>
    <tr id="maven_install-maven_install_json">
      <td><code>maven_install_json</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          A label to a `maven_install.json` file to use pinned artifacts for generating
  build targets. e.g `//:maven_install.json`.
        </p>
      </td>
    </tr>
    <tr id="maven_install-override_targets">
      <td><code>override_targets</code></td>
      <td>
        optional. default is <code>{}</code>
        <p>
          A mapping of `group:artifact` to Bazel target labels. All occurrences of the
  target label for `group:artifact` will be an alias to the specified label, therefore overriding
  the original generated `jvm_import` or `aar_import` target.
        </p>
      </td>
    </tr>
    <tr id="maven_install-strict_visibility">
      <td><code>strict_visibility</code></td>
      <td>
        optional. default is <code>False</code>
        <p>
          Controls visibility of transitive dependencies. If `True`, transitive dependencies
  are private and invisible to user's rules. If `False`, transitive dependencies are public and
  visible to user's rules.
        </p>
      </td>
    </tr>
  </tbody>
</table>


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


### Parameters

<table class="params-table">
  <colgroup>
    <col class="col-param" />
    <col class="col-description" />
  </colgroup>
  <tbody>
    <tr id="maven.repository-url">
      <td><code>url</code></td>
      <td>
        required.
        <p>
          A string containing the repository url (ex: "https://maven.google.com/").
        </p>
      </td>
    </tr>
    <tr id="maven.repository-user">
      <td><code>user</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          A username for this Maven repository, if it requires authentication (ex: "johndoe").
        </p>
      </td>
    </tr>
    <tr id="maven.repository-password">
      <td><code>password</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          A password for this Maven repository, if it requires authentication (ex: "example-password").
        </p>
      </td>
    </tr>
  </tbody>
</table>


<a name="#maven.artifact"></a>

## maven.artifact

<pre>
maven.artifact(<a href="#maven.artifact-group">group</a>, <a href="#maven.artifact-artifact">artifact</a>, <a href="#maven.artifact-version">version</a>, <a href="#maven.artifact-packaging">packaging</a>, <a href="#maven.artifact-classifier">classifier</a>, <a href="#maven.artifact-override_license_types">override_license_types</a>, <a href="#maven.artifact-exclusions">exclusions</a>, <a href="#maven.artifact-neverlink">neverlink</a>)
</pre>

Generates the data map for a Maven artifact given the available information about its coordinates.

### Parameters

<table class="params-table">
  <colgroup>
    <col class="col-param" />
    <col class="col-description" />
  </colgroup>
  <tbody>
    <tr id="maven.artifact-group">
      <td><code>group</code></td>
      <td>
        required.
        <p>
          The Maven artifact coordinate group name (ex: "com.google.guava").
        </p>
      </td>
    </tr>
    <tr id="maven.artifact-artifact">
      <td><code>artifact</code></td>
      <td>
        required.
        <p>
          The Maven artifact coordinate artifact name (ex: "guava").
        </p>
      </td>
    </tr>
    <tr id="maven.artifact-version">
      <td><code>version</code></td>
      <td>
        required.
        <p>
          The Maven artifact coordinate version name (ex: "27.0-jre").
        </p>
      </td>
    </tr>
    <tr id="maven.artifact-packaging">
      <td><code>packaging</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          The Maven packaging specifier (ex: "jar").
        </p>
      </td>
    </tr>
    <tr id="maven.artifact-classifier">
      <td><code>classifier</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          The Maven artifact classifier (ex: "javadoc").
        </p>
      </td>
    </tr>
    <tr id="maven.artifact-override_license_types">
      <td><code>override_license_types</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          An array of Bazel license type strings to use for this artifact's rules (overrides autodetection) (ex: ["notify"]).
        </p>
      </td>
    </tr>
    <tr id="maven.artifact-exclusions">
      <td><code>exclusions</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          An array of exclusion objects to create exclusion specifiers for this artifact (ex: maven.exclusion("junit", "junit")).
        </p>
      </td>
    </tr>
    <tr id="maven.artifact-neverlink">
      <td><code>neverlink</code></td>
      <td>
        optional. default is <code>None</code>
        <p>
          Determines if this artifact should be part of the runtime classpath.
        </p>
      </td>
    </tr>
  </tbody>
</table>


<a name="#maven.exclusion"></a>

## maven.exclusion

<pre>
maven.exclusion(<a href="#maven.exclusion-group">group</a>, <a href="#maven.exclusion-artifact">artifact</a>)
</pre>

Generates the data map for a Maven artifact exclusion.

### Parameters

<table class="params-table">
  <colgroup>
    <col class="col-param" />
    <col class="col-description" />
  </colgroup>
  <tbody>
    <tr id="maven.exclusion-group">
      <td><code>group</code></td>
      <td>
        required.
        <p>
          The Maven group name of the dependency to exclude, e.g. "com.google.guava".
        </p>
      </td>
    </tr>
    <tr id="maven.exclusion-artifact">
      <td><code>artifact</code></td>
      <td>
        required.
        <p>
          The Maven artifact name of the dependency to exclude, e.g. "guava".
        </p>
      </td>
    </tr>
  </tbody>
</table>


