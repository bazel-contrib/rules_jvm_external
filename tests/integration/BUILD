load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

genquery(
    name = "testonly-deps",
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
        "@testonly_testing//:pin",
    ],
    testonly = True,
)

diff_test(
    name = "testonly_artifacts_test",
    file1 = select({
        "@bazel_tools//src/conditions:windows": "testonly-deps.golden",
        "//conditions:default": "testonly-deps.golden.unix",
    }),
    file2 = ":testonly-deps",
)

java_test(
    name = "ArtifactExclusionsTest",
    srcs = ["ArtifactExclusionsTest.java"],
    test_class = "com.jvm.external.ArtifactExclusionsTest",
    deps = [
        "@exclusion_testing//:com_google_guava_guava",
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_hamcrest_hamcrest_core",
    ],
)

java_test(
    name = "GlobalArtifactExclusionsTest",
    srcs = ["GlobalArtifactExclusionsTest.java"],
    test_class = "com.jvm.external.GlobalArtifactExclusionsTest",
    deps = [
        "@global_exclusion_testing//:com_diffplug_durian_durian_core",
        "@global_exclusion_testing//:com_google_guava_guava",
        "@global_exclusion_testing//:com_squareup_okhttp3_okhttp",
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_hamcrest_hamcrest_core",
    ],
)

java_test(
    name = "NeverlinkTest",
    srcs = ["NeverlinkTest.java"],
    test_class = "com.jvm.external.NeverlinkTest",
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:org_hamcrest_hamcrest",
    ],
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
)

java_test(
    name = "UnsafeSharedCacheTest",
    srcs = ["UnsafeSharedCacheTest.java"],
    test_class = "com.jvm.external.UnsafeSharedCacheTest",
    deps = [
        ":guava_import",
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_hamcrest_hamcrest_core",
    ],
    tags = ["manual"],
)

java_test(
    name = "JsonArtifactsTest",
    srcs = ["JsonArtifactsTest.java"],
    test_class = "com.jvm.external.JsonArtifactsTest",
    deps = [
        "@json_artifacts_testing//:org_json_json",
        "@json_artifacts_testing//:io_quarkus_quarkus_maven_plugin",
    ],
)
