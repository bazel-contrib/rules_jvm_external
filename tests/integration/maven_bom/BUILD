load("@aspect_bazel_lib//lib:diff_test.bzl", "diff_test")
load("@bazel_skylib//rules:common_settings.bzl", "string_flag")
load("//:defs.bzl", "artifact", "java_export", "maven_bom")

maven_bom(
    name = "bom",
    dependencies_maven_coordinates = "com.example:bom-deps:1.0.0",
    java_exports = [
        ":one-dep",
        ":two-deps",
    ],
    maven_coordinates = "com.example:bom:1.0.0",
)

diff_test(
    name = "validate-bom",
    file1 = ":bom",
    file2 = "bom.golden.xml",
)

diff_test(
    name = "validate-dependencies-bom",
    file1 = ":bom-dependencies",
    file2 = "bom-dependencies.golden.xml",
)

java_export(
    name = "two-deps",
    srcs = ["TwoDeps.java"],
    maven_coordinates = "com.example:two-deps:1.0.0",
    deps = [
        artifact("com.google.guava:guava"),
        artifact("org.hamcrest:hamcrest"),
    ],
)

java_export(
    name = "one-dep",
    srcs = ["OneDep.java"],
    maven_coordinates = "com.example:one-dep:1.0.0",
    visibility = [
        ":__pkg__",
        "//tests/integration/pom_file:__pkg__",
    ],
    deps = [
        artifact("com.google.guava:guava"),
    ],
)

maven_bom(
    name = "transitive-bom",
    dependencies_maven_coordinates = "com.example:bom-transitive-deps:1.0.0",
    java_exports = [
        # This dep depends on `:one-dep` and `:two-dep`, but we include `:one-dep`
        # here. That means that we expect the BOM to include the two deps listed
        # here, and the dependencies-bom should contain `:two-dep` plus guava
        ":transitive-dep",
        ":one-dep",
        # But not the second!
    ],
    maven_coordinates = "com.example:bom-transitive:1.0.0",
)

java_export(
    name = "transitive-dep",
    srcs = ["TransitiveDep.java"],
    maven_coordinates = "com.example:transitive:1.0.0",
    deps = [
        ":one-dep",
        ":two-deps",
    ],
)

diff_test(
    name = "validate-transitive-bom",
    file1 = ":transitive-bom",
    file2 = "transitive-bom.golden.xml",
)

diff_test(
    name = "validate-transitive-dependencies-bom",
    file1 = ":transitive-bom-dependencies",
    file2 = "transitive-bom-dependencies.golden.xml",
)

string_flag(
    name = "version",
    build_setting_default = "0.0.0-dev",
    make_variable = "VERSION",
)

maven_bom(
    name = "substitution-bom",
    java_exports = [":one-dep"],
    maven_coordinates = "com.example:bom-substitution:$(VERSION)",
    dependencies_maven_coordinates = "com.example:bom-substitution-deps:$(VERSION)",
    toolchains = [":version"],
)

diff_test(
    name = "validate-substitution-bom",
    file1 = ":substitution-bom",
    file2 = "substitution-bom.golden.xml",
)

diff_test(
    name = "validate-substitution-dependencies-bom",
    file1 = ":substitution-bom-dependencies",
    file2 = "substitution-bom-dependencies.golden.xml",
)

# Test that exclusions from maven artifacts are included in the dependencies BOM
java_export(
    name = "with-exclusion-dep",
    srcs = ["OneDep.java"],
    maven_coordinates = "com.example:with-exclusion:1.0.0",
    deps = [
        # This artifact has an exclusion for com.google.errorprone:error_prone_annotations
        # defined in MODULE.bazel via dev_maven.artifact
        "@pom_exclusion_testing_coursier//:com_google_guava_guava",
    ],
)

maven_bom(
    name = "exclusion-bom",
    dependencies_maven_coordinates = "com.example:exclusion-bom-deps:1.0.0",
    java_exports = [
        ":with-exclusion-dep",
    ],
    maven_coordinates = "com.example:exclusion-bom:1.0.0",
)

diff_test(
    name = "validate-exclusion-bom",
    file1 = ":exclusion-bom",
    file2 = "exclusion-bom.golden.xml",
)

diff_test(
    name = "validate-exclusion-dependencies-bom",
    file1 = ":exclusion-bom-dependencies",
    file2 = "exclusion-bom-dependencies.golden.xml",
)

# Test that $(VERSION) substitution works in transitive java_export coordinates
java_export(
    name = "versioned-lib",
    srcs = ["OneDep.java"],
    maven_coordinates = "com.example:versioned-lib:$(VERSION)",
    toolchains = [":version"],
    deps = [
        artifact("com.google.guava:guava"),
    ],
)

java_export(
    name = "depends-on-versioned",
    srcs = ["TwoDeps.java"],
    maven_coordinates = "com.example:depends-on-versioned:1.0.0",
    toolchains = [":version"],
    deps = [
        ":versioned-lib",
    ],
)

maven_bom(
    name = "versioned-transitive-bom",
    dependencies_maven_coordinates = "com.example:versioned-transitive-deps:$(VERSION)",
    java_exports = [
        ":depends-on-versioned",
    ],
    maven_coordinates = "com.example:versioned-transitive-bom:$(VERSION)",
    toolchains = [":version"],
)

diff_test(
    name = "validate-versioned-transitive-bom",
    file1 = ":versioned-transitive-bom",
    file2 = "versioned-transitive-bom.golden.xml",
)

diff_test(
    name = "validate-versioned-transitive-dependencies-bom",
    file1 = ":versioned-transitive-bom-dependencies",
    file2 = "versioned-transitive-bom-dependencies.golden.xml",
)

# Test that exclusions set via java_export's exclusions attribute propagate to the BOM
# (as opposed to exclusions from maven_exclusion= tags on maven artifacts)
java_export(
    name = "with-java-export-exclusion",
    srcs = ["OneDep.java"],
    maven_coordinates = "com.example:with-java-export-exclusion:1.0.0",
    # This is a java_export-level exclusion, not a maven artifact exclusion
    exclusions = {
        artifact("com.google.guava:guava"): [
            "com.google.guava:failureaccess",
        ],
    },
    deps = [
        artifact("com.google.guava:guava"),
    ],
)

maven_bom(
    name = "java-export-exclusion-bom",
    dependencies_maven_coordinates = "com.example:java-export-exclusion-bom-deps:1.0.0",
    java_exports = [
        ":with-java-export-exclusion",
    ],
    maven_coordinates = "com.example:java-export-exclusion-bom:1.0.0",
)

diff_test(
    name = "validate-java-export-exclusion-bom",
    file1 = ":java-export-exclusion-bom",
    file2 = "java-export-exclusion-bom.golden.xml",
)

diff_test(
    name = "validate-java-export-exclusion-dependencies-bom",
    file1 = ":java-export-exclusion-bom-dependencies",
    file2 = "java-export-exclusion-bom-dependencies.golden.xml",
)
