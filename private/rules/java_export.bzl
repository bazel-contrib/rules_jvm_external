load("//private/rules:javadoc.bzl", "javadoc")
load("//private/rules:maven_project_jar.bzl", "maven_project_jar")
load("//private/rules:maven_publish.bzl", "maven_publish")
load("//private/rules:pom_file.bzl", "pom_file")

def java_export(
        name,
        maven_coordinates,
        deploy_env = [],
        pom_template = None,
        visibility = None,
        tags = [],
        **kwargs):
    """Extends `java_library` to allow maven artifacts to be uploaded.

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

    Args:
      name: A unique name for this target
      maven_coordinates: The maven coordinates for this target.
      pom_template: The template to be used for the pom.xml file.
      deploy_env: A list of labels of java targets to exclude from the generated jar
      visibility: The visibility of the target
      kwargs: These are passed to [`java_library`](https://docs.bazel.build/versions/master/be/java.html#java_library),
        and so may contain any valid parameter for that rule.
    """

    tags = tags + ["maven_coordinates=%s" % maven_coordinates]
    lib_name = "%s-lib" % name

    # Construct the java_library we'll export from here.
    native.java_library(
        name = lib_name,
        tags = tags,
        **kwargs
    )

    # Merge the jars to create the maven project jar
    maven_project_jar(
        name = "%s-project" % name,
        target = ":%s" % lib_name,
        deploy_env = deploy_env,
        tags = tags,
    )

    native.filegroup(
        name = "%s-maven-artifact" % name,
        srcs = [
            ":%s-project" % name,
        ],
        output_group = "maven_artifact",
    )

    native.filegroup(
        name = "%s-maven-source" % name,
        srcs = [
            ":%s-project" % name,
        ],
        output_group = "maven_source",
    )

    javadoc(
        name = "%s-docs" % name,
        deps = [
            ":%s-project" % name,
        ],
    )

    pom_file(
        name = "%s-pom" % name,
        target = ":%s" % lib_name,
        pom_template = pom_template,
    )

    maven_publish(
        name = "%s.publish" % name,
        coordinates = maven_coordinates,
        pom = "%s-pom" % name,
        javadocs = "%s-docs" % name,
        artifact_jar = ":%s-maven-artifact" % name,
        source_jar = ":%s-maven-source" % name,
        visibility = visibility,
    )

    # Finally, alias the primary output
    native.alias(
        name = name,
        actual = ":%s-project" % name,
        visibility = visibility,
    )
