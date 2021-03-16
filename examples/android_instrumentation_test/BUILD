# Platform configuration.
platform(
    name = "android_platform",
    constraint_values = [
        "@bazel_tools//platforms:x86_64",
        "@bazel_tools//platforms:linux",
        "@bazel_toolchains//constraints:xenial",
        "@bazel_tools//tools/cpp:clang",
    ],
    # TODO(@jin): s/di-cloud-exp/rbe-containers/ when the official android-test container with libxcursor1 and libxcomposite1 is ready.
    # URI for official container: docker://gcr.io/rbe-containers/ubuntu16_04-android_test@sha256:<sha256>
    remote_execution_properties = """
        properties: {
            name: "container-image"
            value: "docker://gcr.io/di-cloud-exp/rbe-ubuntu16-04-android@sha256:eb3828f71faf595f44b20b97d205e73e8a21982f1d7a170c3ec8f9d33ce3179a"
        }
        properties: {
            name: "dockerNetwork"
            value: "standard"
        }
        properties: {
            name: "dockerPrivileged"
            value: "true"
        }
        properties: {
            name: "gceMachineType"
            value: "n1-standard-2"
        }
""",
)
