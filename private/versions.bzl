_COURSIER_CLI_VERSION = "v2.0.0-RC3-4"

COURSIER_CLI_HTTP_FILE_NAME = ("coursier_cli_" + _COURSIER_CLI_VERSION).replace(".", "_").replace("-", "_")
COURSIER_CLI_GITHUB_ASSET_URL = "https://github.com/coursier/coursier/releases/download/{COURSIER_CLI_VERSION}/coursier.jar".format(COURSIER_CLI_VERSION = _COURSIER_CLI_VERSION)

# Run 'bazel run //:mirror_coursier' to upload a copy of the jar to the Bazel mirror.
COURSIER_CLI_BAZEL_MIRROR_URL = "https://mirror.bazel.build/coursier_cli/" + COURSIER_CLI_HTTP_FILE_NAME + ".jar"
COURSIER_CLI_SHA256 = "8f35f92fb8e021f96b3aa8145c66c3b2e29295baabb28ff50569e613438afcbd"
