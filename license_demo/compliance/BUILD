# Organization local compliance rules go here

load("@rules_license//rules:license.bzl", "license")

package(
    default_applicable_licenses = [":license"],
    default_visibility = ["//visibility:public"],
)

license(
    name = "apache",
    license_kinds = [
        "@rules_license//licenses/spdx:Apache-2.0"
    ],
    license_text = "LICENSE_apache.txt",
)
