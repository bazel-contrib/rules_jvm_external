#
# Utilities for working with artifacts
#

load("//:specs.bzl", "utils")

def deduplicate_and_sort_artifacts(dep_tree, artifacts, verbose):
    # The deps json returned from coursier can have duplicate artifacts with
    # different dependencies and exclusions. We want to de-duplicate the
    # artifacts and not choose ones with empty dependencies if possible.
    # We will ignore the exclusions from the resolver and rely on the user-specified
    # exclusions from the maven_install declaration.

    # As we de-duplicate prefer the duplicates with non-empty dependency lists
    deduped_artifacts = {}
    null_artifacts = []
    for artifact in dep_tree["dependencies"]:
        # Coursier expands the exclusions on an artifact to all of its dependencies.
        # This is too broad, so we set them to empty and append the exclusion map
        # to the dep_tree using the user-defined exclusions.
        artifact["exclusions"] = []
        if artifact["file"] == None:
            null_artifacts.append(artifact)
            continue
        elif artifact["file"] in deduped_artifacts:
            if len(artifact["dependencies"]) > 0 and len(deduped_artifacts[artifact["file"]]["dependencies"]) == 0:
                deduped_artifacts[artifact["file"]] = artifact
        else:
            deduped_artifacts[artifact["file"]] = artifact

    sorted_deduped_values = []
    for key in sorted(deduped_artifacts.keys()):
        sorted_deduped_values.append(deduped_artifacts[key])

    dep_tree.update({"dependencies": sorted_deduped_values + null_artifacts})

    return dep_tree
