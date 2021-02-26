def generate_javadoc(ctx, javadoc, source_jars, classpath, output):
    args = ctx.actions.args()
    args.add_all(["--out", output])
    args.add_all(source_jars, before_each = "--in")
    args.add_all(classpath.to_list(), before_each = "--cp")

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

    classpath = depset(transitive = [dep[JavaInfo].transitive_runtime_jars for dep in ctx.attr.deps])

    generate_javadoc(ctx, ctx.executable._javadoc, sources, classpath, jar_file)

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
        "_javadoc": attr.label(
            default = "//private/tools/java/rules/jvm/external/javadoc",
            cfg = "host",
            executable = True,
        ),
    },
)
