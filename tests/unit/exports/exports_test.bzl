"""Unit tests for exports."""

load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("@compatibility_proxy//:proxy.bzl", "JavaInfo", "java_library")
load("//private/rules:has_maven_deps.bzl", "MavenInfo", "has_maven_deps")
load("//private/rules:maven_project_jar.bzl", "maven_project_jar")

def _exports_test_impl(ctx):
    env = analysistest.begin(ctx)
    tut_with_aspect = env.ctx.attr.target_under_test_with_aspect
    asserts.true(env, MavenInfo in tut_with_aspect, "java_library should provide MavenInfo")
    maven_info = tut_with_aspect[MavenInfo]
    asserts.equals(env, [Label("//tests/unit/exports:exported_leaf"), Label("//tests/unit/exports:is_export_has_exports")], maven_info.transitive_exports.to_list())

    tut = env.ctx.attr.target_under_test
    asserts.true(env, JavaInfo in tut_with_aspect, "maven_project_jar should provide JavaInfo")
    java_info = tut[JavaInfo]
    asserts.equals(env, ["maven_project_jar.jar", "libexported_leaf.jar", "libis_export_has_exports.jar"], [f.basename for f in java_info.transitive_runtime_jars.to_list()])

    return analysistest.end(env)

exports_test = analysistest.make(
    _exports_test_impl,
    attrs = {
        # We can't just use target_under_test because we need to add our own aspect to the attribute.
        # See https://github.com/bazelbuild/bazel-skylib/pull/299
        "target_under_test_with_aspect": attr.label(aspects = [has_maven_deps], mandatory = True),
    },
)

def _manifest_entries_test_impl(ctx):
    env = analysistest.begin(ctx)
    actions = analysistest.target_actions(env)
    asserts.false(env, "AmendManifestEntry" in [action.mnemonic for action in actions])
    merges = [action for action in actions if action.mnemonic == "MergeJars"]
    asserts.equals(env, 2, len(merges))
    binary_merges = [action for action in merges if any([output.basename == "amended_with_manifest.jar" for output in action.outputs.to_list()])]
    asserts.equals(env, 1, len(binary_merges))
    if binary_merges:
        asserts.true(env, "--manifest-entry" in binary_merges[0].argv)
        asserts.true(env, "Version:1.0" in binary_merges[0].argv)
    for action in merges:
        if action not in binary_merges:
            asserts.false(env, "--manifest-entry" in action.argv)
    return analysistest.end(env)

manifest_entries_test = analysistest.make(_manifest_entries_test_impl)

def exports_tests(name):
    maven_project_jar(
        name = "maven_project_jar",
        target = ":library_to_test",
    )

    maven_project_jar(
        name = "with_manifest",
        target = ":library_to_test",
        manifest_entries = {"Version": "1.0"},
    )

    manifest_entries_test(
        name = "manifest_entries_test",
        target_under_test = ":with_manifest",
    )

    java_library(
        name = "library_to_test",
        deps = [":is_dep_has_exports", ":is_export_has_exports"],
        exports = [":is_export_has_exports"],
        srcs = ["Foo.java"],
    )

    java_library(
        name = "is_dep_has_exports",
        deps = [":leaf"],
        exports = [":leaf"],
        srcs = ["Foo.java"],
    )

    java_library(
        name = "is_export_has_exports",
        deps = [":exported_leaf"],
        exports = [":exported_leaf"],
        srcs = ["Foo.java"],
    )

    java_library(
        name = "leaf",
        srcs = ["Foo.java"],
    )

    java_library(
        name = "exported_leaf",
        srcs = ["Foo.java"],
    )

    exports_test(
        name = "exports_test",
        target_under_test = ":maven_project_jar",
        target_under_test_with_aspect = ":library_to_test",
    )
