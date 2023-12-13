load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_android//android:rules.bzl", "aar_import")

build_test(
    name = "override_targets",
    targets = [":app"],
    tags = ["manual"], # https://github.com/bazelbuild/rules_jvm_external/issues/978
)

android_binary(
    name = "app",
    custom_package = "not.used",
    manifest = "AndroidManifest.xml",
    deps = [
        "@regression_testing//:com_google_ar_sceneform_ux_sceneform_ux",
    ],
    tags = ["manual"], # https://github.com/bazelbuild/rules_jvm_external/issues/978
)

# rendering-1.10.0.aar is known to have an incomplete list of dependencies in
# its POM file. We manually replace the generated target here to work around
# this issue. See https://github.com/bazelbuild/rules_jvm_external/issues/199
# for more information.
aar_import(
    name = "sceneform_rendering",
    aar = "@com.google.ar.sceneform_rendering//file",
    tags = ["maven_coordinates=com.google.ar.sceneform:rendering:aar:1.10.0", "manual"],
    visibility = ["@regression_testing//:__subpackages__"],
    deps = [
        # Add the missing dependencies
        "@regression_testing//:com_google_ar_core",
        "@regression_testing//:com_google_ar_sceneform_sceneform_base",
        "@regression_testing//:com_google_ar_sceneform_filament_android",
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
    scope = ["@override_target_in_deps//:io_opentelemetry_opentelemetry_sdk"],
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
