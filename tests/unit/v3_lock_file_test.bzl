load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:v3_lock_file.bzl", "v3_lock_file")

def _find_artifact(artifacts, coordinates):
    for artifact in artifacts:
        if artifact["coordinates"] == coordinates:
            return artifact
    fail("Unable to find artifact %s in %s" % (coordinates, artifacts))

def _null_shasum_artifact_not_in_repo_gets_no_urls_impl(ctx):
    env = unittest.begin(ctx)

    # An artifact with a null shasum that is not listed in any repository
    # gets a file path but no urls, so no http_file is created for it.
    lock_file_contents = {
        "artifacts": {
            "com.google.protobuf:protoc": {
                "shasums": {
                    "jar": None,
                },
                "version": "4.34.0",
            },
            "com.google.protobuf:protoc:pom": {
                "shasums": {
                    "jar": "3c93425cd60be25f08a9a43d490c9cf4979052749286ccde0ce26702ad34c851",
                },
                "version": "4.34.0",
            },
        },
        "dependencies": {},
        "files": {},
        "repositories": {
            "https://repo.maven.apache.org/maven2/": [
                "com.google.protobuf:protoc:pom",
            ],
        },
        "services": {},
        "skipped": [],
        "version": "3",
    }

    artifacts = v3_lock_file.get_artifacts(lock_file_contents)

    protoc = _find_artifact(artifacts, "com.google.protobuf:protoc:4.34.0")
    asserts.equals(env, None, protoc["sha256"])
    asserts.equals(env, [], protoc["urls"])

    protoc_pom = _find_artifact(artifacts, "com.google.protobuf:protoc:4.34.0@pom")
    asserts.equals(
        env,
        "com/google/protobuf/protoc/4.34.0/protoc-4.34.0.pom",
        protoc_pom["file"],
    )
    asserts.equals(
        env,
        ["https://repo.maven.apache.org/maven2/com/google/protobuf/protoc/4.34.0/protoc-4.34.0.pom"],
        protoc_pom["urls"],
    )

    return unittest.end(env)

def _m2local_null_shasum_snapshot_gets_file_path_impl(ctx):
    env = unittest.begin(ctx)

    # A non-timestamped snapshot from m2Local has a null sha256 (content is mutable).
    # NOTE: get_artifacts does not branch on the m2local flag itself (the m2local file:// URL is
    # built later by download_pinned_deps/get_m2local_url). What this pins is the get_artifacts
    # contract that a null-sha artifact with no remote repository still exposes a "file" path (so
    # that later URL construction has something to work with) and emits no urls. The m2local flag
    # is included to model a realistic m2Local lock file.
    lock_file_contents = {
        "artifacts": {
            "com.example:snapshot": {
                "shasums": {
                    "jar": None,
                },
                "version": "1.0-SNAPSHOT",
            },
        },
        "dependencies": {},
        "files": {},
        "m2local": True,
        "repositories": {},
        "services": {},
        "skipped": [],
        "version": "3",
    }

    artifacts = v3_lock_file.get_artifacts(lock_file_contents)

    snapshot = _find_artifact(artifacts, "com.example:snapshot:1.0-SNAPSHOT")
    asserts.equals(env, None, snapshot["sha256"])
    # file must be set so get_m2local_url can build a file:// URL
    asserts.equals(
        env,
        "com/example/snapshot/1.0-SNAPSHOT/snapshot-1.0-SNAPSHOT.jar",
        snapshot["file"],
    )
    # no remote repositories, so urls is empty (m2local URL is added later by download_pinned_deps)
    asserts.equals(env, [], snapshot["urls"])

    return unittest.end(env)

def _remote_null_shasum_snapshot_gets_urls_impl(ctx):
    env = unittest.begin(ctx)

    # A non-timestamped snapshot from a remote repository has a null sha256
    # (content is mutable) but still gets urls so it can be downloaded without
    # checksum verification.
    lock_file_contents = {
        "artifacts": {
            "com.example:snapshot": {
                "shasums": {
                    "jar": None,
                },
                "version": "1.0-SNAPSHOT",
            },
        },
        "dependencies": {},
        "files": {},
        "repositories": {
            "https://snapshots.example.com/": [
                "com.example:snapshot",
            ],
        },
        "services": {},
        "skipped": [],
        "version": "3",
    }

    artifacts = v3_lock_file.get_artifacts(lock_file_contents)

    snapshot = _find_artifact(artifacts, "com.example:snapshot:1.0-SNAPSHOT")
    asserts.equals(env, None, snapshot["sha256"])
    asserts.equals(
        env,
        "com/example/snapshot/1.0-SNAPSHOT/snapshot-1.0-SNAPSHOT.jar",
        snapshot["file"],
    )
    asserts.equals(
        env,
        ["https://snapshots.example.com/com/example/snapshot/1.0-SNAPSHOT/snapshot-1.0-SNAPSHOT.jar"],
        snapshot["urls"],
    )

    return unittest.end(env)

def _timestamped_snapshot_uses_snapshot_dir_in_path_impl(ctx):
    env = unittest.begin(ctx)

    # A timestamped snapshot keeps its sha256 (immutable) and uses the
    # -SNAPSHOT base version in the directory path, timestamped version in the filename.
    lock_file_contents = {
        "artifacts": {
            "com.google.guava:guava": {
                "shasums": {
                    "jar": "d89e0f34a41aeb50714adf632b1e1feb22954858ff86c0ad2f604c4c8ed3185e",
                },
                "version": "999.0.0-HEAD-jre-20260626.231353-374",
            },
        },
        "dependencies": {},
        "files": {},
        "m2local": True,
        "repositories": {},
        "services": {},
        "skipped": [],
        "version": "3",
    }

    artifacts = v3_lock_file.get_artifacts(lock_file_contents)

    guava = _find_artifact(artifacts, "com.google.guava:guava:999.0.0-HEAD-jre-20260626.231353-374")
    asserts.equals(
        env,
        "d89e0f34a41aeb50714adf632b1e1feb22954858ff86c0ad2f604c4c8ed3185e",
        guava["sha256"],
    )
    asserts.equals(
        env,
        "com/google/guava/guava/999.0.0-HEAD-jre-SNAPSHOT/guava-999.0.0-HEAD-jre-20260626.231353-374.jar",
        guava["file"],
    )

    return unittest.end(env)

null_shasum_artifact_not_in_repo_gets_no_urls_test = unittest.make(_null_shasum_artifact_not_in_repo_gets_no_urls_impl)
m2local_null_shasum_snapshot_gets_file_path_test = unittest.make(_m2local_null_shasum_snapshot_gets_file_path_impl)
remote_null_shasum_snapshot_gets_urls_test = unittest.make(_remote_null_shasum_snapshot_gets_urls_impl)
timestamped_snapshot_uses_snapshot_dir_in_path_test = unittest.make(_timestamped_snapshot_uses_snapshot_dir_in_path_impl)

def v3_lock_file_test_suite():
    unittest.suite(
        "v3_lock_file_tests",
        null_shasum_artifact_not_in_repo_gets_no_urls_test,
        m2local_null_shasum_snapshot_gets_file_path_test,
        remote_null_shasum_snapshot_gets_urls_test,
        timestamped_snapshot_uses_snapshot_dir_in_path_test,
    )
