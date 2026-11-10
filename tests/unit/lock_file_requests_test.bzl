load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/rules:lock_file_inputs.bzl", "canonical_inputs")
load("//private/rules:lock_file_requests.bzl", "attach_requests", "read_inputs")

def _declared_version_is_preserved_when_selection_differs_impl(ctx):
    env = unittest.begin(ctx)
    inputs = canonical_inputs(artifacts = [{"group": "g", "artifact": "a", "version": "1"}, {"group": "g", "artifact": "a", "version": "2", "force_version": True}])
    lock = {"artifacts": [["g:a:2", None, 0]]}
    attach_requests(lock, inputs)
    asserts.equals(env, [{"force_version": True}, {"version": "1"}], lock["artifacts"][0][3]["requests"])
    asserts.equals(env, inputs, read_inputs(lock))
    return unittest.end(env)

declared_version_is_preserved_when_selection_differs_test = unittest.make(_declared_version_is_preserved_when_selection_differs_impl)

def _missing_classifier_is_not_attached_to_main_jar_impl(ctx):
    env = unittest.begin(ctx)
    request = {"group": "g", "artifact": "a", "version": "", "classifier": "native"}
    inputs = canonical_inputs(artifacts = [request])
    lock = {"artifacts": [["g:a:2", None, 0], ["g:a:2:sources", None, 0]]}
    attach_requests(lock, inputs)
    asserts.equals(env, [request], lock["unresolved_requests"])
    asserts.false(env, "requests" in lock["artifacts"][0][3])
    asserts.equals(env, ["g:a:jar:sources"], lock["artifacts"][0][3]["variants"])
    asserts.equals(env, inputs, read_inputs(lock))
    return unittest.end(env)

missing_classifier_is_not_attached_to_main_jar_test = unittest.make(_missing_classifier_is_not_attached_to_main_jar_impl)

def _infers_packaging_only_when_omitted_impl(ctx):
    env = unittest.begin(ctx)
    inputs = canonical_inputs(artifacts = [{"group": "g", "artifact": "a", "version": "1"}, {"group": "g", "artifact": "a", "version": "1", "packaging": "jar"}])
    lock = {"artifacts": [["g:a:1@dylib", None, 0]]}
    attach_requests(lock, inputs)
    asserts.equals(env, [{}], lock["artifacts"][0][3]["requests"])
    asserts.equals(env, [{"group": "g", "artifact": "a", "version": "1", "packaging": "jar"}], lock["unresolved_requests"])
    asserts.equals(env, inputs, read_inputs(lock))
    return unittest.end(env)

infers_packaging_only_when_omitted_test = unittest.make(_infers_packaging_only_when_omitted_impl)

def _infers_download_packaging_when_pom_is_also_present_impl(ctx):
    env = unittest.begin(ctx)
    inputs = canonical_inputs(artifacts = [{"group": "g", "artifact": "a", "version": "1"}])
    lock = {"artifacts": [["g:a:1@json", None, 0], ["g:a:1@pom", None, 0]]}
    attach_requests(lock, inputs)
    asserts.equals(env, [{}], lock["artifacts"][0][3]["requests"])
    asserts.equals(env, 3, len(lock["artifacts"][1]))
    asserts.equals(env, inputs, read_inputs(lock))
    return unittest.end(env)

infers_download_packaging_when_pom_is_also_present_test = unittest.make(_infers_download_packaging_when_pom_is_also_present_impl)

def lock_file_requests_test_suite():
    unittest.suite(
        "lock_file_requests_tests",
        declared_version_is_preserved_when_selection_differs_test,
        missing_classifier_is_not_attached_to_main_jar_test,
        infers_packaging_only_when_omitted_test,
        infers_download_packaging_when_pom_is_also_present_test,
    )
