#
# Utilities for working with artifacts
#

load("//:specs.bzl", "utils")

def deduplicate_and_sort_artifacts(dep_tree, artifacts, excluded_artifacts, verbose):
    # The deps json returned from coursier can have duplicate artifacts with
    # different dependencies and exclusions. We want to de-duplicate the
    # artifacts, match the exclusions specified in the maven_install declaration
    # and not choose ones with empty dependencies if possible

    # First we find all of the artifacts that have user-defined exclusions.
    artifacts_with_exclusions = {}
    for a in artifacts:
        coordinate = utils.artifact_coordinate(a)
        parts = coordinate.split(":")
        coordinate = "{}:{}".format(parts[0], parts[1])
        if "exclusions" in a and len(a["exclusions"]) > 0:
            deduped_exclusions = {}
            for e in a["exclusions"]:
                if e["group"] == "*" and e["artifact"] == "*":
                    deduped_exclusions = {"*:*": True}
                    break
                deduped_exclusions["{}:{}".format(e["group"], e["artifact"])] = True

            artifacts_with_exclusions[coordinate] = sorted(deduped_exclusions.keys())

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
    dep_tree.update({"exclusions": artifacts_with_exclusions})

    return dep_tree
