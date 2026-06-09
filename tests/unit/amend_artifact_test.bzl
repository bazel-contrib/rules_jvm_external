"""Tests for `amend_artifact` matching both artifacts and BOMs."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/extensions:maven.bzl", "apply_amendment")
load("//private/lib:coordinates.bzl", "unpack_coordinates")

def _amend(coordinates, force_version = None, neverlink = None, testonly = None, exclusions = None):
    """Builds a struct mirroring an `amend_artifact` tag."""
    return struct(
        coordinates = coordinates,
        force_version = force_version,
        neverlink = neverlink,
        testonly = testonly,
        exclusions = exclusions,
    )

def _amends_artifact_impl(ctx):
    env = unittest.begin(ctx)

    artifacts = [unpack_coordinates("com.google.guava:guava:31.1-jre")]
    boms = []

    matched = apply_amendment(_amend("com.google.guava:guava", testonly = "true"), artifacts, boms)

    asserts.true(env, matched)
    asserts.equals(env, True, artifacts[0].testonly)
    asserts.equals(env, "31.1-jre", artifacts[0].version)

    return unittest.end(env)

amends_artifact_test = unittest.make(_amends_artifact_impl)

def _amends_bom_impl(ctx):
    env = unittest.begin(ctx)

    artifacts = []
    boms = [unpack_coordinates("com.google.protobuf:protobuf-bom:3.25.5")]

    matched = apply_amendment(_amend("com.google.protobuf:protobuf-bom", force_version = "on"), artifacts, boms)

    asserts.true(env, matched)
    asserts.equals(env, "com.google.protobuf", boms[0].group)
    asserts.equals(env, "protobuf-bom", boms[0].artifact)
    asserts.equals(env, True, boms[0].force_version)

    # Amending the BOM must not change its declared version.
    asserts.equals(env, "3.25.5", boms[0].version)

    return unittest.end(env)

amends_bom_test = unittest.make(_amends_bom_impl)

def _amends_bom_alongside_artifacts_impl(ctx):
    env = unittest.begin(ctx)

    artifacts = [unpack_coordinates("com.google.guava:guava:31.1-jre")]
    boms = [unpack_coordinates("com.google.protobuf:protobuf-bom:3.25.5")]

    matched = apply_amendment(_amend("com.google.protobuf:protobuf-bom", force_version = "on"), artifacts, boms)

    asserts.true(env, matched)
    asserts.equals(env, True, boms[0].force_version)

    # The artifact that wasn't targeted is left untouched: a raw unpacked
    # artifact has no `force_version` field, whereas an amended one always does.
    asserts.false(env, hasattr(artifacts[0], "force_version"))

    return unittest.end(env)

amends_bom_alongside_artifacts_test = unittest.make(_amends_bom_alongside_artifacts_impl)

def _no_match_returns_false_impl(ctx):
    env = unittest.begin(ctx)

    artifacts = [unpack_coordinates("com.google.guava:guava:31.1-jre")]
    boms = [unpack_coordinates("com.google.protobuf:protobuf-bom:3.25.5")]

    matched = apply_amendment(_amend("org.example:does-not-exist", force_version = "on"), artifacts, boms)

    # The caller relies on this being False to fail the build with a helpful message.
    asserts.false(env, matched)

    return unittest.end(env)

no_match_returns_false_test = unittest.make(_no_match_returns_false_impl)

def amend_artifact_test_suite():
    unittest.suite(
        "amend_artifact_tests",
        amends_artifact_test,
        amends_bom_test,
        amends_bom_alongside_artifacts_test,
        no_match_returns_false_test,
    )
