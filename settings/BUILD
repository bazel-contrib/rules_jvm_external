load("//settings:stamp_manifest.bzl", "stamp_manifest")
load("//settings:extract_proguard_config.bzl", "extract_proguard_config")

package(
    default_visibility = ["//visibility:public"],
)

exports_files([
    "extract_proguard_config.bzl",
    "stamp_manifest.bzl",
])

stamp_manifest(
    name = "stamp_manifest",
    build_setting_default = True,
)

extract_proguard_config(
    name = "extract_proguard_config",
    build_setting_default = False,
)
