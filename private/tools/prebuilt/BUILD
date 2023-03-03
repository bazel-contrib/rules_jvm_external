# This package contains static executables used by the maven_install repository rule.
# To update the prebuilt binaries, `bazel run //scripts:refresh-prebuilts`

exports_files([
    "hasher_deploy.jar",  # built from //private/tools/java/com/github/bazelbuild/rules_jvm_external:hasher-tool_deploy.jar
    "list_packages_deploy.jar",  # built from //private/tools/java/com/github/bazelbuild/rules_jvm_external/jar:ListPackages_deploy.jar
    "lock_file_converter_deploy.jar",  # built from //private/tools/java/com/github/bazelbuild/rules_jvm_external/coursier:LockFileConverter_deploy.jar
    "outdated_deploy.jar",  # built from //private/tools/java/com/github/bazelbuild/rules_jvm_external/maven:outdated_deploy.jar
])
