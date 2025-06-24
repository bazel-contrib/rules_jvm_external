load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@rules_android//android:rules.bzl", "aar_import", "android_binary")
load("@rules_java//java:defs.bzl", "java_library")
load("//tests/integration:is_bzlmod_enabled.bzl", "is_bzlmod_enabled")

build_test(
    name = "override_targets",
    tags = ["manual"],  # https://github.com/bazelbuild/rules_jvm_external/issues/978
    targets = [":app"],
)

android_binary(
    name = "app",
    custom_package = "not.used",
    manifest = "AndroidManifest.xml",
    tags = ["manual"],  # https://github.com/bazelbuild/rules_jvm_external/issues/978
    deps = [
        "@regression_testing_coursier//:com_google_ar_sceneform_ux_sceneform_ux",
    ],
)

# rendering-1.10.0.aar is known to have an incomplete list of dependencies in
# its POM file. We manually replace the generated target here to work around
# this issue. See https://github.com/bazelbuild/rules_jvm_external/issues/199
# for more information.
aar_import(
    name = "sceneform_rendering",
    aar = "@com.google.ar.sceneform_rendering//file",
    tags = [
        "manual",
        "maven_coordinates=com.google.ar.sceneform:rendering:aar:1.10.0",
    ],
    visibility = ["@regression_testing_coursier//:__subpackages__"],
    deps = [
        # Add the missing dependencies
        "@regression_testing_coursier//:com_google_ar_core",
        "@regression_testing_coursier//:com_google_ar_sceneform_sceneform_base",
        "@regression_testing_coursier//:com_google_ar_sceneform_filament_android",
    ],
)

java_library(
    name = "additional_deps",
    visibility = ["//visibility:public"],
    exports = [
        "@override_target_in_deps//:original_io_opentelemetry_opentelemetry_api",
    ],
    runtime_deps = [
        "@override_target_in_deps//:redis_clients_jedis",
    ],
)

genquery(
    name = "trace_otel_deps",
    expression = "deps(@override_target_in_deps//:io_opentelemetry_opentelemetry_sdk)",
    opts = [
        "--nohost_deps",
        "--noimplicit_deps",
    ],
    scope = ["@override_target_in_deps//:io_opentelemetry_opentelemetry_sdk"],
)

genrule(
    name = "override_contains_additional_deps_sorted",
    testonly = 1,
    srcs = [":trace_otel_deps"],
    outs = ["override_contains_additional_deps_sorted.txt"],
    cmd = "cat $< | sed -e 's|^@@|@|g; s|\r||g' | sed -e 's|^@[^/]*[+~]|@|g; s|\r||g' | sort > $@",
)

sh_test(
    name = "override_contains_additional_deps",
    srcs = ["override_contains_additional_deps.sh"],
    data = [
        ":trace_otel_deps",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)

genquery(
    name = "root_module_can_override",
    expression = "deps(@root_module_can_override//:com_squareup_okhttp3_okhttp)",
    opts = [
        "--nohost_deps",
        "--noimplicit_deps",
    ],
    scope = ["@root_module_can_override//:com_squareup_okhttp3_okhttp"],
)

genrule(
    name = "root_module_can_override_sorted",
    testonly = 1,
    srcs = [":root_module_can_override"],
    outs = ["root_module_can_override_sorted.txt"],
    cmd = "cat $< | sed -e 's|^@@|@|g; s|\r||g' | sed -e 's|^@[^/]*[+~]|@|g; s|\r||g' | sort > $@",
)

diff_test(
    name = "root_module_can_override_test",
    file1 = ":root_module_can_override.golden",
    file2 = ":root_module_can_override_sorted.txt",
    # This test only makes sense if we're running with `bzlmod` enabled
    tags = [] if is_bzlmod_enabled() else ["manual"],
)
