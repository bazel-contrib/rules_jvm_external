load(":maven_version.bzl", "MavenVersionProvider")

MavenDeployInfo = provider(
    fields = {
        "deps": "MavenDeployInfo providers of dependencies",
        "jar": "jar file to deploy",
        "maven_coordinates": "maven coordinates for this jar",
        "srcjar": "jar file with sources",
        "pom": "pom file",
    },
)

def _get_maven_coordinates(tags, target_label):
    maven_coordinates = []
    for tag in tags:
        if tag.startswith("maven_coordinates="):
            maven_coordinates.append(tag[len("maven_coordinates="):])
        if len(maven_coordinates) > 1:
            fail("You should not set more than one maven_coordinates tag per java_library")

    if len(maven_coordinates) == 0:
        fail("The dependency {} is missing a maven_coordinates tag".format(target_label))

    return maven_coordinates[0]

def _maven_pom_aspect_impl(target, ctx):
    if JavaInfo not in target:
        return[MavenDeployInfo(
            deps = [],
            jar = None,
            maven_coordinates = None,
            srcjar = None,
            pom = None,
        )]

    target_maven_coordinates = _get_maven_coordinates(ctx.rule.attr.tags, target.label)

    # No need to generate pom files for external dependencies.
    if target.label.workspace_root.startswith("external/"):
        return [
            MavenDeployInfo(
                deps = [],
                jar = None,
                maven_coordinates = target_maven_coordinates,
                srcjar = None,
                pom = None,
            )
        ]

    deps_maven_coordinates = []
    transitive_maven_deploy_info = []
    for dep in getattr(ctx.rule.attr, "deps", []):
        if dep[MavenDeployInfo].maven_coordinates:
            deps_maven_coordinates.append(dep[MavenDeployInfo].maven_coordinates)
            transitive_maven_deploy_info.append(dep[MavenDeployInfo])

    pom_file = ctx.actions.declare_file("{}_pom.xml".format(ctx.rule.attr.name))
    dependencies_xml = []
    if len(deps_maven_coordinates) > 0:
        dependency_block = """        <dependency>
            <groupId>{group_id}</groupId>
            <artifactId>{artifact_id}</artifactId>
            <version>{version}</version>
        </dependency>"""
        dependencies_xml.append("     <dependencies>")
        for dep_coordinate in deps_maven_coordinates:
            dependencies_xml.append(
                dependency_block.format(
                    group_id = dep_coordinate.split(":")[0],
                    artifact_id = dep_coordinate.split(":")[1],
                    version = ctx.attr._maven_version[MavenVersionProvider].maven_version,
                )
            )
        dependencies_xml.append("     </dependencies>")

    ctx.actions.expand_template(
        template = ctx.file._pom_xml_template,
        output = pom_file,
        substitutions = {
            "{target_group_id}": target_maven_coordinates.split(":")[0],
            "{target_artifact_id}": target_maven_coordinates.split(":")[1],
            "{target_version}": ctx.attr._maven_version[MavenVersionProvider].maven_version,
            "{target_dependencies}": "\n".join(dependencies_xml),
        }
    )

    target_output_jars = target[JavaInfo].outputs.jars
    jar = target_output_jars[0].class_jar

    target_source_jar = None
    for output in target_output_jars:
        if output.source_jar and output.source_jar.basename.endswith('-src.jar'):
            target_source_jar = output.source_jar
            break

    return [
        MavenDeployInfo(
            deps = depset(transitive_maven_deploy_info),
            jar = target[JavaInfo].outputs.jars[0].class_jar,
            maven_coordinates = target_maven_coordinates,
            srcjar = target_source_jar,
            pom = pom_file,
        )
    ]

maven_pom_aspect = aspect(
    attr_aspects = [
        "deps",
    ],
    attrs = {
        "_pom_xml_template": attr.label(
            allow_single_file = True,
            default = "//deploy:pom.xml.template",
        ),
        "_maven_version": attr.label(
            default = Label("//deploy:maven_version"),
        )
    },
    implementation = _maven_pom_aspect_impl,
    provides = [MavenDeployInfo]
)


def _maven_deploy_impl(ctx):
    transitive_maven_deploy_info = []
    for target in ctx.attr.targets:
        transitive_maven_deploy_info.append(target[MavenDeployInfo])
        transitive_maven_deploy_info.extend(target[MavenDeployInfo].deps.to_list())
    transitive_maven_deploy_info = depset(transitive_maven_deploy_info).to_list()

    maven_deploy_runfiles = []
    maven_deploy_content = []
    for maven_deploy_info in transitive_maven_deploy_info:
        if maven_deploy_info.jar:
            maven_deploy_runfiles.append(maven_deploy_info.jar)
            if maven_deploy_info.srcjar:
                maven_deploy_runfiles.append(maven_deploy_info.srcjar)
            maven_deploy_runfiles.append(maven_deploy_info.pom)

            maven_deploy_content.extend([
                "mvn -q org.apache.maven.plugins:maven-deploy-plugin:2.8.2:deploy-file \\",
                "  -Durl=https://maven.global.square/artifactory/jar-releases \\",
                "  -DrepositoryId=jar-releases \\",
                "  -DupdateReleaseInfo=true \\",
                "  -Dfile={} \\".format(maven_deploy_info.jar.path),
                "  -Dsources={} \\".format(maven_deploy_info.srcjar.path) if maven_deploy_info.srcjar else "",
                "  -DpomFile={} \\".format(maven_deploy_info.pom.path),
                "  -Dversion={}".format(ctx.attr._maven_version[MavenVersionProvider].maven_version),
                "",
            ])

    maven_deploy_script = ctx.actions.declare_file("maven-deploy.sh")
    ctx.actions.write(
        output = maven_deploy_script,
        content = "\n".join([
            "#!/bin/bash",
            "",
            "\n".join(maven_deploy_content),
        ]),
        is_executable = True,
    )

    return DefaultInfo(
        runfiles = ctx.runfiles(files = maven_deploy_runfiles),
        executable = maven_deploy_script,
    )

maven_deploy = rule(
    attrs = {
        "targets": attr.label_list(
            mandatory = True,
            allow_empty = False,
            providers = [JavaInfo],
            aspects = [
                maven_pom_aspect,
            ],
        ),
        "_maven_version": attr.label(
            default = Label("//deploy:maven_version"),
        )
    },
    executable = True,
    implementation = _maven_deploy_impl,
)
