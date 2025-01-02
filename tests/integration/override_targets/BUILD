load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@rules_java//java:defs.bzl", "java_library")

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
