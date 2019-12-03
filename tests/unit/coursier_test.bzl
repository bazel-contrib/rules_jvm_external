load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//:coursier.bzl",
    infer = "infer_artifact_path_from_primary_and_repos",
    "remove_auth_from_url",
)

ALL_TESTS = []
def add_test(test_impl_func):
    test = unittest.make(test_impl_func)
    ALL_TESTS.append(test)
    return test

def _infer_doc_example_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "group/path/to/artifact/file.jar",
        infer("http://a:b@c/group/path/to/artifact/file.jar", ["http://c"]))

infer_doc_example_test = add_test(_infer_doc_example_test_impl)

def _infer_basic_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "group/artifact/version/foo.jar",
        infer("https://base/group/artifact/version/foo.jar", ["https://base"]))
    asserts.equals(
        env,
        "group/artifact/version/foo.jar",
        infer("http://base/group/artifact/version/foo.jar", ["http://base"]))
    return unittest.end(env)

infer_basic_test = add_test(_infer_basic_test_impl)

def _infer_auth_basic_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "group1/artifact/version/foo.jar",
        infer("https://a@c/group1/artifact/version/foo.jar", ["https://a:b@c"]))
    asserts.equals(
        env,
        "group2/artifact/version/foo.jar",
        infer("https://a@c/group2/artifact/version/foo.jar", ["https://a@c"]))
    asserts.equals(
        env,
        "group3/artifact/version/foo.jar",
        infer("https://a@c/group3/artifact/version/foo.jar", ["https://c"]))
    return unittest.end(env)

infer_auth_basic_test = add_test(_infer_auth_basic_test_impl)

def _infer_leading_repo_miss_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "group/artifact/version/foo.jar",
        infer("https://a@c/group/artifact/version/foo.jar", ["https://a:b@c/missubdir", "https://a:b@c"]))
    return unittest.end(env)

infer_leading_repo_miss_test = add_test(_infer_leading_repo_miss_test_impl)

def _infer_repo_trailing_slash_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "group/artifact/version/foo.jar",
        infer("https://a@c/group/artifact/version/foo.jar", ["https://a:b@c"]))
    asserts.equals(
        env,
        "group/artifact/version/foo.jar",
        infer("https://a@c/group/artifact/version/foo.jar", ["https://a:b@c/"]))
    asserts.equals(
        env,
        "group/artifact/version/foo.jar",
        infer("https://a@c/group/artifact/version/foo.jar", ["https://a:b@c//"]))
    return unittest.end(env)

infer_repo_trailing_slash_test = add_test(_infer_repo_trailing_slash_test_impl)

def _remove_auth_basic_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "https://c1",
        remove_auth_from_url("https://a:b@c1"))
    return unittest.end(env)

remove_auth_basic_test = add_test(_remove_auth_basic_test_impl)

def _remove_auth_basic_with_path_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "https://c1/some/random/path",
        remove_auth_from_url("https://a:b@c1/some/random/path"))
    return unittest.end(env)

remove_auth_basic_with_path_test = add_test(_remove_auth_basic_with_path_test_impl)

def _remove_auth_only_user_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "https://c1",
        remove_auth_from_url("https://a@c1"))
    return unittest.end(env)

remove_auth_only_user_test = add_test(_remove_auth_only_user_test_impl)

def _remove_auth_noauth_noop_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "https://c1",
        remove_auth_from_url("https://c1"))
    return unittest.end(env)

remove_auth_noauth_noop_test = add_test(_remove_auth_noauth_noop_test_impl)

def coursier_test_suite():
    unittest.suite(
        "coursier_tests",
        *ALL_TESTS
    )
