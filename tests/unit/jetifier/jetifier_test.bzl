load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load(
    "//private/rules:jetifier.bzl",
    "jetify_artifact_dependencies",
    "jetify_maven_coord",
)

ALL_TESTS = []

def add_test(test_impl_func):
    test = unittest.make(test_impl_func)
    ALL_TESTS.append(test)
    return test

def _jetify_artifact_dependencies_returns_original_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        ["mygroup:myartifact:myversion"],
        jetify_artifact_dependencies(["mygroup:myartifact:myversion"]),
    )
    return unittest.end(env)

jetify_artifact_dependencies_returns_original_test = add_test(_jetify_artifact_dependencies_returns_original_test)

def _jetify_artifact_dependencies_returns_jetified_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        ["androidx.multidex:multidex:2.0.0"],
        jetify_artifact_dependencies(["com.android.support:multidex:1.0.3"]),
    )
    return unittest.end(env)

jetify_artifact_dependencies_returns_jetified_test = add_test(_jetify_artifact_dependencies_returns_jetified_test)

def _jetify_maven_coord_no_match_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        None,
        jetify_maven_coord(group = "com.android.support", artifact = "multidex", version = "nomatch"),
    )
    return unittest.end(env)

jetify_maven_coord_no_match_test = add_test(_jetify_maven_coord_no_match_test)

def _jetify_maven_coord_finds_match_test(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        ("androidx.multidex", "multidex", "2.0.0"),
        jetify_maven_coord(group = "com.android.support", artifact = "multidex", version = "1.0.3"),
    )
    return unittest.end(env)

jetify_maven_coord_finds_match_test = add_test(_jetify_maven_coord_finds_match_test)

def jetifier_test_suite():
    unittest.suite(
        "jetifier_tests",
        *ALL_TESTS
    )
