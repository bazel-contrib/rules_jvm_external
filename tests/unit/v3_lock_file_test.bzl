load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:v3_lock_file.bzl", "v3_lock_file")

def _get_artifacts_ignores_dependency_targets_without_artifacts_test_impl(ctx):
    env = unittest.begin(ctx)

    artifacts = v3_lock_file.get_artifacts({
        "artifacts": {
            "com.example:dep": {
                "shasums": {
                    "jar": "def",
                },
                "version": "1.0",
            },
            "com.example:root": {
                "shasums": {
                    "jar": "abc",
                },
                "version": "1.0",
            },
        },
        "dependencies": {
            "com.example:root": [
                "com.example:dep",
                "com.example:missing-native",
            ],
        },
        "repositories": {},
        "services": {},
        "version": "3",
    })

    # `com.example:missing-native` has no artifact entry, so the dependency edge is dropped rather
    # than emitting a dangling target reference.
    root = [artifact for artifact in artifacts if artifact["coordinates"] == "com.example:root:1.0"][0]
    asserts.equals(env, ["com.example:dep:spoofed-version"], root["deps"])

    return unittest.end(env)

get_artifacts_ignores_dependency_targets_without_artifacts_test = unittest.make(
    _get_artifacts_ignores_dependency_targets_without_artifacts_test_impl,
)

def _get_artifacts_keeps_classifier_dependency_targets_test_impl(ctx):
    env = unittest.begin(ctx)

    artifacts = v3_lock_file.get_artifacts({
        "artifacts": {
            "com.example:dep": {
                "shasums": {
                    "test-fixtures": "def",
                },
                "version": "1.0",
            },
            "com.example:root": {
                "shasums": {
                    "jar": "abc",
                },
                "version": "1.0",
            },
        },
        "dependencies": {
            "com.example:root": [
                "com.example:dep:jar:test-fixtures",
                "com.example:missing-native",
            ],
        },
        "repositories": {},
        "services": {},
        "version": "3",
    })

    # A classifier dependency target that does have a matching shasum is preserved.
    root = [artifact for artifact in artifacts if artifact["coordinates"] == "com.example:root:1.0"][0]
    asserts.equals(env, ["com.example:dep:spoofed-version:test-fixtures@jar"], root["deps"])

    return unittest.end(env)

get_artifacts_keeps_classifier_dependency_targets_test = unittest.make(
    _get_artifacts_keeps_classifier_dependency_targets_test_impl,
)

def _compute_lock_file_hash_tolerates_aggregator_sharing_key_with_sibling_test_impl(ctx):
    env = unittest.begin(ctx)

    # A binary-less aggregator (`com.example:umbrella`) shares its `group:artifact` key with a
    # classified sibling (`com.example:umbrella:jar:unshaded`) that owns the only shasum. The plain
    # key still appears in `repositories` and `dependencies` but is never reconstructed into the
    # hash's intermediate map, so computing the hash must not fail on the missing key.
    hashes = v3_lock_file.compute_lock_file_hash({
        "artifacts": {
            "com.example:dep": {
                "shasums": {
                    "jar": "def",
                },
                "version": "1.0",
            },
            "com.example:umbrella": {
                "shasums": {
                    "unshaded": "abc",
                },
                "version": "1.0",
            },
        },
        "dependencies": {
            "com.example:umbrella": ["com.example:dep"],
            "com.example:umbrella:jar:unshaded": ["com.example:dep"],
        },
        "repositories": {
            "https://example.com/repo/": [
                "com.example:dep",
                "com.example:umbrella",
                "com.example:umbrella:jar:unshaded",
            ],
        },
        "version": "3",
    })

    asserts.true(env, "com.example:umbrella:jar:unshaded" in hashes)
    asserts.true(env, "com.example:dep" in hashes)
    asserts.false(env, "com.example:umbrella" in hashes)

    return unittest.end(env)

compute_lock_file_hash_tolerates_aggregator_sharing_key_with_sibling_test = unittest.make(
    _compute_lock_file_hash_tolerates_aggregator_sharing_key_with_sibling_test_impl,
)

def v3_lock_file_test_suite():
    unittest.suite(
        "v3_lock_file_tests",
        get_artifacts_ignores_dependency_targets_without_artifacts_test,
        get_artifacts_keeps_classifier_dependency_targets_test,
        compute_lock_file_hash_tolerates_aggregator_sharing_key_with_sibling_test,
    )
