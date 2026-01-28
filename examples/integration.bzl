"""Macros for managing the integration test framework for examples."""

load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load(
    "@rules_bazel_integration_test//bazel_integration_test:defs.bzl",
    "bazel_integration_test",
    "default_test_runner",
)

def derive_example_metadata(directory):
    """Derive metadata for an example directory.

    Args:
        directory: The example directory name.

    Returns:
        A struct with workspace_files and has_module fields.
    """
    return struct(
        directory = directory,
        # Include example files + root-level files needed for local_path_override(path = "../../")
        workspace_files = native.glob(
            ["%s/**/**" % directory],
            # exclude any bazel directories if existing
            exclude = ["%s/bazel-*/**" % directory],
        ) + ["//:local_repository_files"],
        exclude = [
            # Version exclusions - create a file like `exclude/8.x` to skip that version
            version.rpartition("/")[2]
            for version in native.glob(
                ["%s/exclude/*" % directory],
                allow_empty = True,
            )
        ],
        only = [
            # Version inclusions - create a file like `only/8.x` to only run that version
            version.rpartition("/")[2]
            for version in native.glob(
                ["%s/only/*" % directory],
                allow_empty = True,
            )
        ],
        has_module = len(native.glob(
            ["%s/MODULE.bazel" % directory, "%s/MODULE" % directory],
            allow_empty = True,
        )) > 0,
    )

def example_integration_test_suite(name, metadata, tags = []):
    """Create integration tests for an example across all Bazel versions.

    Only bzlmod is supported (no WORKSPACE mode).

    Args:
        name: The name of the example/test suite.
        metadata: The metadata struct from derive_example_metadata.
        tags: Additional tags for the tests.
    """
    if not metadata.has_module:
        # Skip examples without MODULE.bazel (bzlmod only)
        return

    for version in bazel_binaries.versions.all:
        if version in metadata.only or (not metadata.only and version not in metadata.exclude):
            clean_bazel_version = Label(version).name

            test_runner_name = "%s_%s_test_runner" % (name, clean_bazel_version)
            default_test_runner(
                name = test_runner_name,
                bazel_cmds = [
                    "info",
                    "build //...",
                ],
            )

            bazel_integration_test(
                name = "%s_%s_test" % (name, clean_bazel_version),
                timeout = "eternal",
                bazel_version = version,
                tags = tags + [clean_bazel_version, name, "bzlmod", "examples"],
                test_runner = test_runner_name,
                workspace_files = metadata.workspace_files,
                workspace_path = metadata.directory,
            )

    native.test_suite(
        name = name,
        tags = [name, "-manual"],
    )
