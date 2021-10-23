#
# Utilites for working with artifacts
#

load("//:specs.bzl", "utils")

def deduplicate_and_sort_artifacts(dep_tree, artifacts, excluded_artifacts, verbose):
    # First we find all of the artifacts that have exclusions
    artifacts_with_exclusions = {}
    for a in artifacts:
        coordinate = utils.artifact_coordinate(a)
        if "exclusions" in a:
            deduped_exclusions = {}
            for e in excluded_artifacts:
                deduped_exclusions["{}:{}".format(e["group"], e["artifact"])] = True
            for e in a["exclusions"]:
                if e["group"] == "*" and e["artifact"] == "*":
                    deduped_exclusions = {"*:*": True}
                    break
                deduped_exclusions["{}:{}".format(e["group"], e["artifact"])] = True
            artifacts_with_exclusions[coordinate] = deduped_exclusions.keys()

    # As we de-duplicate the list keep the duplicate artifacts with exclusions separate
    # so we can look at them and select the one that has the same exclusions
    duplicate_artifacts_with_exclusions = {}
    deduped_artifacts = {}
    null_artifacts = []
    for artifact in dep_tree["dependencies"]:
        if artifact["file"] == None:
            null_artifacts.append(artifact)
            continue
        if artifact["coord"] in artifacts_with_exclusions:
            if artifact["coord"] in duplicate_artifacts_with_exclusions:
                duplicate_artifacts_with_exclusions[artifact["coord"]].append(artifact)
            else:
                duplicate_artifacts_with_exclusions[artifact["coord"]] = [artifact]
        else:
            if artifact["file"] in deduped_artifacts:
                continue
            deduped_artifacts[artifact["file"]] = artifact

    # Look through the duplicates with exclusions and try to select the artifact
    # that has the same exclusions as specified in the artifact
    for duplicate_coord in duplicate_artifacts_with_exclusions:
        deduped_artifact_with_exclusion = duplicate_artifacts_with_exclusions[duplicate_coord][0]
        found_artifact_with_exclusion = False
        for duplicate_artifact in duplicate_artifacts_with_exclusions[duplicate_coord]:
            if sorted(duplicate_artifact["exclusions"]) == sorted(artifacts_with_exclusions[duplicate_coord]):
                found_artifact_with_exclusion = True
                deduped_artifact_with_exclusion = duplicate_artifact
        if verbose and not found_artifact_with_exclusion:
            print("Could not find duplicate artifact with matching exclusions for {} when de-duplicating the dependency tree. Using exclusions {}".format(deduped_artifact_with_exclusion))
        deduped_artifacts[deduped_artifact_with_exclusion["file"]] = deduped_artifact_with_exclusion

    # After we have added the de-duped artifacts with exclusions we need to re-sort the list
    sorted_deduped_values = []
    for key in sorted(deduped_artifacts.keys()):
        sorted_deduped_values.append(deduped_artifacts[key])

    dep_tree.update({"dependencies": sorted_deduped_values + null_artifacts})

    return dep_tree
