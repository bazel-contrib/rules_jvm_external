_BUILD = """
alias(
    name = "jar",
    actual = "@{generating_repository}//:{target_name}",
    visibility = ["//visibility:public"]
)
"""

def _compat_repository_impl(repository_ctx):
    repository_ctx.file(
        "jar/BUILD",
        _BUILD.format(
            generating_repository = repository_ctx.attr.generating_repository,
            target_name = repository_ctx.name,
        ),
        executable = False,
    )

compat_repository = repository_rule(
    implementation = _compat_repository_impl,
    attrs = {
        "generating_repository": attr.string(default = "maven"),
    }
)
