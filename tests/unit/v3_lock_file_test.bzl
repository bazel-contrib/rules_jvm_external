load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:v3_lock_file.bzl", "v3_lock_file")

def _find_artifact(artifacts, coordinates):
    for artifact in artifacts:
        if artifact["coordinates"] == coordinates:
            return artifact
    fail("Unable to find artifact %s in %s" % (coordinates, artifacts))

def _null_shasum_artifacts_do_not_get_synthetic_file_impl(ctx):
    env = unittest.begin(ctx)

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
    asserts.equals(env, None, protoc["file"])
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

null_shasum_artifacts_do_not_get_synthetic_file_test = unittest.make(_null_shasum_artifacts_do_not_get_synthetic_file_impl)

def v3_lock_file_test_suite():
    unittest.suite(
        "v3_lock_file_tests",
        null_shasum_artifacts_do_not_get_synthetic_file_test,
    )
