_COURSIER_CLI_VERSION = "v2.0.0-RC5-3"

COURSIER_CLI_HTTP_FILE_NAME = ("coursier_cli_" + _COURSIER_CLI_VERSION).replace(".", "_").replace("-", "_")
COURSIER_CLI_GITHUB_ASSET_URL = "https://github.com/coursier/coursier/releases/download/{COURSIER_CLI_VERSION}/coursier.jar".format(COURSIER_CLI_VERSION = _COURSIER_CLI_VERSION)

# Run 'bazel run //:mirror_coursier' to upload a copy of the jar to the Bazel mirror.
COURSIER_CLI_BAZEL_MIRROR_URL = "https://mirror.bazel.build/coursier_cli/" + COURSIER_CLI_HTTP_FILE_NAME + ".jar"
COURSIER_CLI_SHA256 = "6598d9277705ad8369a4f9c64217fbc31c19234f2cbcca9b1e5c4300a3abb317"
