load("@rules_jvm_external//:defs.bzl", "artifact")

java_test(
    name = "ArtifactExclusionsTest",
    srcs = ["ArtifactExclusionsTest.java"],
    test_class = "com.jvm.external.ArtifactExclusionsTest",
    deps = [
        "@regression_testing//:com_google_guava_guava_27_0_jre",
        "@maven//:org_hamcrest_hamcrest_core",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

java_test(
    name = "NeverlinkTest",
    srcs = ["NeverlinkTest.java"],
    test_class = "com.jvm.external.NeverlinkTest",
    deps = [
        "@regression_testing//:com_squareup_javapoet",
        "@maven//:org_hamcrest_hamcrest_core",
        "@maven//:org_hamcrest_hamcrest",

    ],
)

java_test(
    name = "UnsafeSharedCacheTest",
    srcs = ["UnsafeSharedCacheTest.java"],
    test_class = "com.jvm.external.UnsafeSharedCacheTest",
    deps = [
        "@unsafe_shared_cache//:com_google_guava_guava_27_0_jre",
        "@maven//:org_hamcrest_hamcrest_core",
        "@maven//:org_hamcrest_hamcrest",
    ],
)
