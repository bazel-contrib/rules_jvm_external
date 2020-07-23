MavenPublishInfo = provider (
    fields = {
        "coordinates": "Maven coordinates for the project, which may be None",
        "pom": "Pom.xml file for metdata",
        "javadocs": "Javadoc jar file for documentation files",
        "artifact_jar": "Jar with the code and metadata for execution",
        "source_jar": "Jar with the source code for review",
    }
)

_TEMPLATE = """#!/usr/bin/env bash

echo "Uploading {coordinates} to {maven_repo}"
./uploader {maven_repo} {gpg_sign} {user} {password} {coordinates} pom.xml artifact.jar source.jar doc.jar
"""

def _maven_publish_impl(ctx):
    executable = ctx.actions.declare_file("%s-publisher" % ctx.attr.name)

    maven_repo = ctx.var.get("maven_repo", "''")
    gpg_sign = ctx.var.get("gpg_sign", "'false'")
    user = ctx.var.get("maven_user", "''")
    password = ctx.var.get("maven_password", "''")

    ctx.actions.write(
        output = executable,
        is_executable = True,
        content = _TEMPLATE.format(
            coordinates = ctx.attr.coordinates,
            gpg_sign = gpg_sign,
            maven_repo = maven_repo,
            password = password,
            user = user,
        ),
    )

    return [
        DefaultInfo(
            files = depset([executable]),
            executable = executable,
            runfiles = ctx.runfiles(
                symlinks = {
                    "artifact.jar": ctx.file.artifact_jar,
                    "doc.jar": ctx.file.javadocs,
                    "pom.xml": ctx.file.pom,
                    "source.jar": ctx.file.source_jar,
                    "uploader": ctx.executable._uploader,
                },
                collect_data = True,
            ).merge(ctx.attr._uploader[DefaultInfo].data_runfiles),
        ),
        MavenPublishInfo(
            coordinates = ctx.attr.coordinates,
            artifact_jar = ctx.file.artifact_jar,
            javadocs = ctx.file.javadocs,
            source_jar = ctx.file.source_jar,
            pom = ctx.file.pom
        )
    ]

maven_publish = rule(
    _maven_publish_impl,
    doc = """Publish artifacts to a maven repository.

The maven repository may accessed locally using a `file://` URL, or
remotely using an `https://` URL. The following flags may be set
using `--define`:

  gpg_sign: Whether to sign artifacts using GPG
  maven_repo: A URL for the repo to use. May be "https" or "file".
  maven_user: The user name to use when uploading to the maven repository.
  maven_password: The password to use when uploading to the maven repository.

When signing with GPG, the current default key is used.
""",
    executable = True,
    attrs = {
        "coordinates": attr.string(
            mandatory = True,
        ),
        "pom": attr.label(
            mandatory = True,
            allow_single_file = True,
        ),
        "javadocs": attr.label(
            mandatory = True,
            allow_single_file = True,
        ),
        "artifact_jar": attr.label(
            mandatory = True,
            allow_single_file = True,
        ),
        "source_jar": attr.label(
            mandatory = True,
            allow_single_file = True,
        ),
        "_uploader": attr.label(
            executable = True,
            cfg = "host",
            default = "//private/tools/java/rules/jvm/external/maven:MavenPublisher",
            allow_files = True,
        ),
    }
)
