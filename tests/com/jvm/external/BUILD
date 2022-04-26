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
    name = "JsonArtifactsTest",
    srcs = ["JsonArtifactsTest.java"],
    test_class = "com.jvm.external.JsonArtifactsTest",
    deps = [
        "@json_artifacts_testing//:io_quarkus_quarkus_maven_plugin",
        "@json_artifacts_testing//:org_json_json",
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

java_test(
    name = "UnsafeSharedCacheTest",
    srcs = ["UnsafeSharedCacheTest.java"],
    tags = ["manual"],
    test_class = "com.jvm.external.UnsafeSharedCacheTest",
    deps = [
        "//tests/integration:guava_import",
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_hamcrest_hamcrest_core",
    ],
)
