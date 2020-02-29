load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load(
    "//private/rules:jetifier.bzl",
    "jetify_maven_coord",
    "jetify_coord_str",
)

ALL_TESTS = []

def add_test(test_impl_func):
    test = unittest.make(test_impl_func)
    ALL_TESTS.append(test)
    return test

def _jetify_coord_str_returns_original_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "mygroup:myartifact:myversion",
        jetify_coord_str("mygroup:myartifact:myversion")
    )
    return unittest.end(env)

jetify_coord_str_returns_original_test = add_test(_jetify_coord_str_returns_original_test)

def _jetify_coord_str_returns_jetified_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "androidx.multidex:multidex:2.0.0",
        jetify_coord_str("com.android.support:multidex:1.0.3")
    )
    return unittest.end(env)

jetify_coord_str_returns_jetified_test = add_test(_jetify_coord_str_returns_jetified_test)

def _jetify_maven_coord_no_match_without_version_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        None,
        jetify_maven_coord(group = "nomatch", artifact = "nomatch"),
    )
    return unittest.end(env)

jetify_maven_coord_no_match_without_version_test = add_test(_jetify_maven_coord_no_match_without_version_test)

def _jetify_maven_coord_no_match_with_version_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        None,
        jetify_maven_coord(group = "com.android.support", artifact = "multidex", version = "nomatch"),
    )
    return unittest.end(env)

jetify_maven_coord_no_match_with_version_test = add_test(_jetify_maven_coord_no_match_with_version_test)

def _jetify_maven_coord_finds_match_without_version_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        ("androidx.multidex", "multidex"),
        jetify_maven_coord(group = "com.android.support", artifact = "multidex"),
    )
    return unittest.end(env)

jetify_maven_coord_finds_match_without_version_test = add_test(_jetify_maven_coord_finds_match_without_version_test)

def _jetify_maven_coord_finds_match_with_version_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        ("androidx.multidex", "multidex", "2.0.0"),
        jetify_maven_coord(group = "com.android.support", artifact = "multidex", version = "1.0.3"),
    )
    return unittest.end(env)

jetify_maven_coord_finds_match_with_version_test = add_test(_jetify_maven_coord_finds_match_with_version_test)



def jetifier_test_suite():
    unittest.suite(
        "jetifier_tests",
        *ALL_TESTS
    )
