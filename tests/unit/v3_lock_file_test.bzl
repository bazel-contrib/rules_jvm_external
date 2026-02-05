load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:v3_lock_file.bzl", "v3_lock_file")

def _v3_filters_null_shasums_impl(ctx):
    env = unittest.begin(ctx)

    lock_file_contents = {
        "artifacts": {
            "com.android.support:appcompat-v7:aar": {
                "version": "28.0.0",
                "shasums": {
                    "aar": "abc123",
                    "sources": None,  # Sources that don't exist for AAR-only dependency
                },
            },
            "com.android.support:design:aar": {
                "version": "28.0.0",
                "shasums": {
                    "aar": "def456",
                    "sources": "ghi789",
                },
            },
            "com.google.guava:guava": {
                "version": "30.0-jre",
                "shasums": {
                    "jar": "jkl012",
                    "sources": "mno345",
                },
            },
        },
    }

    artifacts = v3_lock_file.get_artifacts(lock_file_contents)

    asserts.equals(env, 5, len(artifacts))
    
    # Verify the phantom artifact is not present
    coordinates_list = [a["coordinates"] for a in artifacts]
    asserts.true(env, "com.android.support:appcompat-v7:28.0.0:aar@aar" in coordinates_list)
    asserts.false(env, "com.android.support:appcompat-v7:28.0.0:sources@aar" in coordinates_list, "Phantom sources should be filtered")

    return unittest.end(env)

v3_filters_null_shasums_test = unittest.make(_v3_filters_null_shasums_impl)

def _v3_keeps_valid_artifacts_impl(ctx):
    env = unittest.begin(ctx)

    # Verify normal artifacts with valid shasums are not affected
    lock_file_contents = {
        "artifacts": {
            "com.google.guava:guava": {
                "version": "30.0-jre",
                "shasums": {
                    "jar": "abc123",
                    "sources": "def456",
                },
            },
            "com.android.support:design:aar": {
                "version": "28.0.0",
                "shasums": {
                    "aar": "ghi789",
                    "sources": "jkl012",
                },
            },
        },
    }

    artifacts = v3_lock_file.get_artifacts(lock_file_contents)

    # Should get 4 artifacts (JAR + JAR sources + AAR + AAR sources)
    asserts.equals(env, 4, len(artifacts))
    
    # Verify all artifacts are present with correct shasums
    coordinates_to_sha = {a["coordinates"]: a["sha256"] for a in artifacts}
    
    asserts.equals(env, "abc123", coordinates_to_sha.get("com.google.guava:guava:30.0-jre"))
    asserts.equals(env, "def456", coordinates_to_sha.get("com.google.guava:guava:30.0-jre:sources"))
    asserts.equals(env, "ghi789", coordinates_to_sha.get("com.android.support:design:28.0.0:aar@aar"))
    asserts.equals(env, "jkl012", coordinates_to_sha.get("com.android.support:design:28.0.0:sources@aar"))

    return unittest.end(env)

v3_keeps_valid_artifacts_test = unittest.make(_v3_keeps_valid_artifacts_impl)

def v3_lock_file_test_suite():
    unittest.suite(
        "v3_lock_file_tests",
        v3_filters_null_shasums_test,
        v3_keeps_valid_artifacts_test,
    )
