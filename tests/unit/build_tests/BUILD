load("@bazel_skylib//rules:build_test.bzl", "build_test")

# Regression tests for particular artifacts that revealed problems for this
# project.

build_test(
    name = "all_artifacts",
    targets = [
        "@regression_testing//:org_pantsbuild_jarjar",
        "@regression_testing//:com_github_fommil_netlib_all",
        "@regression_testing//:nz_ac_waikato_cms_weka_weka_stable",
        "@regression_testing//:com_digitalasset_damlc_osx",
        "@regression_testing//:org_eclipse_jetty_orbit_javax_servlet",
        "@regression_testing//:org_apache_flink_flink_test_utils_2_12",
        "@maven//:org_openjfx_javafx_base_mac",
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
