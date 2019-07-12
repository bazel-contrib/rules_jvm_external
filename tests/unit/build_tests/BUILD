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
        "@regression_testing//:ch_epfl_scala_compiler_interface",
        "@regression_testing//:org_openjfx_javafx_base",
        "@regression_testing//:io_kubernetes_client_java",
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

build_test(
    name = "compat_repository_alias_jarless",
    targets = [
        "@org_pantsbuild_jarjar",
        "@nz_ac_waikato_cms_weka_weka_stable",
        "@com_digitalasset_damlc_osx",
        "@org_apache_flink_flink_test_utils_2_12",
    ],
)
