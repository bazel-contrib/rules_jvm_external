load("@bazel_skylib//rules:build_test.bzl", "build_test")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@rules_jvm_external//settings:stamp_manifest.bzl", "stamp_manifest")

build_test(
    name = "artifacts",
    targets = [
        # Test jar that does not have a META-INF/MANIFEST.MF file doesn't error when stamped
        "@manifest_stamp_testing//:javax_inject_javax_inject",
        # Test jar that has duplicate entries in the jar doesn't error when stamped
        "@manifest_stamp_testing//:org_apache_beam_beam_sdks_java_core",
    ],
)

genrule(
    name = "extract_manifest",
    srcs = [
        "@manifest_stamp_testing//:com_google_guava_guava",
    ],
    outs = [
        "guava_MANIFEST.MF",
    ],
    cmd = "$(JAVABASE)/bin/jar xf $< META-INF/MANIFEST.MF && cat META-INF/MANIFEST.MF | grep -E '^Bundle-Version|Target-Label' | sort > $@",
    toolchains = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
)

diff_test(
    name = "diff_manifest_test",
    file1 = select({
        "@bazel_tools//src/conditions:windows": "guava_MANIFEST.MF.golden",
        "//conditions:default": "guava_MANIFEST.MF.golden.unix",
    }),
    file2 = "guava_MANIFEST.MF",
    # TODO(https://github.com/bazelbuild/rules_jvm_external/issues/283): Fix reproducibility issues.
    # This test requires --//settings:stamp_manifest.
    tags = ["manual"],
)

genrule(
    name = "extract_signed_manifest",
    srcs = [
        "@manifest_stamp_testing//:org_bouncycastle_bcprov_jdk15on",
    ],
    outs = [
        "signed_MANIFEST.MF",
    ],
    cmd = "$(JAVABASE)/bin/jar xf $< META-INF/MANIFEST.MF && cat META-INF/MANIFEST.MF | grep -E '^Bundle-Version|Target-Label' | sort > $@",
    toolchains = ["@bazel_tools//tools/jdk:current_host_java_runtime"],
)

# Signed jars don't get a Target-Label because adding contents to the
# MANIFEST.MF file breaks the jar signature.
diff_test(
    name = "diff_signed_manifest_test",
    file1 = select({
        "@bazel_tools//src/conditions:windows": "bcprov_MANIFEST.MF.golden",
        "//conditions:default": "bcprov_MANIFEST.MF.golden.unix",
    }),
    file2 = "signed_MANIFEST.MF",
)
