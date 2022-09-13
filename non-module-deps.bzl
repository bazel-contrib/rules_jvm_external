load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def _non_module_deps_impl(mctx):
    http_archive(
        name = "io_bazel_rules_kotlin",
        sha256 = "946747acdbeae799b085d12b240ec346f775ac65236dfcf18aa0cd7300f6de78",
        urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v1.7.0-RC-2/rules_kotlin_release.tgz"],
    )

non_module_deps = module_extension(
    _non_module_deps_impl,
)
