load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")

def _does_aar_import_have_srcjar_impl(ctx):
    env = analysistest.begin(ctx)

    source_jars = analysistest.target_under_test(env)[JavaInfo].source_jars
    asserts.true(
        env,
        len(source_jars) > 0,
        """
@starlark_aar_import_with_sources_test//:androidx_activity_activity_compose_1_4_0_beta01 is expected to have a srcjar provided to the aar_import but found:
    {}
        """.format(source_jars),
    )

    return analysistest.end(env)

does_aar_import_have_srcjar_test = analysistest.make(_does_aar_import_have_srcjar_impl)

def _does_aar_import_not_have_srcjar_test_impl(ctx):
    env = analysistest.begin(ctx)

    source_jars = analysistest.target_under_test(env)[JavaInfo].source_jars
    asserts.true(
        env,
        len(source_jars) <= 0,
        """
@starlark_aar_import_test//:com_android_support_appcompat_v7_28_0_0 is expected to have a srcjar provided to the aar_import but found:
    {}
        """.format(source_jars),
    )

    return analysistest.end(env)

does_aar_import_not_have_srcjar_test = analysistest.make(_does_aar_import_not_have_srcjar_test_impl)

def aar_import_test_suite(name):
    does_aar_import_have_srcjar_test(
        name = "does_aar_import_have_srcjar_test",
        target_under_test = "@starlark_aar_import_with_sources_test//:androidx_activity_activity_compose_1_4_0_beta01",
    )

    does_aar_import_not_have_srcjar_test(
        name = "does_aar_import_not_have_srcjar_test",
        target_under_test = "@starlark_aar_import_test//:com_android_support_appcompat_v7_28_0_0",
    )

    native.test_suite(
        name = name,
        tests = [
            ":does_aar_import_have_srcjar_test",
            ":does_aar_import_not_have_srcjar_test",
        ],
    )
