load("@bazel_skylib//lib:paths.bzl", "paths")

def is_windows(ctx):
    return ctx.configuration.host_path_separator == ";"

def file_to_rlocationpath(ctx, file):
    return paths.normalize(ctx.workspace_name + "/" + file.short_path)
