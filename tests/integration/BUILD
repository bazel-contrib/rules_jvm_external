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
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_hamcrest_hamcrest_core",
        "@regression_testing//:com_squareup_javapoet",
    ],
)

java_test(
    name = "UnsafeSharedCacheTest",
    srcs = ["UnsafeSharedCacheTest.java"],
    test_class = "com.jvm.external.UnsafeSharedCacheTest",
    deps = [
        "@maven//:org_hamcrest_hamcrest",
        "@maven//:org_hamcrest_hamcrest_core",
        "@unsafe_shared_cache//:com_google_guava_guava",
    ],
)
