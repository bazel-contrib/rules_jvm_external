_COURSIER_CLI_VERSION = "v2.1.0-M7"

COURSIER_CLI_HTTP_FILE_NAME = ("coursier_cli_" + _COURSIER_CLI_VERSION).replace(".", "_").replace("-", "_")
COURSIER_CLI_GITHUB_ASSET_URL = "https://github.com/coursier/coursier/releases/download/{COURSIER_CLI_VERSION}/coursier.jar".format(COURSIER_CLI_VERSION = _COURSIER_CLI_VERSION)

# Run 'bazel run //:mirror_coursier' to upload a copy of the jar to the Bazel mirror.
# COURSIER_CLI_BAZEL_MIRROR_URL = "https://mirror.bazel.build/coursier_cli/" + COURSIER_CLI_HTTP_FILE_NAME + ".jar"
COURSIER_CLI_BAZEL_MIRROR_URL = "https://repo1.maven.org/maven2/io/get-coursier/coursier-cli_2.12/2.1.0-M7/" + COURSIER_CLI_HTTP_FILE_NAME + ".jar"
COURSIER_CLI_SHA256 = "37e4b11139a1c547d1a564d1d169c9caf781260bc6563599e7aaf8d9a7ad934d"

JQ_VERSIONS = {
    "linux": struct(
        url = "https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64",
        sha256 = "af986793a515d500ab2d35f8d2aecd656e764504b789b66d7e1a0b727a124c44",
    ),
    "macos": struct(
        url = "https://github.com/stedolan/jq/releases/download/jq-1.6/jq-osx-amd64",
        sha256 = "5c0a0a3ea600f302ee458b30317425dd9632d1ad8882259fcaf4e9b868b2b1ef",
    ),
    "windows": struct(
        url = "https://github.com/stedolan/jq/releases/download/jq-1.6/jq-win64.exe",
        sha256 = "a51d36968dcbdeabb3142c6f5cf9b401a65dc3a095f3144bd0c118d5bb192753",
    ),
}
