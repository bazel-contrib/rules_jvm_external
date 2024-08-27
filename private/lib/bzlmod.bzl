# Utility functions for handling bzlmod-related features

def to_visible_name(name):
    """Convert a name (typically from `Label.workspace_name`) to the human-readable version.

    `bzlmod` will mangle the names of repos, but it's often useful to have the human-readable
    version of the name available.

    Args:
      name: (string) The name to convert, often this will be from `Label.workspace_name`
    """

    if "+" in name:
        return name.rpartition("+")[-1]

    if "~" in name:
        return name.rpartition("~")[-1]

    return name

def get_file_owner_repo_name(name):
    """Convert a name (typically from `File.owner.workspace_name`) to the owning repo

    Args:
      name: (string) The name to convert, often this will be from `File.owner.workspace_name`
    """

    # Common case first
    if "+" in name:
        return name.partition("+")[0]

    return name.partition("~")[0]
