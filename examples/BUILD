load(":integration.bzl", "derive_example_metadata", "example_integration_test_suite")

# Number of shards for parallel CI execution
SHARD_COUNT = 3

# Generate integration tests for all examples
[
    example_integration_test_suite(
        name = example,
        metadata = metadata,
        tags = ["shard_%s" % (idx % SHARD_COUNT)],
    )
    for (
        idx,
        (example, metadata),
    ) in enumerate({
        example: derive_example_metadata(
            directory = example,
        )
        for example in {
            # Extract directory names from files, de-duplicate via dict
            file.partition("/")[0]: True
            for file in glob(
                ["**/*"],
                # Exclude files directly in examples/ and specific problematic examples
                exclude = [
                    "*",  # Exclude files directly in examples/
                ],
            )
        }
    }.items())
]

# Test suites for running all examples
test_suite(
    name = "all_examples",
    tags = ["examples", "-manual"],
)

# Per-version test suites
test_suite(
    name = "all_examples_8_x",
    tags = ["examples", "8.x", "-manual"],
)

test_suite(
    name = "all_examples_9_x",
    tags = ["examples", ".bazelversion", "-manual"],
)

# Shard-based test suites for CI
[
    test_suite(
        name = "shard_%s" % shard,
        tags = ["shard_%s" % shard, "-manual"],
    )
    for shard in range(SHARD_COUNT)
]
