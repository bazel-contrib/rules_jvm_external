load(":javadoc.bzl", "javadoc")
load(":maven_bom_fragment.bzl", "maven_bom_fragment")
load(":maven_project_jar.bzl", "DEFAULT_EXCLUDED_WORKSPACES", "maven_project_jar")
load(":maven_utils.bzl", "generate_pom")
load(":maven_publish.bzl", "maven_publish")
load(":pom_file.bzl", "pom_file")

def _zip_pom_file_impl(ctx):
    # Expand maven coordinates for any variables to be replaced.
    coordinates = ctx.expand_make_variables("coordinates", ctx.attr.coordinates, ctx.var)

    out = generate_pom(
        ctx,
        coordinates = coordinates,
        pom_template = ctx.file.pom_template,
        out_name = "%s.xml" % ctx.label.name,
    )

    return [
        DefaultInfo(
            files = depset([out]),
            data_runfiles = ctx.runfiles([out]),
        ),
        OutputGroupInfo(
            pom = depset([out]),
        ),
    ]

_zip_pom_file = rule(
    _zip_pom_file_impl,
    doc = """Generate a pom.xml file that lists first-order maven dependencies for zips.

The following substitutions are performed on the template file:

  {groupId}: Replaced with the maven coordinates group ID.
  {artifactId}: Replaced with the maven coordinates artifact ID.
  {version}: Replaced by the maven coordinates version.
  {type}: Replaced by the maven coordinates type, if present (defaults to "jar")
  {scope}: Replaced by the maven coordinates type, if present (defaults to "compile")
""",
    attrs = {
        "pom_template": attr.label(
            doc = "Template file to use for the pom.xml",
            default = "//private/templates:pom.tpl",
            allow_single_file = True,
        ),
        "coordinates": attr.string(
            doc = "The coordinates of the artifact",
            mandatory = True,
        ),
    },
)

def zip_export(
        name,
        maven_coordinates,
        target = None,
        pom_template = None,
        visibility = None,
        tags = [],
        testonly = False,
        classifier_artifacts = {},
        toolchains = None):
    """
    This macro is to publish zip files to a maven repository.

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

    Generated rules:
      * `name-pom`: The pom.xml file.
      * `name.publish`: To be executed by `bazel run` to publish to a maven repo.

    Args:
      name: A unique name for this target
      maven_coordinates: The maven coordinates for this target.
      target: A primary zip file to publish.
      pom_template: The template to be used for the pom.xml file.
      classifier_artifacts: A dict of classifier -> artifact of additional artifacts to publish to Maven.
      visibility: The visibility of the target
    """

    # Sometimes users pass `None` as the value for attributes. Guard against this
    tags = tags if tags else []
    classifier_artifacts = classifier_artifacts if classifier_artifacts else {}

    classifier_artifacts = dict(classifier_artifacts)  # unfreeze

    _zip_pom_file(
        name = "%s-pom" % name,
        pom_template = pom_template,
        coordinates = maven_coordinates,
        visibility = visibility,
        tags = tags,
        testonly = testonly,
    )

    maven_publish(
        name = "%s.publish" % name,
        coordinates = maven_coordinates,
        pom = "%s-pom" % name,
        artifact = target,
        classifier_artifacts = {v: k for (k, v) in classifier_artifacts.items() if v},
        visibility = visibility,
        tags = tags,
        testonly = testonly,
        toolchains = toolchains,
    )

    # We may want to aggregate several `java_export` targets into a single Maven BOM POM
    # https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms
    if target:
        maven_bom_fragment(
            name = "%s.bom-fragment" % name,
            maven_coordinates = maven_coordinates,
            artifact = target,
            src_artifact = ":%s-maven-source" % name,
            javadoc_artifact = None if "no-javadocs" in tags else ":%s-docs" % name,
            pom = ":%s-pom" % name,
            testonly = testonly,
            tags = tags,
            visibility = visibility,
            toolchains = toolchains,
        )

    # Finally, alias the primary output
    native.alias(
        name = name,
        actual = ":%s-project" % name,
        visibility = visibility,
        tags = tags,
        testonly = testonly,
    )
