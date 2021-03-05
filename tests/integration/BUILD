load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

genquery(
    name = "testonly-deps",
    testonly = True,
    expression = "attr(testonly, 1, deps(@testonly_testing//:all, 0))",
    scope = [
        "@testonly_testing//:org_codehaus_mojo_animal_sniffer_annotations_1_17",
        "@testonly_testing//:org_checkerframework_checker_qual_2_5_2",
        "@testonly_testing//:com_google_j2objc_j2objc_annotations_1_1",
        "@testonly_testing//:com_google_guava_listenablefuture_9999_0_empty_to_avoid_conflict_with_guava",
        "@testonly_testing//:com_google_guava_guava_27_0_jre",
        "@testonly_testing//:com_google_guava_guava",
        "@testonly_testing//:org_codehaus_mojo_animal_sniffer_annotations",
        "@testonly_testing//:org_checkerframework_checker_qual",
        "@testonly_testing//:com_google_j2objc_j2objc_annotations",
        "@testonly_testing//:com_google_guava_listenablefuture",
        "@testonly_testing//:com_google_guava_failureaccess_1_0",
        "@testonly_testing//:com_google_guava_failureaccess",
        "@testonly_testing//:com_google_errorprone_error_prone_annotations_2_2_0",
        "@testonly_testing//:com_google_errorprone_error_prone_annotations",
        "@testonly_testing//:com_google_code_findbugs_jsr305_3_0_2",
        "@testonly_testing//:com_google_code_findbugs_jsr305",
        "@testonly_testing//:com_google_auto_value_auto_value_annotations_1_6_3",
        "@testonly_testing//:com_google_auto_value_auto_value_annotations",
        "@testonly_testing//:outdated",
        "@testonly_testing//:pin",
    ],
)

genrule(
    name = "testonly-deps-sorted",
    srcs = [":testonly-deps"],
    outs = ["testonly-deps-sorted.txt"],
    cmd = "cat $< | sort > $@",
    testonly = 1,
)

diff_test(
    name = "testonly_artifacts_test",
    file1 = select({
        "@bazel_tools//src/conditions:windows": "testonly-deps.golden",
        "//conditions:default": "testonly-deps.golden.unix",
    }),
    file2 = ":testonly-deps-sorted",
)

# https://github.com/coursier/coursier/issues/1792
# https://github.com/bazelbuild/rules_jvm_external/issues/433
genquery(
    name = "version_interval_deps",
    testonly = True,
    expression = "deps(@version_interval_testing//:io_grpc_grpc_netty_shaded, 2)",
    opts = [
        "--nohost_deps",
        "--noimplicit_deps",
    ],
    scope = ["@version_interval_testing//:io_grpc_grpc_netty_shaded"],
)

genrule(
    name = "version-interval-deps-sorted",
    srcs = [":version_interval_deps"],
    outs = ["version-interval-deps-sorted.txt"],
    cmd = "cat $< | sort > $@",
    testonly = 1,
)

diff_test(
    name = "version_interval_deps_test",
    file1 = select({
        "@bazel_tools//src/conditions:windows": "version-interval-deps.golden.dos",
        "//conditions:default": "version-interval-deps.golden.unix",
    }),
    file2 = ":version-interval-deps-sorted.txt",
)

java_import(
    name = "guava_import",
    jars = [
        # The default @maven//:com_google_guava_guava is a stamped jar created
        # from the actual symlinked jar. Instead of referencing the stamped jar,
        # which isn't part of the unsafe shared cache, import the symlink file label
        # here directly, and bypass the stamping in jvm_import.bzl.
        "@unsafe_shared_cache//:v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
    ],
    visibility = [
        "//tests/com/jvm/external:__pkg__",
    ],
)
