# Rules and macros API

These symbols are exported from `@rules_jvm_external//:defs.bzl` and
`@rules_jvm_external//:kt_defs.bzl`.

Load the symbols you need at the top of your BUILD file. For example:

```python
load("@rules_jvm_external//:defs.bzl", "java_export", "maven_bom")
```

## DEFAULT_REPOSITORY_NAME

<pre>
load("@rules_jvm_external//:defs.bzl", "DEFAULT_REPOSITORY_NAME")
</pre>

The default generated Maven repository name, `"maven"`.
<!-- Generated with Stardoc: http://skydoc.bazel.build -->



<a id="javadoc"></a>

## javadoc

<pre>
load("@rules_jvm_external//:defs.bzl", "javadoc")

javadoc(<a href="#javadoc-name">name</a>, <a href="#javadoc-deps">deps</a>, <a href="#javadoc-additional_dependencies">additional_dependencies</a>, <a href="#javadoc-doc_deps">doc_deps</a>, <a href="#javadoc-doc_resources">doc_resources</a>, <a href="#javadoc-doc_url">doc_url</a>, <a href="#javadoc-excluded_packages">excluded_packages</a>,
        <a href="#javadoc-excluded_workspaces">excluded_workspaces</a>, <a href="#javadoc-included_packages">included_packages</a>, <a href="#javadoc-javadocopts">javadocopts</a>)
</pre>

Generate a javadoc from all the `deps`

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="javadoc-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="javadoc-deps"></a>deps |  The java libraries to generate javadocs for.<br><br>The source jars of each dep will be used to generate the javadocs. Currently docs for transitive dependencies are not generated.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | required |  |
| <a id="javadoc-additional_dependencies"></a>additional_dependencies |  Mapping of `Label`s to the excluded workspace names. Note that this must match the values passed to the `pom_file` rule so the `pom.xml` correctly lists these dependencies.   | <a href="https://bazel.build/rules/lib/dict">Dictionary: Label -> String</a> | optional |  `{}`  |
| <a id="javadoc-doc_deps"></a>doc_deps |  `javadoc` targets referenced by the current target.<br><br>Use this to automatically add appropriate `-linkoffline` javadoc options to resolve references to packages documented by the given javadoc targets that have `url` specified.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="javadoc-doc_resources"></a>doc_resources |  Resources to include in the javadoc jar.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="javadoc-doc_url"></a>doc_url |  The URL at which this documentation will be hosted.<br><br>This information is only used by javadoc targets depending on this target.   | String | optional |  `""`  |
| <a id="javadoc-excluded_packages"></a>excluded_packages |  A list of packages to exclude from the generated javadoc. Wildcards are supported at the end of the package name. For example, `com.example.*` will exclude all the subpackages of `com.example`, while `com.example` will exclude only the files directly in `com.example`.   | List of strings | optional |  `[]`  |
| <a id="javadoc-excluded_workspaces"></a>excluded_workspaces |  A list of bazel workspace names to exclude from the generated jar   | List of strings | optional |  `["com_google_protobuf", "protobuf"]`  |
| <a id="javadoc-included_packages"></a>included_packages |  A list of packages to include in the generated javadoc. Wildcards are supported at the end of the package name. For example, `com.example.*` will include all the subpackages of `com.example`, while `com.example` will include only the files directly in `com.example`.   | List of strings | optional |  `[]`  |
| <a id="javadoc-javadocopts"></a>javadocopts |  javadoc options. Note sources and classpath are derived from the deps. Any additional options can be passed here. If nothing is passed, a default list of options is used: ["-notimestamp", "-use", "-quiet", "-Xdoclint:-missing", "-encoding", "UTF8"]   | List of strings | optional |  `["-notimestamp", "-use", "-quiet", "-Xdoclint:-missing", "-encoding", "UTF8"]`  |


<a id="pom_file"></a>

## pom_file

<pre>
load("@rules_jvm_external//:defs.bzl", "pom_file")

pom_file(<a href="#pom_file-name">name</a>, <a href="#pom_file-additional_dependencies">additional_dependencies</a>, <a href="#pom_file-coordinates">coordinates</a>, <a href="#pom_file-exclusions">exclusions</a>, <a href="#pom_file-pom_template">pom_template</a>, <a href="#pom_file-target">target</a>)
</pre>

