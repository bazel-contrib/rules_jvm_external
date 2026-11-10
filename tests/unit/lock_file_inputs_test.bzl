load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:lock_file_inputs.bzl", "canonical_inputs")

def _request(version = "1", **kwargs):
    return dict(group = "example", artifact = "library", version = version, **kwargs)

def _normalises_effective_declarations_impl(ctx):
    env = unittest.begin(ctx)
    first = _request(force_version = False, exclusions = [{"group": "z", "artifact": "a"}, {"group": "a", "artifact": "b"}])
    second = _request(exclusions = ["a:b", "z:a"])
    snapshot = canonical_inputs(artifacts = [first, second])
    asserts.equals(env, [_request(exclusions = ["a:b", "z:a"])], snapshot["artifacts"])
    asserts.equals(env, snapshot, canonical_inputs(artifacts = [json.encode(second)]))
    return unittest.end(env)

normalises_effective_declarations_test = unittest.make(_normalises_effective_declarations_impl)

def _preserves_requested_versions_and_variants_impl(ctx):
    env = unittest.begin(ctx)
    original = canonical_inputs(artifacts = [_request()])
    for changed in [_request(version = "2"), _request(version = ""), _request(force_version = True), _request(classifier = "sources"), _request(packaging = "jar")]:
        asserts.true(env, original != canonical_inputs(artifacts = [changed]))
    asserts.equals(env, 2, len(canonical_inputs(artifacts = [_request(), _request(classifier = "sources")])["artifacts"]))
    return unittest.end(env)

preserves_requested_versions_and_variants_test = unittest.make(_preserves_requested_versions_and_variants_impl)

def _bom_order_and_resolution_settings_are_inputs_impl(ctx):
    env = unittest.begin(ctx)
    boms = [_request(version = "1"), _request(version = "2")]
    original = canonical_inputs(boms = boms)
    asserts.equals(env, boms, original["boms"])
    asserts.true(env, original != canonical_inputs(boms = list(reversed(boms))))
    for setting in [{"fetch_sources": True}, {"fetch_javadoc": True}, {"version_conflict_policy": "pinned"}, {"additional_coursier_options": ["--foo", "bar"]}, {"excluded_artifacts": ["a:b"]}, {"resolver": "gradle"}]:
        asserts.true(env, original != canonical_inputs(boms = boms, **setting))
    return unittest.end(env)

bom_order_and_resolution_settings_are_inputs_test = unittest.make(_bom_order_and_resolution_settings_are_inputs_impl)

def lock_file_inputs_test_suite():
    unittest.suite(
        "lock_file_inputs_tests",
        normalises_effective_declarations_test,
        preserves_requested_versions_and_variants_test,
        bom_order_and_resolution_settings_are_inputs_test,
    )
