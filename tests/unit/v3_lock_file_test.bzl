load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:v3_lock_file.bzl", "v3_lock_file")

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
            ],
        },
        "repositories": {},
        "services": {},
        "version": "3",
    })

    root = [artifact for artifact in artifacts if artifact["coordinates"] == "com.example:root:1.0"][0]
    asserts.equals(env, ["com.example:dep:spoofed-version:test-fixtures@jar"], root["deps"])

    return unittest.end(env)

get_artifacts_keeps_classifier_dependency_targets_test = unittest.make(
    _get_artifacts_keeps_classifier_dependency_targets_test_impl,
)

def _compute_lock_file_hash_accepts_cleaned_aggregator_collision_test_impl(ctx):
    env = unittest.begin(ctx)

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
            "com.example:umbrella:jar:unshaded": ["com.example:dep"],
        },
        "repositories": {
            "https://example.com/repo/": [
                "com.example:dep",
                "com.example:umbrella:jar:unshaded",
            ],
        },
        "version": "3",
    })

    asserts.true(env, "com.example:umbrella:jar:unshaded" in hashes)
    asserts.true(env, "com.example:dep" in hashes)
    asserts.false(env, "com.example:umbrella" in hashes)

    return unittest.end(env)

compute_lock_file_hash_accepts_cleaned_aggregator_collision_test = unittest.make(
    _compute_lock_file_hash_accepts_cleaned_aggregator_collision_test_impl,
)

def v3_lock_file_test_suite():
    unittest.suite(
        "v3_lock_file_tests",
        get_artifacts_keeps_classifier_dependency_targets_test,
        compute_lock_file_hash_accepts_cleaned_aggregator_collision_test,
    )
