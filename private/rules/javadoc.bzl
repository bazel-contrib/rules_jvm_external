load(":maven_project_jar.bzl", "DEFAULT_EXCLUDED_WORKSPACES")
load(":maven_utils.bzl", "determine_additional_dependencies")

def generate_javadoc(ctx, javadoc, source_jars, classpath, javadocopts, output):
    args = ctx.actions.args()
    args.add_all(["--out", output])
    args.add_all(source_jars, before_each = "--in")
    args.add_all(classpath, before_each = "--cp")
    args.add_all(javadocopts)

    ctx.actions.run(
        executable = javadoc,
        outputs = [output],
        inputs = depset(source_jars, transitive = [classpath]),
        arguments = [args],
    )

def _javadoc_impl(ctx):
    sources = []
    for dep in ctx.attr.deps:
        sources.extend(dep[JavaInfo].source_jars)

    jar_file = ctx.actions.declare_file("%s.jar" % ctx.attr.name)

    # Gather additional files to add to the classpath
    additional_deps = depset(transitive = [dep[JavaInfo].transitive_runtime_jars for dep in ctx.attr.deps])
    classpath = depset(transitive = [dep[JavaInfo].transitive_runtime_jars for dep in ctx.attr.deps] + [additional_deps])

    # javadoc options and javac options overlap, but we cannot
    # necessarily rely on those to derive the javadoc options we need
    # from dep[JavaInfo].compilation_info (which, FWIW, always returns
    # `None` https://github.com/bazelbuild/bazel/issues/10170). For this
    # reason we allow people to set javadocopts via the rule attrs.

    generate_javadoc(ctx, ctx.executable._javadoc, sources, classpath, ctx.attr.javadocopts, jar_file)

    return [
        DefaultInfo(files = depset([jar_file])),
    ]

javadoc = rule(
    _javadoc_impl,
    doc = "Generate a javadoc from all the `deps`",
    attrs = {
        "deps": attr.label_list(
            doc = """The java libraries to generate javadocs for.

          The source jars of each dep will be used to generate the javadocs.
          Currently docs for transitive dependencies are not generated.
          """,
            mandatory = True,
            providers = [
                [JavaInfo],
            ],
        ),
        "javadocopts": attr.string_list(
            doc = """javadoc options.
            Note sources and classpath are derived from the deps. Any additional
            options can be passed here.
            """,
        ),
        "excluded_workspaces": attr.string_list(
            doc = "A list of bazel workspace names to exclude from the generated jar",
            allow_empty = True,
            default = DEFAULT_EXCLUDED_WORKSPACES,
        ),
        "additional_dependencies": attr.label_keyed_string_dict(
            doc = "Mapping of `Label`s to the excluded workspace names. Note that this must match the values passed to the `pom_file` rule so the `pom.xml` correctly lists these dependencies.",
            allow_empty = True,
            providers = [
                [JavaInfo],
            ],
        ),
        "_javadoc": attr.label(
            default = "//private/tools/java/com/github/bazelbuild/rules_jvm_external/javadoc:javadoc",
            cfg = "exec",
            executable = True,
        ),
    },
)
