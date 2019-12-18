_JAR_BUILD = """
alias(
    name = "jar",
    actual = "@{generating_repository}//:{target_name}",
    visibility = ["//visibility:public"]
)
"""

_ROOT_BUILD = """
alias(
    name = "{repository_name}",
    actual = "@{generating_repository}//:{target_name}",
    visibility = ["//visibility:public"]
)
"""

def _compat_repository_impl(repository_ctx):
    repository_ctx.file(
        "jar/BUILD",
        _JAR_BUILD.format(
            generating_repository = repository_ctx.attr.generating_repository,
            target_name = repository_ctx.name,
        ),
        executable = False,
    )

    repository_ctx.file(
        "BUILD",
        _ROOT_BUILD.format(
            repository_name = repository_ctx.name,
            generating_repository = repository_ctx.attr.generating_repository,
            target_name = repository_ctx.name,
        ),
        executable = False,
    )

compat_repository = repository_rule(
    implementation = _compat_repository_impl,
    attrs = {
        "generating_repository": attr.string(default = "maven"),
    },
)
