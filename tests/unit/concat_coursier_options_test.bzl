"""Tests for merging `additional_coursier_options` across modules.

These options form an ordered argument vector passed verbatim to coursier, so
they must be concatenated (root first) and never deduplicated.
"""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/extensions:maven.bzl", "concat_coursier_options")

def _preserves_repeated_tokens_impl(ctx):
    env = unittest.begin(ctx)

    # coursier's `--variant` flag is repeatable; deduplicating would collapse
    # the repeated `--variant` tokens and corrupt the argument list.
    root_options = [
        "--enable-modules",
        "--variant",
        "org.gradle.category=library",
        "--variant",
        "org.gradle.usage=runtime",
    ]

    merged = concat_coursier_options(root_options, [])

    asserts.equals(env, root_options, merged)

    return unittest.end(env)

preserves_repeated_tokens_test = unittest.make(_preserves_repeated_tokens_impl)

def _concatenates_root_first_impl(ctx):
    env = unittest.begin(ctx)

    root_options = ["--enable-modules"]
    non_root_options = ["--variant", "org.gradle.usage=runtime"]

    merged = concat_coursier_options(root_options, non_root_options)

    # Root options come first, then non-root, with nothing dropped.
    asserts.equals(
        env,
        ["--enable-modules", "--variant", "org.gradle.usage=runtime"],
        merged,
    )

    return unittest.end(env)

concatenates_root_first_test = unittest.make(_concatenates_root_first_impl)

def _keeps_cross_module_duplicates_impl(ctx):
    env = unittest.begin(ctx)

    # The same flag declared by both a root and a non-root module is kept twice:
    # we cannot safely deduplicate an ordered, positional argument vector.
    merged = concat_coursier_options(["--enable-modules"], ["--enable-modules"])

    asserts.equals(env, ["--enable-modules", "--enable-modules"], merged)

    return unittest.end(env)

keeps_cross_module_duplicates_test = unittest.make(_keeps_cross_module_duplicates_impl)

def concat_coursier_options_test_suite():
    unittest.suite(
        "concat_coursier_options_tests",
        preserves_repeated_tokens_test,
        concatenates_root_first_test,
        keeps_cross_module_duplicates_test,
    )
