load("@aspect_bazel_lib//lib:diff_test.bzl", "diff_test")
load("//:defs.bzl", "artifact", "java_export")

java_export(
    name = "pom-example",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "//tests/integration/maven_bom:one-dep",
    ],
)

diff_test(
    name = "validate-pom",
    file1 = ":pom-example-pom",
    file2 = "pom.golden.xml",
)

java_export(
    name = "pom-example-with-runtime-dep",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    runtime_deps = [
        "//tests/integration/maven_bom:one-dep",
    ],
    deps = [
        "//tests/integration/maven_bom:one-dep",
        artifact("com.google.guava:guava"),
    ],
)

diff_test(
    name = "validate-pom-with-runtime-dep",
    file1 = ":pom-example-with-runtime-dep-pom",
    file2 = "pom-with-runtime-dep.golden.xml",
)

java_export(
    name = "pom-example-with-classifier-dep",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    runtime_deps = [
        "@maven//:io_netty_netty_tcnative_boringssl_static_linux_aarch_64",
    ],
    deps = [
        "@maven//:io_netty_netty_tcnative_boringssl_static_linux_aarch_64",
        "@maven//:io_netty_netty_tcnative_boringssl_static_linux_x86_64",
    ],
)

diff_test(
    name = "validate-pom-example-with-classifier-dep",
    file1 = ":pom-example-with-classifier-dep-pom",
    file2 = "pom-with-classifier-dep.golden.xml",
)

java_export(
    name = "pom-example-with-compile-only-dep",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    # This dep can be any neverlink dep.
    deps = ["@regression_testing_coursier//:com_squareup_javapoet"],
)

diff_test(
    name = "validate-pom-example-with-compile-only-dep",
    file1 = ":pom-example-with-compile-only-dep-pom",
    file2 = "pom-with-compile-only-dep.golden.xml",
)

java_export(
    name = "pom-example-with-exported-dep",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    exports = [
        "//tests/integration/maven_bom:one-dep",
    ],
    deps = [
        "//tests/integration/maven_bom:one-dep",
    ],
)

diff_test(
    name = "validate-pom-example-with-exported-dep",
    file1 = ":pom-example-with-exported-dep-pom",
    file2 = "pom-with-exported-dep.golden.xml",
)

java_export(
    name = "pom-example-with-exported-compile-only-dep",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    exports = [
        "@regression_testing_coursier//:com_squareup_javapoet",
    ],
    deps = ["@regression_testing_coursier//:com_squareup_javapoet"],
)

diff_test(
    name = "validate-pom-example-exported-compile-only-dep",
    file1 = ":pom-example-with-exported-compile-only-dep-pom",
    file2 = "pom-example-with-exported-compile-only-dep.golden.xml",
)

####
# Exclusion Tests
####
java_export(
    name = "pom-example-with-exclusions-coursier",
    srcs = ["PomExample.java"],
    # A java_export specific exclusion
    exclusions = {
        "@pom_exclusion_testing_coursier//:com_google_guava_guava": [
            "com.google.guava:failureaccess",
        ],
    },
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "@pom_exclusion_testing_coursier//:com_google_guava_guava",
    ],
)

diff_test(
    name = "validate-pom-example-with-exclusions-coursier",
    file1 = ":pom-example-with-exclusions-coursier-pom",
    file2 = "pom-with-exclusions.golden.xml",
)

java_export(
    name = "pom-example-with-exclusions-maven",
    srcs = ["PomExample.java"],
    # A java_export specific exclusion
    exclusions = {
        "@pom_exclusion_testing_maven//:com_google_guava_guava": [
            "com.google.guava:failureaccess",
        ],
    },
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "@pom_exclusion_testing_maven//:com_google_guava_guava",
    ],
)

diff_test(
    name = "validate-pom-example-with-exclusions-maven",
    file1 = ":pom-example-with-exclusions-maven-pom",
    file2 = "pom-with-exclusions.golden.xml",
)

java_export(
    name = "pom-example-with-exclusions-gradle",
    srcs = ["PomExample.java"],
    # A java_export specific exclusion
    exclusions = {
        "@pom_exclusion_testing_gradle//:com_google_guava_guava": [
            "com.google.guava:failureaccess",
        ],
    },
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "@pom_exclusion_testing_gradle//:com_google_guava_guava",
    ],
)

diff_test(
    name = "validate-pom-example-with-exclusions-gradle",
    file1 = ":pom-example-with-exclusions-gradle-pom",
    file2 = "pom-with-exclusions.golden.xml",
)

java_export(
    name = "pom-example-with-specific-exclusions",
    srcs = ["PomExample.java"],
    # A java_export specific exclusion
    exclusions = {
        "@maven//:com_google_guava_guava": [
            "com.google.guava:failureaccess",
        ],
    },
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "@maven//:com_google_guava_guava",
    ],
)

diff_test(
    name = "validate-pom-example-with-specific-exclusions",
    file1 = ":pom-example-with-specific-exclusions-pom",
    file2 = "pom-with-specific-exclusions.golden.xml",
)

java_export(
    name = "pom-example-with-maven-defined-exclusions-coursier",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "@pom_exclusion_testing_coursier//:com_google_guava_guava",
    ],
)

diff_test(
    name = "validate-pom-example-with-maven-defined-exclusions-coursier",
    file1 = ":pom-example-with-maven-defined-exclusions-coursier-pom",
    file2 = "pom-with-maven-defined-exclusions.golden.xml",
)

java_export(
    name = "pom-example-with-maven-defined-exclusions-maven",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "@pom_exclusion_testing_maven//:com_google_guava_guava",
    ],
)

diff_test(
    name = "validate-pom-example-with-maven-defined-exclusions-maven",
    file1 = ":pom-example-with-maven-defined-exclusions-maven-pom",
    file2 = "pom-with-maven-defined-exclusions.golden.xml",
)

java_export(
    name = "pom-example-with-maven-defined-exclusions-gradle",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        "@pom_exclusion_testing_gradle//:com_google_guava_guava",
    ],
)

diff_test(
    name = "validate-pom-example-with-maven-defined-exclusions-gradle",
    file1 = ":pom-example-with-maven-defined-exclusions-gradle-pom",
    file2 = "pom-with-maven-defined-exclusions.golden.xml",
)

java_library(
    name = "other-library",
    srcs = ["OtherLibrary.java"],
    deps = [
        "@pom_exclusion_testing_coursier//:com_google_guava_guava",
    ],
)

# This is to ensure that the guava dependency appears in the pom.xml with the correct exclusions,
# even though it is not directly on the java_export, it is on the java_library dep.
java_export(
    name = "pom-transitive-exclusion",
    srcs = ["PomExample.java"],
    maven_coordinates = "com.example:app:1.0.0",
    deps = [
        ":other-library",
    ],
)
