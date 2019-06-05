_BUILD = """
alias(
    name = "jar",
    actual = "@maven//:%s",
    visibility = ["//visibility:public"]
)
"""

def _compat_repository_impl(repository_ctx):
    repository_ctx.file(
        "jar/BUILD",
        _BUILD % repository_ctx.name,
        executable = False,
    )

compat_repository = repository_rule(
    implementation = _compat_repository_impl
)
