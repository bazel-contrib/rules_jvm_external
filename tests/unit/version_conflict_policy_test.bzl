"""Tests for resolver-specific version conflict policies."""

load("@bazel_skylib//lib:partial.bzl", "partial")
load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/extensions:maven.bzl", "apply_root_version_conflict_policy")
load("//private/lib:coordinates.bzl", "unpack_coordinates")

def _pinned_policy_forces_versioned_root_artifacts_impl(ctx):
    env = unittest.begin(ctx)

    for resolver in ["gradle", "maven"]:
        versioned = unpack_coordinates("com.example:root:1.0")
        versionless = unpack_coordinates("com.example:managed-by-bom")

        artifacts = apply_root_version_conflict_policy(
            [versioned, versionless],
            resolver,
            "pinned",
        )

        asserts.true(env, artifacts[0].force_version)
        asserts.false(env, hasattr(artifacts[1], "force_version"))

    return unittest.end(env)

pinned_policy_forces_versioned_root_artifacts_test = unittest.make(_pinned_policy_forces_versioned_root_artifacts_impl)

def _other_policies_leave_root_artifacts_unchanged_impl(ctx):
    env = unittest.begin(ctx)

    artifact = unpack_coordinates("com.example:root:1.0")

    default_artifacts = apply_root_version_conflict_policy([artifact], "gradle", "default")
    coursier_artifacts = apply_root_version_conflict_policy([artifact], "coursier", "pinned")

    asserts.false(env, hasattr(default_artifacts[0], "force_version"))
    asserts.false(env, hasattr(coursier_artifacts[0], "force_version"))

    return unittest.end(env)

other_policies_leave_root_artifacts_unchanged_test = unittest.make(_other_policies_leave_root_artifacts_unchanged_impl)

def version_conflict_policy_test_suite():
    unittest.suite(
        "version_conflict_policy_tests",
        partial.make(pinned_policy_forces_versioned_root_artifacts_test, size = "small"),
        partial.make(other_policies_leave_root_artifacts_unchanged_test, size = "small"),
    )
