"""Tests for layered (bzlmod) artifact deduplication with root priority."""

load("@bazel_skylib//lib:partial.bzl", "partial")
load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/extensions:maven.bzl", "deduplicate_artifacts_with_root_priority")
load("//private/lib:coordinates.bzl", "unpack_coordinates")

def _versions_of(artifacts, group, artifact):
    return [a.version for a in artifacts if a.group == group and a.artifact == artifact]

def _forced(coordinates):
    a = unpack_coordinates(coordinates)
    return struct(
        group = a.group,
        artifact = a.artifact,
        version = a.version,
        packaging = getattr(a, "packaging", None),
        classifier = getattr(a, "classifier", None),
        force_version = True,
    )

def _non_root_higher_version_evicts_root_artifact_impl(ctx):
    env = unittest.begin(ctx)

    # Regression test for the duplicate that survived dedup: the root
    # declares 2.33, a contributing module declares 2.37, nothing is
    # forced. Highest version wins -- and the losing root artifact must
    # leave the list, or _check_artifacts_are_unique() fails installs
    # with duplicate_version_warning = "error".
    merged = deduplicate_artifacts_with_root_priority(
        "external_deps",
        [unpack_coordinates("args4j:args4j:2.33")],
        {"jgit": [unpack_coordinates("args4j:args4j:2.37")]},
        None,
        None,
    )

    asserts.equals(env, ["2.37"], _versions_of(merged, "args4j", "args4j"))

    return unittest.end(env)

non_root_higher_version_evicts_root_artifact_test = unittest.make(_non_root_higher_version_evicts_root_artifact_impl)

def _root_higher_version_drops_non_root_artifact_impl(ctx):
    env = unittest.begin(ctx)

    merged = deduplicate_artifacts_with_root_priority(
        "external_deps",
        [unpack_coordinates("args4j:args4j:2.37")],
        {"jgit": [unpack_coordinates("args4j:args4j:2.33")]},
        None,
        None,
    )

    asserts.equals(env, ["2.37"], _versions_of(merged, "args4j", "args4j"))

    return unittest.end(env)

root_higher_version_drops_non_root_artifact_test = unittest.make(_root_higher_version_drops_non_root_artifact_impl)

def _forced_root_version_beats_higher_non_root_impl(ctx):
    env = unittest.begin(ctx)

    merged = deduplicate_artifacts_with_root_priority(
        "external_deps",
        [_forced("args4j:args4j:2.33")],
        {"jgit": [unpack_coordinates("args4j:args4j:2.37")]},
        None,
        None,
    )

    asserts.equals(env, ["2.33"], _versions_of(merged, "args4j", "args4j"))

    return unittest.end(env)

forced_root_version_beats_higher_non_root_test = unittest.make(_forced_root_version_beats_higher_non_root_impl)

def _non_overlapping_artifacts_pass_through_impl(ctx):
    env = unittest.begin(ctx)

    merged = deduplicate_artifacts_with_root_priority(
        "external_deps",
        [unpack_coordinates("args4j:args4j:2.33")],
        {"jgit": [unpack_coordinates("com.googlecode.javaewah:JavaEWAH:1.2.3")]},
        None,
        None,
    )

    asserts.equals(env, ["2.33"], _versions_of(merged, "args4j", "args4j"))
    asserts.equals(env, ["1.2.3"], _versions_of(merged, "com.googlecode.javaewah", "JavaEWAH"))

    return unittest.end(env)

non_overlapping_artifacts_pass_through_test = unittest.make(_non_overlapping_artifacts_pass_through_impl)

def layered_dedup_test_suite():
    unittest.suite(
        "layered_dedup_tests",
        partial.make(non_root_higher_version_evicts_root_artifact_test, size = "small"),
        partial.make(root_higher_version_drops_non_root_artifact_test, size = "small"),
        partial.make(forced_root_version_beats_higher_non_root_test, size = "small"),
        partial.make(non_overlapping_artifacts_pass_through_test, size = "small"),
    )