Generate a pom.xml file that lists first-order maven dependencies.

The following substitutions are performed on the template file:

  {groupId}: Replaced with the maven coordinates group ID.
  {artifactId}: Replaced with the maven coordinates artifact ID.
  {version}: Replaced by the maven coordinates version.
  {type}: Replaced by the maven coordinates type, if present (defaults to "jar")
  {scope}: Replaced by the maven coordinates type, if present (defaults to "compile")
  {dependencies}: Replaced by a list of maven dependencies directly relied upon
    by java_library targets within the artifact. Dependencies have exclusions
    for any transitive dependencies that are occur in deploy_env.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="pom_file-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="pom_file-additional_dependencies"></a>additional_dependencies |  Mapping of `Label`s to the excluded workspace names   | <a href="https://bazel.build/rules/lib/dict">Dictionary: Label -> String</a> | optional |  `{}`  |
| <a id="pom_file-coordinates"></a>coordinates |  Manual maven coordinates to use   | String | optional |  `""`  |
| <a id="pom_file-exclusions"></a>exclusions |  Mapping of dependency labels to a list of exclusions (encoded as a json string). Each exclusion is a dict with a group and an artifact.   | <a href="https://bazel.build/rules/lib/dict">Dictionary: Label -> String</a> | optional |  `{}`  |
| <a id="pom_file-pom_template"></a>pom_template |  Template file to use for the pom.xml   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `"@rules_jvm_external//private/templates:pom.tpl"`  |
| <a id="pom_file-target"></a>target |  The rule to base the pom file on. Must be a java_library and have a maven_coordinate tag.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |


<a id="MavenPublishInfo"></a>

## MavenPublishInfo

<pre>
load("@rules_jvm_external//:defs.bzl", "MavenPublishInfo")

MavenPublishInfo(<a href="#MavenPublishInfo-coordinates">coordinates</a>, <a href="#MavenPublishInfo-pom">pom</a>, <a href="#MavenPublishInfo-javadocs">javadocs</a>, <a href="#MavenPublishInfo-artifact">artifact</a>, <a href="#MavenPublishInfo-source_jar">source_jar</a>, <a href="#MavenPublishInfo-classifier_artifacts">classifier_artifacts</a>)
</pre>

Information about the Maven artifacts produced by a publishing target.

**FIELDS**

| Name  | Description |
| :------------- | :------------- |
| <a id="MavenPublishInfo-coordinates"></a>coordinates |  Maven coordinates for the project, which may be None    |
| <a id="MavenPublishInfo-pom"></a>pom |  POM file containing project metadata    |
| <a id="MavenPublishInfo-javadocs"></a>javadocs |  Javadoc jar file for documentation files    |
| <a id="MavenPublishInfo-artifact"></a>artifact |  Primary artifact to be published, typically a jar    |
| <a id="MavenPublishInfo-source_jar"></a>source_jar |  Jar with the source code for review    |
| <a id="MavenPublishInfo-classifier_artifacts"></a>classifier_artifacts |  Dict of extra artifacts to be published under classifiers    |


<a id="artifact"></a>

## artifact

<pre>
load("@rules_jvm_external//:defs.bzl", "artifact")

artifact(<a href="#artifact-a">a</a>, <a href="#artifact-repository_name">repository_name</a>)
</pre>

Returns the versionless Bazel label for a Maven artifact.

**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="artifact-a"></a>a |  Maven coordinates as a string or artifact specification dictionary.   |  none |
| <a id="artifact-repository_name"></a>repository_name |  Name of the generated Maven repository.   |  `"maven"` |

**RETURNS**

The artifact's versionless Bazel label.


<a id="create_jar"></a>

## create_jar

<pre>
load("@rules_jvm_external//:defs.bzl", "create_jar")

create_jar(<a href="#create_jar-name">name</a>, <a href="#create_jar-inputs">inputs</a>, <a href="#create_jar-out">out</a>)
</pre>

Creates a JAR containing the given input files.

