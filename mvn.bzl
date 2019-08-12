def _mvn_package_impl(ctx):
    inputs = []
    inputs.extend(ctx.files.srcs)
    inputs.append(ctx.file.pom_xml)

    for input in inputs:
        print(input.short_path)

    output_jar = ctx.actions.declare_file("target/%s-%s.jar" % (ctx.attr.artifact_id, ctx.attr.version))
    target_dir = ctx.actions.declare_directory("target")
    outputs = [output_jar, target_dir]

    # -Djar.finalName=custom-jar-name
    ctx.actions.run_shell(
        inputs = inputs,
        outputs = outputs,
        mnemonic = "MvnPackage",
        progress_message = "Running 'mvn package' for %s" % output_jar.short_path,
        command = "mvn package -DskipTests -DbazelOutputDir=%s -Pbazel" % (output_jar.dirname),
    )

    return [
        DefaultInfo(
            files = depset(outputs),
        ),
        JavaInfo(
            output_jar = output_jar,
            compile_jar = output_jar,
        ),
    ]

mvn_package = rule(
    implementation = _mvn_package_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = True, allow_empty = False),
        "pom_xml": attr.label(allow_single_file = True, mandatory = True),
        "artifact_id": attr.string(mandatory = True),
        "group_id": attr.string(mandatory = True),
        "version": attr.string(mandatory = True),
    },
)