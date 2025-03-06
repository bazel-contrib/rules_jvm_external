def read_artifacts_from_file(mctx, artifacts_file_label):
    """Reads a file with a list of artifacts and returns a list of artifacts.

    Args:
        mctx: The module context.
        artifacts_file_label: The label of the file with the list of artifacts.

    Returns:
        A list of artifacts.
    """
    if not artifacts_file_label:
        return []

    artifacts_file_path = mctx.path(artifacts_file_label)
    artifacts_file_content = mctx.read(artifacts_file_path)

    artifacts = []
    variables = {}

    for line in artifacts_file_content.splitlines():
        line = line.split("#", 1)[0].strip()
        if not line:
            continue

        # variable assignment and substitution
        if "=" in line:
            key, value = line.split("=", 1)
            variables[key.strip()] = value.strip()
        else:
            for var, val in variables.items():
                line = line.replace("${" + var + "}", val)  # Perform substitution
            artifacts.append(line)

    return artifacts
