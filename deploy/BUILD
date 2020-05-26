# tools/maven/BUILD

load("//deploy:maven_version.bzl", "maven_version")

package(default_visibility = ["//visibility:public"])

exports_files(["pom.xml.template"])

maven_version(
    name = "maven_version",
    build_setting_default = "SNAPSHOT",
)
