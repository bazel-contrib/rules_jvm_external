load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:lock_file_records.bzl", "compute_hashes", "records_from_v3", "row_hash")

def _canonical_hash_matches_java_impl(ctx):
    env = unittest.begin(ctx)
    row = ["g:a:1", None, 0, {"requests": [{"testonly": True, "version": "[1,2)"}]}]
    asserts.equals(env, 1788491109, row_hash(row, ["g:c", "g:b"], False, True))
    return unittest.end(env)

canonical_hash_matches_java_test = unittest.make(_canonical_hash_matches_java_impl)

def _child_versions_and_repositories_do_not_change_parent_hash_impl(ctx):
    env = unittest.begin(ctx)
    lock = {
        "artifacts": [["g:parent:1", "a" * 64, 0], ["g:child:1", "b" * 64, 0]],
        "dependencies": {"g:parent": ["g:child"]},
        "repositories": {"repo": ["g:parent", "g:child"]},
    }
    before = compute_hashes(lock)
    lock["artifacts"][1][0] = "g:child:2"
    lock["repositories"] = {"mirror": ["g:child"], "another": ["g:parent"]}
    after = compute_hashes(lock)
    asserts.equals(env, before["g:parent"], after["g:parent"])
    asserts.true(env, before["g:child"] != after["g:child"])
    lock["dependencies"]["g:parent"] = []
    asserts.true(env, before["g:parent"] != compute_hashes(lock)["g:parent"])
    lock["dependencies"] = {"g:parent": ["g:child"], "g:child": ["g:parent"]}
    asserts.equals(env, before["g:parent"], compute_hashes(lock)["g:parent"])
    lock["dependencies"].pop("g:child")
    lock["artifacts"].pop()
    asserts.equals(env, None, compute_hashes(lock)["g:parent"])
    return unittest.end(env)

child_versions_and_repositories_do_not_change_parent_hash_test = unittest.make(_child_versions_and_repositories_do_not_change_parent_hash_impl)

def _converts_classifiers_and_non_jar_packaging_impl(ctx):
    env = unittest.begin(ctx)
    original = {"artifacts": {
        "g:library": {"version": "1", "shasums": {"jar": "a" * 64, "sources": None}},
        "g:archive:tgz": {"version": "2", "shasums": {"linux": "b" * 64}},
    }}
    rows = records_from_v3(original)
    asserts.equals(env, ["g:archive:2:linux@tgz", "g:library:1", "g:library:1:sources"], [row[0] for row in rows])
    asserts.equals(env, None, rows[2][1])
    return unittest.end(env)

converts_classifiers_and_non_jar_packaging_test = unittest.make(_converts_classifiers_and_non_jar_packaging_impl)

def _named_versions_keep_their_classifier_impl(ctx):
    env = unittest.begin(ctx)
    lock = {"artifacts": [["g:a:RELEASE:sources", None, 0]]}
    asserts.equals(env, ["g:a:jar:sources"], compute_hashes(lock).keys())
    return unittest.end(env)

named_versions_keep_their_classifier_test = unittest.make(_named_versions_keep_their_classifier_impl)

def lock_file_records_test_suite():
    unittest.suite(
        "lock_file_records_tests",
        canonical_hash_matches_java_test,
        named_versions_keep_their_classifier_test,
        child_versions_and_repositories_do_not_change_parent_hash_test,
        converts_classifiers_and_non_jar_packaging_test,
    )