**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="create_jar-name"></a>name |  Name of the generated target.   |  none |
| <a id="create_jar-inputs"></a>inputs |  Files to include in the JAR.   |  none |
| <a id="create_jar-out"></a>out |  Output JAR name. Defaults to `<name>.jar`.   |  `None` |


<a id="java_export"></a>

## java_export

<pre>
load("@rules_jvm_external//:defs.bzl", "java_export")

java_export(<a href="#java_export-name">name</a>, <a href="#java_export-maven_coordinates">maven_coordinates</a>, <a href="#java_export-manifest_entries">manifest_entries</a>, <a href="#java_export-deploy_env">deploy_env</a>, <a href="#java_export-excluded_workspaces">excluded_workspaces</a>, <a href="#java_export-exclusions">exclusions</a>,
            <a href="#java_export-pom_template">pom_template</a>, <a href="#java_export-allowed_duplicate_names">allowed_duplicate_names</a>, <a href="#java_export-visibility">visibility</a>, <a href="#java_export-tags">tags</a>, <a href="#java_export-testonly">testonly</a>, <a href="#java_export-classifier_artifacts">classifier_artifacts</a>,
            <a href="#java_export-publish_maven_metadata">publish_maven_metadata</a>, <a href="#java_export-kwargs">kwargs</a>)
</pre>

Extends `java_library` to allow maven artifacts to be uploaded.

This macro can be used as a drop-in replacement for `java_library`, but
also generates an implicit `name.publish` target that can be run to publish
maven artifacts derived from this macro to a maven repository. The publish
rule understands the following variables (declared using `--define` when
using `bazel run`, or as environment variables in ALL_CAPS form):

  * `maven_repo`: A URL for the repo to use. May be "https" or "file". Can also be set with environment variable `MAVEN_REPO`.
  * `maven_user`: The user name to use when uploading to the maven repository. Can also be set with environment variable `MAVEN_USER`.
  * `maven_password`: The password to use when uploading to the maven repository. Can also be set with environment variable `MAVEN_PASSWORD`.


This macro also generates a `name-pom` target that creates the `pom.xml` file
associated with the artifacts. The template used is derived from the (optional)
`pom_template` argument, and the following substitutions are performed on
the template file:

  * `{groupId}`: Replaced with the maven coordinates group ID.
  * `{artifactId}`: Replaced with the maven coordinates artifact ID.
  * `{version}`: Replaced by the maven coordinates version.
  * `{type}`: Replaced by the maven coordinates type, if present (defaults to "jar")
  * `{scope}`: Replaced by the maven coordinates type, if present (defaults to "compile")
  * `{dependencies}`: Replaced by a list of maven dependencies directly relied upon
    by java_library targets within the artifact.

The "edges" of the artifact are found by scanning targets that contribute to
runtime dependencies for the following tags:

  * `maven_coordinates=group:artifact:type:version`: Specifies a dependency of
    this artifact.
  * `maven:compile-only`: Specifies that this dependency should not be listed
    as a dependency of the artifact being generated.

To skip generation of the javadoc jar, add the `no-javadocs` tag to the target.

Generated rules:
  * `name`: A `java_library` that other rules can depend upon.
  * `name-docs`: A javadoc jar file.
  * `name-pom`: The pom.xml file.
  * `name.publish`: To be executed by `bazel run` to publish to a maven repo.


