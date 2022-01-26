load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//private/rules:java_export.bzl", "maven_export")

def kt_jvm_export(
        name,
        maven_coordinates,
        deploy_env = [],
        pom_template = None,
        visibility = None,
        tags = [],
        **kwargs):
    """Extends `kt_jvm_library` to allow maven artifacts to be uploaded. This
    rule is the Kotlin JVM version of `java_export`.

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
      * `{type}`: Replaced by the maven coordintes type, if present (defaults to "jar")
      * `{dependencies}`: Replaced by a list of maven dependencies directly relied upon
        by kt_jvm_library targets within the artifact.

    The "edges" of the artifact are found by scanning targets that contribute to
    runtime dependencies for the following tags:

      * `maven_coordinates=group:artifact:type:version`: Specifies a dependency of
        this artifact.
      * `maven:compile_only`: Specifies that this dependency should not be listed
        as a dependency of the artifact being generated.

    To skip generation of the javadoc jar, add the `no-javadocs` tag to the target.

    Generated rules:
      * `name`: A `kt_jvm_library` that other rules can depend upon.
      * `name-docs`: A javadoc jar file.
      * `name-pom`: The pom.xml file.
      * `name.publish`: To be executed by `bazel run` to publish to a maven repo.

    Args:
      name: A unique name for this target
      maven_coordinates: The maven coordinates for this target.
      pom_template: The template to be used for the pom.xml file.
      deploy_env: A list of labels of java targets to exclude from the generated jar
      visibility: The visibility of the target
      kwargs: These are passed to [`kt_jvm_library`](https://bazelbuild.github.io/rules_kotlin/kotlin),
        and so may contain any valid parameter for that rule.
    """

    maven_coordinates_tags = ["maven_coordinates=%s" % maven_coordinates]
    lib_name = "%s-lib" % name

    javadocopts = kwargs.pop("javadocopts", [])

    # Construct the kt_jvm_library we'll export from here.
    kt_jvm_library(
        name = lib_name,
        tags = tags + maven_coordinates_tags,
        **kwargs
    )

    maven_export(
        name,
        maven_coordinates,
        maven_coordinates_tags,
        deploy_env,
        pom_template,
        visibility,
        tags,
        lib_name,
        javadocopts
    )
