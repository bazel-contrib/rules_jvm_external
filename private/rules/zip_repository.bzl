load("@bazel_tools//tools/build_defs/repo:utils.bzl", "patch")

def _zip_repository_impl(repository_ctx):
    repository_ctx.extract(
        repository_ctx.attr.path,
        output = ".",
        stripPrefix = repository_ctx.attr.strip_prefix,
    )
    patch(
        repository_ctx,
        patches = repository_ctx.attr.patches,
        patch_args = repository_ctx.attr.patch_args,
    )

zip_repository = repository_rule(
    _zip_repository_impl,
    doc = """Create a repository from a saved zip file generated using the `//:freeze` target.""",
    attrs = {
        "path": attr.label(
            doc = "Path to the zip file to use.",
            mandatory = True,
        ),
        "strip_prefix": attr.string(
            doc = "Prefix to remove from zip.",
        ),
        "patches": attr.label_list(
            doc = "A list of patches to be applied to the unpacked zip contents.",
        ),
        "patch_args": attr.string_list(
            doc = "Arguments to pass to the `patch` command.",
        ),
    },
)
