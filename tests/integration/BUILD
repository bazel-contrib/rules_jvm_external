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
)
