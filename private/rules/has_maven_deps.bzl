MavenInfo = provider(
    fields = {
        "coordinates": "Maven coordinates for the project, which may be None",
        "artifact_jars": "Depset of runtime jars that are unique to the artifact",
        "jars_from_maven_deps": "Depset of jars from all transitive maven dependencies",
        "artifact_source_jars": "Depset of source jars that unique to the artifact",
        "source_jars_from_maven_deps": "Depset of jars from all transitive maven dependencies",
        "maven_deps": "Depset of first order maven dependencies",
        "as_maven_dep": "Depset of this project if used as a maven dependency",
        "deps_java_infos": "Depset of JavaInfo instances of dependencies not included in the project",
        "transitive_infos": "Dict of label to JavaInfos",
    },
)

_EMPTY_INFO = MavenInfo(
    coordinates = None,
    artifact_jars = depset(),
    artifact_source_jars = depset(),
    jars_from_maven_deps = depset(),
    source_jars_from_maven_deps = depset(),
    maven_deps = depset(),
    as_maven_dep = depset(),
    deps_java_infos = depset(),
    transitive_infos = {},
)

_MAVEN_PREFIX = "maven_coordinates="
_STOP_TAGS = ["maven:compile-only", "no-maven"]

def _read_coordinates(tags):
    coordinates = []
    for stop_tag in _STOP_TAGS:
        if stop_tag in tags:
            return None

    for tag in tags:
        if tag.startswith(_MAVEN_PREFIX):
            coordinates.append(tag[len(_MAVEN_PREFIX):])

    if len(coordinates) > 1:
        fail("Zero or one set of coordinates should be defined: %s" % coordinates)

    if len(coordinates) == 1:
        return coordinates[0]

    return None

_ASPECT_ATTRS = [
    "deps",
    "exports",
    "runtime_deps",
]

def _set_diff(first, second):
    """Returns all items in `first` that are not in `second`"""

    return [item for item in first if item not in second]

def _filter_external_jars(workspace_name, items):
    return [item for item in items if item.owner.workspace_name in ["", workspace_name]]

def _has_maven_deps_impl(target, ctx):
    if not JavaInfo in target:
        return [_EMPTY_INFO]

    # Check the stop tags first to let us exit quickly.
    for tag in ctx.rule.attr.tags:
        if tag in _STOP_TAGS:
            return _EMPTY_INFO

    all_deps = []
    for attr in _ASPECT_ATTRS:
        all_deps.extend(getattr(ctx.rule.attr, attr, []))

    all_infos = []
    first_order_java_infos = []
    for dep in all_deps:
        if not MavenInfo in dep:
            continue

        all_infos.append(dep[MavenInfo])

        if JavaInfo in dep and dep[MavenInfo].coordinates:
            first_order_java_infos.append(dep[JavaInfo])

    deps_java_infos = depset(
        items = first_order_java_infos,
        transitive = [dep.deps_java_infos for dep in all_infos])

    all_jars = target[JavaInfo].transitive_runtime_jars
    jars_from_maven_deps = depset(transitive = [info.jars_from_maven_deps for info in all_infos])
    items = _set_diff(all_jars.to_list(), jars_from_maven_deps.to_list())
    artifact_jars = depset(_filter_external_jars(ctx.workspace_name, items))

    all_source_jars = target[JavaInfo].transitive_source_jars
    source_jars_from_maven_deps = depset(transitive = [jpi.source_jars_from_maven_deps for jpi in all_infos])
    items = _set_diff(all_source_jars.to_list(), source_jars_from_maven_deps.to_list())
    artifact_source_jars = depset(_filter_external_jars(ctx.workspace_name, items))

    coordinates = _read_coordinates(ctx.rule.attr.tags)

    first_order_maven_deps = depset(transitive = [jpi.as_maven_dep for jpi in all_infos])

    # If we have coordinates our current `all_jars` is also our `maven_dep_jars`.
    # Otherwise, we need to collect them from the MavenInfos we depend
    # upon.
    maven_dep_jars = all_jars if coordinates else jars_from_maven_deps

    transitive_infos = {target.label: target[JavaInfo]}
    for mi in all_infos:
        transitive_infos.update(mi.transitive_infos)

    info = MavenInfo(
        coordinates = coordinates,
        artifact_jars = artifact_jars,
        jars_from_maven_deps = all_jars if coordinates else jars_from_maven_deps,
        artifact_source_jars = artifact_source_jars,
        source_jars_from_maven_deps = all_source_jars if coordinates else source_jars_from_maven_deps,
        maven_deps = first_order_maven_deps,
        as_maven_dep = depset([coordinates]) if coordinates else first_order_maven_deps,
        deps_java_infos = deps_java_infos,
        transitive_infos = transitive_infos,
    )

    return [
        info,
    ]

has_maven_deps = aspect(
    _has_maven_deps_impl,
    attr_aspects = _ASPECT_ATTRS,
)
