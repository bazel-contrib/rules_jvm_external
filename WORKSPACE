android_sdk_repository(name = 'androidsdk')
# local_repository(name = 'gmaven_rules', path = '.')
# load('@gmaven_rules//:gmaven.bzl', 'gmaven_rules')
# gmaven_rules()

load("//:import_external.bzl", "java_import_external", "aar_import_external")

java_import_external(
    name = "org_hamcrest_core",
    jar_sha256 = "66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9",
    jar_urls = [
        "https://mirror.bazel.build/repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
        "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
        "http://maven.ibiblio.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar",
    ],
    licenses = ["notice"],  # BSD
    testonly_ = 1,
)

aar_import_external(
    name = "com_android_support_test_espresso_espresso_core_3_0_1",
    aar_urls = [
        "https://dl.google.com/dl/android/maven2/com/android/support/test/espresso/espresso-core/3.0.1/espresso-core-3.0.1.aar",
    ],
    aar_sha256 = "dbca1a46db203a7ef12aa7cea37e5d5345900f83401b32d5f60f220991290948",
    licenses = ["notice"],  # Apache
    deps = [
        # GMaven does not have org_hamcrest_hamcrest_integration_1_3
        '@com_android_support_test_runner_1_0_1//aar',
        # GMaven does not have com_google_code_findbugs_jsr305_2_0_1
        # GMaven does not have javax_inject_javax_inject_1
        '@com_android_support_test_rules_1_0_1//aar',
        # GMaven does not have com_squareup_javawriter_2_1_1
        # GMaven does not have org_hamcrest_hamcrest_library_1_3
        # '@com_android_support_test_espresso_espresso_idling_resource_3_0_1//aar',
    ],
)

aar_import_external(
    name = "com_android_support_test_rules_1_0_1",
    aar_urls = [
        "https://dl.google.com/dl/android/maven2/com/android/support/test/rules/1.0.1/rules-1.0.1.aar",
    ],
    aar_sha256 = "7ca0f88390c6472177c576355955b63dc64c990405a946fd1e316f6fce233434",
    licenses = ["notice"],  # Apache
)

aar_import_external(
    name = "com_android_support_test_runner_1_0_1",
    aar_urls = [
        "https://dl.google.com/dl/android/maven2/com/android/support/test/runner/1.0.1/runner-1.0.1.aar",
    ],
    aar_sha256 = "c773e2cecbb0a86351f284c91620e7badeac5413da5a95e86f243d411774c42d",
    licenses = ["notice"],  # Apache
)
