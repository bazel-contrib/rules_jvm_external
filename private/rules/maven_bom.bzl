load(":maven_bom_fragment.bzl", "MavenBomFragmentInfo")
load(":maven_publish.bzl", "MavenPublishInfo")
load(":maven_utils.bzl", "generate_pom", "unpack_coordinates")

def _label(label_or_string):
    if type(label_or_string) == "Label":
        return label_or_string

    if type(label_or_string) == "string":
        # We may have a target of the form: `@bar//foo`, `//foo`, `//foo:foo`, `:foo`, `foo`
        if label_or_string.startswith("@"):
            # We have an absolute target. Great!
            return Label(label_or_string)
        elif label_or_string.startswith("//"):
            return Label("@%s" % label_or_string)
        else:
            if label_or_string.startswith(":"):
                label_or_string = label_or_string[1:]
            return Label("@//%s:%s" % (native.package_name(), label_or_string))

    fail("Can only convert either labels or strings: %s" % label_or_string)

def _maven_bom_impl(ctx):
    fragments = [f[MavenBomFragmentInfo] for f in ctx.attr.fragments]

    # We only want to include an entry in the BOM if it's a dependency of
    # more than one `java_export` we're wrapping.

    # Begin by construct a mapping between a particular dependency and "things that depend upon it"
    dep2export = {}
    for fragment in fragments:
        for dep in fragment.maven_info.maven_deps.to_list():
            existing = dep2export.get(dep, [])
            existing.append(fragment.coordinates)
            dep2export[dep] = existing

    # Next, gather those dependencies that have more than one "things that depend upon it"
    shared_deps = [dep for (dep, list) in dep2export.items() if len(list) > 1]
    coordinates_to_exclude = [f.coordinates for f in fragments]
    shared_deps = [dep for dep in shared_deps if dep not in coordinates_to_exclude]

    # And, finally, let's generate the publishing script
    maven_repo = ctx.var.get("maven_repo", "''")
    gpg_sign = ctx.var.get("gpg_sign", "'false'")
    user = ctx.var.get("maven_user", "''")
    password = ctx.var.get("maven_password", "''")

    upload_script = "#!/usr/bin/env bash\nset -eufo pipefail\n\n"

    bom = generate_pom(
        ctx,
        coordinates = ctx.attr.maven_coordinates,
        versioned_dep_coordinates = shared_deps,
        pom_template = ctx.file.pom_template,
        out_name = "%s.xml" % ctx.label.name,
        indent = 12,
    )

    files = [bom]
    upload_script += """echo "Uploading {coordinates} to {maven_repo}"
{uploader} {maven_repo} {gpg_sign} {user} {password} {coordinates} {pom} '' '' ''
""".format(
        uploader = ctx.executable._uploader.short_path,
        coordinates = ctx.attr.maven_coordinates,
        gpg_sign = gpg_sign,
        maven_repo = maven_repo,
        password = password,
        user = user,
        pom = bom.short_path,
    )

    # Now generate a `pom.xml` for each `java_export` we've been given
    poms = {}
    for fragment in fragments:
        info = fragment.maven_info
        versioned = []
        unversioned = []
        for dep in fragment.maven_info.maven_deps.to_list():
            if dep in shared_deps:
                unversioned.append(dep)
            else:
                versioned.append(dep)

        unpacked = unpack_coordinates(fragment.coordinates)

        pom = generate_pom(
            ctx,
            coordinates = fragment.coordinates,
            parent = ctx.attr.maven_coordinates,
            versioned_dep_coordinates = versioned,
            unversioned_dep_coordinates = unversioned,
            pom_template = fragment.pom_template,
            out_name = "%s-%s-pom.xml" % (unpacked.groupId, unpacked.artifactId),
        )
        poms.update({"%s-pom" % fragment.coordinates: pom})

        javadocs_short_path = fragment.javadocs.short_path if fragment.javadocs else "''"

        upload_script += """echo "Uploading {coordinates} to {maven_repo}"
{uploader} {maven_repo} {gpg_sign} {user} {password} {coordinates} {pom} {artifact_jar} {source_jar} {javadoc}
""".format(
            uploader = ctx.executable._uploader.short_path,
            coordinates = info.coordinates,
            gpg_sign = gpg_sign,
            maven_repo = maven_repo,
            password = password,
            user = user,
            pom = pom.short_path,
            artifact_jar = fragment.artifact.short_path,
            source_jar = fragment.srcs.short_path,
            javadoc = javadocs_short_path,
        )
        files.extend([pom, fragment.artifact, fragment.srcs])
        if fragment.javadocs:
            files.append(fragment.javadocs)

    executable = ctx.actions.declare_file("%s-upload.sh" % ctx.label.name)
    ctx.actions.write(
        output = executable,
        is_executable = True,
        content = upload_script,
    )

    pom2outputgroup = {coord: depset([pom]) for (coord, pom) in poms.items()}

    return [
        DefaultInfo(
            files = depset([bom] + poms.values() + [executable]),
            executable = executable,
            runfiles = ctx.runfiles(
                files = files,
                collect_data = True,
            ).merge(ctx.attr._uploader[DefaultInfo].data_runfiles),
        ),
        MavenPublishInfo(
            coordinates = ctx.attr.maven_coordinates,
            artifact_jar = None,
            javadocs = None,
            source_jar = None,
            pom = bom,
        ),
        OutputGroupInfo(
            bom = [bom],
            **pom2outputgroup
        ),
    ]

_maven_bom = rule(
    _maven_bom_impl,
    doc = """Create a Maven BOM file (`pom.xml`) for the given targets.""",
    executable = True,
    attrs = {
        "maven_coordinates": attr.string(
            mandatory = True,
        ),
        "pom_template": attr.label(
            doc = "Template file to use for the BOM pom.xml",
            default = "//private/templates:bom.tpl",
            allow_single_file = True,
        ),
        "_uploader": attr.label(
            executable = True,
            cfg = "host",
            default = "//private/tools/java/rules/jvm/external/maven:MavenPublisher",
            allow_files = True,
        ),
        "fragments": attr.label_list(
            providers = [
                [MavenBomFragmentInfo],
            ],
        ),
    },
)

def maven_bom(name, maven_coordinates, java_exports, tags = None, testonly = None, visibility = None):
    """Generates a Maven BOM `pom.xml` file.

    The generated BOM will contain maven dependencies that are shared between two
    or more of the `java_exports`. This will also generate `pom.xml` files for
    each of the `java_exports`. Within those `pom.xml`s, only dependencies that are
    unique to the `java_export` will have the `version` tag. Dependencies which are
    listed in the BOM will omit the `version` tag.

    The maven repository may accessed locally using a `file://` URL, or
    remotely using an `https://` URL. The following flags may be set
    using `--define`:

      gpg_sign: Whether to sign artifacts using GPG
      maven_repo: A URL for the repo to use. May be "https" or "file".
      maven_user: The user name to use when uploading to the maven repository.
      maven_password: The password to use when uploading to the maven repository.

    When signing with GPG, the current default key is used.

        Args:
          name: A unique name for this rule.
          maven_coordinates: The maven coordinates of this BOM in `groupId:artifactId:version` form.
          java_exports: A list of `java_export` targets that are used to generate the BOM.
    """
    fragments = []
    labels = [_label(je) for je in java_exports]
    fragments = ["%s.bom-fragment" % str(l) for l in labels]

    _maven_bom(
        name = name,
        maven_coordinates = maven_coordinates,
        fragments = fragments,
        tags = tags,
        visibility = visibility,
    )
