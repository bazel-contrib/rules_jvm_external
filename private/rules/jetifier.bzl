load("//:specs.bzl", "parse")
load(":jetifier_maven_map.bzl", "jetifier_maven_map")

def jetify_maven_coord(group, artifact, version):
    """
    Looks up support -> androidx artifact mapping, returns None if no mapping found.
    """
    if (group, artifact) not in jetifier_maven_map:
        return None

    return jetifier_maven_map[(group, artifact)].get(version, None)

def jetify_artifact_dependencies(deps):
    """Takes in list of maven coordinates and returns a list of jetified maven coordinates"""
    ret = []
    for coord_str in deps:
        artifact = parse.parse_maven_coordinate(coord_str)
        jetify_coord_tuple = jetify_maven_coord(
            artifact["group"],
            artifact["artifact"],
            artifact["version"],
        )
        if jetify_coord_tuple:
            artifact["group"] = jetify_coord_tuple[0]
            artifact["artifact"] = jetify_coord_tuple[1]
            artifact["version"] = jetify_coord_tuple[2]
            ret.append("{}:{}{}{}:{}".format(
                artifact["group"],
                artifact["artifact"],
                (":" + artifact["packaging"]) if "packaging" in artifact else "",
                (":" + artifact["classifier"]) if "classifier" in artifact else "",
                artifact["version"],
            ))
        else:
            ret.append(coord_str)
    return ret
