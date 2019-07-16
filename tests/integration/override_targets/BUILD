load("@bazel_skylib//rules:build_test.bzl", "build_test")

build_test(
    name = "override_targets",
    targets = [":app"],
)

android_binary(
    name = "app",
    manifest = "AndroidManifest.xml",
    custom_package = "not.used",
    deps = [
        "@regression_testing//:com_google_ar_sceneform_ux_sceneform_ux"
    ],
)

aar_import(
    name = "sceneform_rendering",
    aar = "@regression_testing//:v1/https/maven.google.com/com/google/ar/sceneform/rendering/1.10.0/rendering-1.10.0.aar",
    deps = [
        # Add the missing dependencies
        "@regression_testing//:com_google_ar_core",
        "@regression_testing//:com_google_ar_sceneform_sceneform_base",
        "@regression_testing//:com_google_ar_sceneform_filament_android",
    ],
    tags = ["maven_coordinates=com.google.ar.sceneform:rendering:aar:1.10.0"],
    visibility = ["@regression_testing//:__subpackages__"]
)