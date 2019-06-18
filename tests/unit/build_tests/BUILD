load("@bazel_skylib//rules:build_test.bzl", "build_test")

# Regression tests for particular artifacts that revealed problems for this
# project.

build_test(
    name = "all_artifacts",
    targets = [
        "@pinned_regression_testing//:org_pantsbuild_jarjar",
        "@pinned_regression_testing//:com_github_fommil_netlib_all",
        "@pinned_regression_testing//:nz_ac_waikato_cms_weka_weka_stable",
        "@pinned_regression_testing//:com_digitalasset_damlc_osx",
        "@pinned_regression_testing//:org_eclipse_jetty_orbit_javax_servlet",
        "@pinned_regression_testing//:org_apache_flink_flink_test_utils_2_12",
    ],
)

build_test(
    name = "compat_repository_alias",
    targets = [
        "@org_pantsbuild_jarjar//jar",
        "@nz_ac_waikato_cms_weka_weka_stable//jar",
        "@com_digitalasset_damlc_osx//jar",
        "@org_apache_flink_flink_test_utils_2_12//jar",
    ],
)
