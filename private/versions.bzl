_COURSIER_CLI_VERSION = "v2.0.0-RC5-3"

COURSIER_CLI_HTTP_FILE_NAME = ("coursier_cli_" + _COURSIER_CLI_VERSION).replace(".", "_").replace("-", "_")
COURSIER_CLI_GITHUB_ASSET_URL = "https://github.com/coursier/coursier/releases/download/{COURSIER_CLI_VERSION}/coursier.jar".format(COURSIER_CLI_VERSION = _COURSIER_CLI_VERSION)

# Run 'bazel run //:mirror_coursier' to upload a copy of the jar to the Bazel mirror.
COURSIER_CLI_BAZEL_MIRROR_URL = "https://mirror.bazel.build/coursier_cli/" + COURSIER_CLI_HTTP_FILE_NAME + ".jar"
COURSIER_CLI_SHA256 = "6598d9277705ad8369a4f9c64217fbc31c19234f2cbcca9b1e5c4300a3abb317"

JQ_VERSIONS = [
    struct(
        os = "linux",
        url = "https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64",
        sha256 = "af986793a515d500ab2d35f8d2aecd656e764504b789b66d7e1a0b727a124c44",
    ),
    struct(
        os = "osx",
        url = "https://github.com/stedolan/jq/releases/download/jq-1.6/jq-osx-amd64",
        sha256 = "5c0a0a3ea600f302ee458b30317425dd9632d1ad8882259fcaf4e9b868b2b1ef",
    ),
    struct(
        os = "windows",
        url = "https://github.com/stedolan/jq/releases/download/jq-1.6/jq-win64.exe",
        sha256 = "a51d36968dcbdeabb3142c6f5cf9b401a65dc3a095f3144bd0c118d5bb192753",
    ),
]