**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="java_export-name"></a>name |  A unique name for this target   |  none |
| <a id="java_export-maven_coordinates"></a>maven_coordinates |  The maven coordinates for this target.   |  none |
| <a id="java_export-manifest_entries"></a>manifest_entries |  A dict of `String: String` containing additional manifest entry attributes and values.   |  `{}` |
| <a id="java_export-deploy_env"></a>deploy_env |  A list of labels of Java targets to exclude from the generated jar. [`java_binary`](https://bazel.build/reference/be/java#java_binary) targets are *not* supported.   |  `[]` |
| <a id="java_export-excluded_workspaces"></a>excluded_workspaces |  A dict of strings representing the workspace names of artifacts that should not be included in the maven jar to a `Label` pointing to the dependency that workspace should be replaced by, or `None` if the exclusion shouldn't be replaced with an extra dependency.   |  `{"com_google_protobuf": None, "protobuf": None}` |
| <a id="java_export-exclusions"></a>exclusions |  Mapping of target labels to a list of exclusions to be added to the POM file. Each label must correspond to a direct maven dependency of this target. Each exclusion is represented as a `group:artifact` string.   |  `{}` |
| <a id="java_export-pom_template"></a>pom_template |  The template to be used for the pom.xml file.   |  `None` |
| <a id="java_export-allowed_duplicate_names"></a>allowed_duplicate_names |  A list of `String` containing patterns for files that can be included more than once in the jar file. Examples include `["log4j.properties"]`   |  `None` |
| <a id="java_export-visibility"></a>visibility |  The visibility of the generated targets.   |  `None` |
| <a id="java_export-tags"></a>tags |  Tags applied to the generated targets.   |  `[]` |
| <a id="java_export-testonly"></a>testonly |  Whether the generated targets should be marked test-only.   |  `None` |
| <a id="java_export-classifier_artifacts"></a>classifier_artifacts |  A dict of classifier -> artifact of additional artifacts to publish to Maven.   |  `{}` |
| <a id="java_export-publish_maven_metadata"></a>publish_maven_metadata |  Whether to publish a maven-metadata.xml to remote repository. Some repositories (like AWS CodeArtifact) require the client to publish this file. It is disabled by default.   |  `False` |
| <a id="java_export-kwargs"></a>kwargs |  <p align="center"> - </p>   |  none |


<a id="java_plugin_artifact"></a>

## java_plugin_artifact

<pre>
load("@rules_jvm_external//:defs.bzl", "java_plugin_artifact")

java_plugin_artifact(<a href="#java_plugin_artifact-maven_coords">maven_coords</a>, <a href="#java_plugin_artifact-plugin_class_name">plugin_class_name</a>, <a href="#java_plugin_artifact-repository_name">repository_name</a>)
</pre>

Returns the generated `java_plugin` label for an annotation processor.

**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="java_plugin_artifact-maven_coords"></a>maven_coords |  Maven coordinates of the artifact containing the processor.   |  none |
| <a id="java_plugin_artifact-plugin_class_name"></a>plugin_class_name |  Fully qualified processor class name.   |  none |
| <a id="java_plugin_artifact-repository_name"></a>repository_name |  Name of the generated Maven repository.   |  `"maven"` |

**RETURNS**

The generated `java_plugin` label.


<a id="maven_artifact"></a>

## maven_artifact

<pre>
load("@rules_jvm_external//:defs.bzl", "maven_artifact")

maven_artifact(<a href="#maven_artifact-a">a</a>)
</pre>

Returns the versionless Bazel label for an artifact in the default Maven repository.

**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="maven_artifact-a"></a>a |  Maven coordinates as a string or artifact specification dictionary.   |  none |

**RETURNS**

The artifact's versionless Bazel label.


<a id="maven_bom"></a>

## maven_bom

<pre>
load("@rules_jvm_external//:defs.bzl", "maven_bom")

maven_bom(<a href="#maven_bom-name">name</a>, <a href="#maven_bom-maven_coordinates">maven_coordinates</a>, <a href="#maven_bom-java_exports">java_exports</a>, <a href="#maven_bom-bom_pom_template">bom_pom_template</a>, <a href="#maven_bom-dependencies_maven_coordinates">dependencies_maven_coordinates</a>,
          <a href="#maven_bom-dependencies_pom_template">dependencies_pom_template</a>, <a href="#maven_bom-tags">tags</a>, <a href="#maven_bom-testonly">testonly</a>, <a href="#maven_bom-visibility">visibility</a>, <a href="#maven_bom-toolchains">toolchains</a>)
</pre>

Generates a Maven BOM `pom.xml` file and an optional "dependencies" `pom.xml`.

The generated BOM will contain a list of all the coordinates of the
`java_export` targets in the `java_exports` parameters. An optional
dependencies artifact will be created if the parameter
`dependencies_maven_coordinates` is set.

Both the BOM and dependencies artifact can be templated to support
customization, but a sensible default template will be used if none is
provided. The template used is derived from the (optional)
`pom_template` argument, and the following substitutions are performed on
the template file:

  * `{groupId}`: Replaced with the maven coordinates group ID.
  * `{artifactId}`: Replaced with the maven coordinates artifact ID.
  * `{version}`: Replaced by the maven coordinates version.
  * `{dependencies}`: Replaced by a list of maven dependencies directly relied upon
    by java_library targets within the artifact.

To publish, call the implicit `*.publish` target(s).

The maven repository may be accessed locally using a `file://` URL, or
remotely using an `https://` URL. The following flags may be set
using `--define`:

  * `gpg_sign`: Whether to sign artifacts using GPG
  * `maven_repo`: A URL for the repo to use. May be "https" or "file".
  * `maven_user`: The user name to use when uploading to the maven repository.
  * `maven_password`: The password to use when uploading to the maven repository.

When signing with GPG, the current default key is used.

Generated rules:
  * `name`: The BOM file itself.
  * `name.publish`: To be executed by `bazel run` to publish the BOM to a maven repo
  * `name-dependencies`: The BOM file for the dependencies `pom.xml`. Only generated if `dependencies_maven_coordinates` is set.
  * `name-dependencies.publish`: To be executed by `bazel run` to publish the dependencies `pom.xml` to a maven repo. Only generated if `dependencies_maven_coordinates` is set.


**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="maven_bom-name"></a>name |  A unique name for this rule.   |  none |
| <a id="maven_bom-maven_coordinates"></a>maven_coordinates |  The maven coordinates of this BOM in `groupId:artifactId:version` form.   |  none |
| <a id="maven_bom-java_exports"></a>java_exports |  A list of `java_export` targets that are used to generate the BOM.   |  none |
| <a id="maven_bom-bom_pom_template"></a>bom_pom_template |  A template used for generating the `pom.xml` of the BOM at `maven_coordinates` (optional)   |  `None` |
| <a id="maven_bom-dependencies_maven_coordinates"></a>dependencies_maven_coordinates |  The maven coordinates of a dependencies artifact to generate in GAV format. If empty, none will be generated. (optional)   |  `None` |
| <a id="maven_bom-dependencies_pom_template"></a>dependencies_pom_template |  A template used for generating the `pom.xml` of the dependencies artifact at `dependencies_maven_coordinates` (optional)   |  `None` |
| <a id="maven_bom-tags"></a>tags |  Tags applied to the generated targets.   |  `None` |
| <a id="maven_bom-testonly"></a>testonly |  Whether the generated targets should be marked test-only.   |  `None` |
| <a id="maven_bom-visibility"></a>visibility |  The visibility of the generated targets.   |  `None` |
| <a id="maven_bom-toolchains"></a>toolchains |  Toolchains used by the generated targets.   |  `[]` |


<a id="maven_export"></a>

## maven_export

<pre>
load("@rules_jvm_external//:defs.bzl", "maven_export")

maven_export(<a href="#maven_export-name">name</a>, <a href="#maven_export-maven_coordinates">maven_coordinates</a>, <a href="#maven_export-lib_name">lib_name</a>, <a href="#maven_export-target">target</a>, <a href="#maven_export-manifest_entries">manifest_entries</a>, <a href="#maven_export-deploy_env">deploy_env</a>,
             <a href="#maven_export-excluded_workspaces">excluded_workspaces</a>, <a href="#maven_export-exclusions">exclusions</a>, <a href="#maven_export-pom_template">pom_template</a>, <a href="#maven_export-allowed_duplicate_names">allowed_duplicate_names</a>, <a href="#maven_export-visibility">visibility</a>, <a href="#maven_export-tags">tags</a>,
             <a href="#maven_export-testonly">testonly</a>, <a href="#maven_export-javadocopts">javadocopts</a>, <a href="#maven_export-classifier_artifacts">classifier_artifacts</a>, <a href="#maven_export-doc_deps">doc_deps</a>, <a href="#maven_export-doc_url">doc_url</a>, <a href="#maven_export-doc_resources">doc_resources</a>,
             <a href="#maven_export-doc_excluded_packages">doc_excluded_packages</a>, <a href="#maven_export-doc_included_packages">doc_included_packages</a>, <a href="#maven_export-publish_maven_metadata">publish_maven_metadata</a>, <a href="#maven_export-toolchains">toolchains</a>)
</pre>

Generates Maven publishing targets for an existing library or archive.

This macro is used by `java_export` and `kt_jvm_export` to generate implicit `name.publish`
targets to publish maven artifacts derived from this macro to a maven repository.

The publish rule understands the following variables (declared using `--define` when
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
  * `{type}`: Replaced by the maven coordinates type, if present (defaults to "jar")
  * `{scope}`: Replaced by the maven coordinates type, if present (defaults to "compile")
  * `{dependencies}`: Replaced by a list of maven dependencies directly relied upon
    by java_library targets within the artifact.

The "edges" of the artifact are found by scanning targets that contribute to
runtime dependencies for the following tags:

  * `maven_coordinates=group:artifact:type:version`: Specifies a dependency of
    this artifact.
  * `maven:compile-only`: Specifies that this dependency should not be listed
    as a dependency of the artifact being generated.

Generated rules:
  * `name-docs`: A javadoc jar file.
  * `name-pom`: The pom.xml file.
  * `name.publish`: To be executed by `bazel run` to publish to a maven repo.


**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="maven_export-name"></a>name |  A unique name for this target.   |  none |
| <a id="maven_export-maven_coordinates"></a>maven_coordinates |  The maven coordinates for this target.   |  none |
| <a id="maven_export-lib_name"></a>lib_name |  Name of the library being exported.   |  `None` |
| <a id="maven_export-target"></a>target |  Name of the archive being exported when no library target is used.   |  `None` |
| <a id="maven_export-manifest_entries"></a>manifest_entries |  A dict of `String: String` containing additional manifest entry attributes and values.   |  `{}` |
| <a id="maven_export-deploy_env"></a>deploy_env |  A list of labels of Java targets to exclude from the generated jar. [`java_binary`](https://bazel.build/reference/be/java#java_binary) targets are *not* supported.   |  `[]` |
| <a id="maven_export-excluded_workspaces"></a>excluded_workspaces |  A dict of strings representing the workspace names of artifacts that should not be included in the maven jar to a `Label` pointing to the dependency that workspace should be replaced by, or `None` if the exclusion shouldn't be replaced with an extra dependency.   |  `{}` |
| <a id="maven_export-exclusions"></a>exclusions |  Mapping of target labels to a list of exclusions to be added to the POM file. Each label must correspond to a direct maven dependency of this target. Each exclusion is represented as a `group:artifact` string.   |  `{}` |
| <a id="maven_export-pom_template"></a>pom_template |  The template to be used for the pom.xml file.   |  `None` |
| <a id="maven_export-allowed_duplicate_names"></a>allowed_duplicate_names |  Patterns for file names that may occur more than once in the generated JAR.   |  `None` |
| <a id="maven_export-visibility"></a>visibility |  The visibility of the generated targets.   |  `None` |
| <a id="maven_export-tags"></a>tags |  Tags applied to the generated targets.   |  `[]` |
| <a id="maven_export-testonly"></a>testonly |  Whether the generated targets should be marked test-only.   |  `False` |
| <a id="maven_export-javadocopts"></a>javadocopts |  Options passed to Javadoc.   |  `None` |
| <a id="maven_export-classifier_artifacts"></a>classifier_artifacts |  A mapping of Maven classifiers to additional artifacts to publish.   |  `{}` |
| <a id="maven_export-doc_deps"></a>doc_deps |  Other `javadoc` targets that are referenced by the generated `javadoc` target (if not using `tags = ["no-javadoc"]`)   |  `[]` |
| <a id="maven_export-doc_url"></a>doc_url |  The URL at which the generated `javadoc` will be hosted (if not using `tags = ["no-javadoc"]`).   |  `""` |
| <a id="maven_export-doc_resources"></a>doc_resources |  Resources to be included in the javadoc jar.   |  `[]` |
| <a id="maven_export-doc_excluded_packages"></a>doc_excluded_packages |  A list of packages to exclude from the generated javadoc. Wildcards are supported at the end of the package name. For example, `com.example.*` will exclude all the subpackages of `com.example`, while `com.example` will exclude only the files directly in `com.example`   |  `[]` |
| <a id="maven_export-doc_included_packages"></a>doc_included_packages |  A list of packages to include in the generated javadoc. Wildcards are supported at the end of the package name. For example, `com.example.*` will include all the subpackages of `com.example`, while `com.example` will include only the files directly in `com.example`   |  `[]` |
| <a id="maven_export-publish_maven_metadata"></a>publish_maven_metadata |  Whether to publish a maven-metadata.xml to remote repository. Some repositories (like AWS CodeArtifact) require the client to publish this file. It is disabled by default.   |  `False` |
| <a id="maven_export-toolchains"></a>toolchains |  Toolchains used by the generated Javadoc target.   |  `None` |


<a id="read_coordinates"></a>

## read_coordinates

<pre>
load("@rules_jvm_external//:defs.bzl", "read_coordinates")

read_coordinates(<a href="#read_coordinates-tags">tags</a>)
</pre>

Reads Maven coordinates from a target's tags.

**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="read_coordinates-tags"></a>tags |  Target tags to inspect.   |  none |

**RETURNS**

The Maven coordinates, or `None` if the target has none or suppresses Maven metadata.


<!-- Generated with Stardoc: http://skydoc.bazel.build -->



<a id="kt_jvm_export"></a>

## kt_jvm_export

<pre>
load("@rules_jvm_external//:kt_defs.bzl", "kt_jvm_export")

kt_jvm_export(<a href="#kt_jvm_export-name">name</a>, <a href="#kt_jvm_export-maven_coordinates">maven_coordinates</a>, <a href="#kt_jvm_export-deploy_env">deploy_env</a>, <a href="#kt_jvm_export-excluded_workspaces">excluded_workspaces</a>, <a href="#kt_jvm_export-pom_template">pom_template</a>, <a href="#kt_jvm_export-visibility">visibility</a>,
              <a href="#kt_jvm_export-tags">tags</a>, <a href="#kt_jvm_export-testonly">testonly</a>, <a href="#kt_jvm_export-kwargs">kwargs</a>)
</pre>

Extends `kt_jvm_library` to allow maven artifacts to be uploaded.

This rule is the Kotlin JVM version of `java_export`.

This macro can be used as a drop-in replacement for `kt_jvm_library`, but
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
  * `{type}`: Replaced by the maven coordinates type, if present (defaults to "jar")
  * `{dependencies}`: Replaced by a list of maven dependencies directly relied upon
    by kt_jvm_library targets within the artifact.

The "edges" of the artifact are found by scanning targets that contribute to
runtime dependencies for the following tags:

  * `maven_coordinates=group:artifact:type:version`: Specifies a dependency of
    this artifact.
  * `maven:compile-only`: Specifies that this dependency should not be listed
    as a dependency of the artifact being generated.

To skip generation of the javadoc jar, add the `no-javadocs` tag to the target.

Generated rules:
  * `name`: A `kt_jvm_library` that other rules can depend upon.
  * `name-docs`: A javadoc jar file.
  * `name-pom`: The pom.xml file.
  * `name.publish`: To be executed by `bazel run` to publish to a maven repo.


**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="kt_jvm_export-name"></a>name |  A unique name for this target.   |  none |
| <a id="kt_jvm_export-maven_coordinates"></a>maven_coordinates |  The maven coordinates for this target.   |  none |
| <a id="kt_jvm_export-deploy_env"></a>deploy_env |  A list of labels of Java targets to exclude from the generated JAR.   |  `[]` |
| <a id="kt_jvm_export-excluded_workspaces"></a>excluded_workspaces |  Workspace names whose artifacts should not be included in the Maven JAR.   |  `{"com_google_protobuf": None, "protobuf": None}` |
| <a id="kt_jvm_export-pom_template"></a>pom_template |  The template to use for the pom.xml file.   |  `None` |
| <a id="kt_jvm_export-visibility"></a>visibility |  The visibility of the target.   |  `None` |
| <a id="kt_jvm_export-tags"></a>tags |  Tags applied to the generated targets.   |  `[]` |
| <a id="kt_jvm_export-testonly"></a>testonly |  Whether the generated targets should be marked test-only.   |  `None` |
| <a id="kt_jvm_export-kwargs"></a>kwargs |  <p align="center"> - </p>   |  none |
