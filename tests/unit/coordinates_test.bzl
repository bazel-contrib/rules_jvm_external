load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private/lib:coordinates.bzl", "unpack_coordinates")

def _group_and_artifact_impl(ctx):
    env = unittest.begin(ctx)

    unpacked = unpack_coordinates("group:artifact")
    asserts.equals(env, "group", unpacked.groupId)
    asserts.equals(env, "artifact", unpacked.artifactId)
    asserts.equals(env, None, unpacked.version)

    return unittest.end(env)

group_and_artifact_test = unittest.make(_group_and_artifact_impl)

def _group_artifact_and_version_impl(ctx):
    env = unittest.begin(ctx)

    unpacked = unpack_coordinates("group:artifact:1.2.3")
    asserts.equals(env, "group", unpacked.groupId)
    asserts.equals(env, "artifact", unpacked.artifactId)
    asserts.equals(env, "1.2.3", unpacked.version)

    return unittest.end(env)

group_artifact_and_version_test = unittest.make(_group_artifact_and_version_impl)

def _complete_original_format_impl(ctx):
    env = unittest.begin(ctx)

    unpacked = unpack_coordinates("group:artifact:type:scope:1.2.3")
    asserts.equals(env, "group", unpacked.groupId)
    asserts.equals(env, "artifact", unpacked.artifactId)
    asserts.equals(env, "1.2.3", unpacked.version)
    asserts.equals(env, "type", unpacked.type)
    asserts.equals(env, "scope", unpacked.scope)

    return unittest.end(env)

complete_original_format_test = unittest.make(_complete_original_format_impl)

def _original_format_omitting_scope_impl(ctx):
    env = unittest.begin(ctx)

    unpacked = unpack_coordinates("group:artifact:type:1.2.3")
    asserts.equals(env, "group", unpacked.groupId)
    asserts.equals(env, "artifact", unpacked.artifactId)
    asserts.equals(env, "1.2.3", unpacked.version)
    asserts.equals(env, "type", unpacked.type)
    asserts.equals(env, None, unpacked.classifier)

    return unittest.end(env)

original_format_omitting_scope_test = unittest.make(_original_format_omitting_scope_impl)

def _gradle_format_without_type_impl(ctx):
    env = unittest.begin(ctx)

    unpacked = unpack_coordinates("group:artifact:1.2.3:classifier")
    asserts.equals(env, "group", unpacked.groupId)
    asserts.equals(env, "artifact", unpacked.artifactId)
    asserts.equals(env, "1.2.3", unpacked.version)
    asserts.equals(env, None, unpacked.type)
    asserts.equals(env, "classifier", unpacked.classifier)

    return unittest.end(env)

gradle_format_without_type_test = unittest.make(_gradle_format_without_type_impl)

def _gradle_format_with_type_and_classifier_impl(ctx):
    env = unittest.begin(ctx)

    unpacked = unpack_coordinates("group:artifact:1.2.3:classifier@type")
    asserts.equals(env, "group", unpacked.groupId)
    asserts.equals(env, "artifact", unpacked.artifactId)
    asserts.equals(env, "1.2.3", unpacked.version)
    asserts.equals(env, "type", unpacked.type)
    asserts.equals(env, "classifier", unpacked.classifier)

    return unittest.end(env)

gradle_format_with_type_and_classifier_test = unittest.make(_gradle_format_with_type_and_classifier_impl)

def _gradle_format_with_type_but_no_classifier_impl(ctx):
    env = unittest.begin(ctx)

    unpacked = unpack_coordinates("group:artifact:1.2.3@type")
    asserts.equals(env, "group", unpacked.groupId)
    asserts.equals(env, "artifact", unpacked.artifactId)
    asserts.equals(env, "1.2.3", unpacked.version)
    asserts.equals(env, "type", unpacked.type)
    asserts.equals(env, None, unpacked.scope)

    return unittest.end(env)

gradle_format_with_type_but_no_classifier_test = unittest.make(_gradle_format_with_type_but_no_classifier_impl)

def _multiple_formats_impl(ctx):
    env = unittest.begin(ctx)

    coords_to_structs = {
        "groupId:artifactId:1.2.3": struct(groupId = "groupId", artifactId = "artifactId", version = "1.2.3", classifier = None, scope = None, type = None),
#        "groupId:artifactId:type:1.2.3": struct(groupId = "groupId", artifactId = "artifactId", version = "1.2.3", scope = None, type = "type"),
#        "groupId:artifactId:type:classifier:1.2.3": struct(groupId = "groupId", artifactId = "artifactId", version = "1.2.3", scope = "classifier", type = "type"),
#        "groupId:artifactId:1.2.3@type": struct(groupId = "groupId", artifactId = "artifactId", version = "1.2.3", scope = None, type = "type"),
#        "groupId:artifactId:1.2.3:classifier@type": struct(groupId = "groupId", artifactId = "artifactId", version = "1.2.3", scope = "classifier", type = "type"),
    }

    for (coords, expected) in coords_to_structs.items():
        unpacked = unpack_coordinates(coords)
        asserts.equals(env, expected, unpacked)

    return unittest.end(env)

multiple_formats_test = unittest.make(_multiple_formats_impl)

def coordinates_test_suite():
    unittest.suite(
        "coordinates_tests",
        group_and_artifact_test,
        group_artifact_and_version_test,
        complete_original_format_test,
        original_format_omitting_scope_test,
        gradle_format_without_type_test,
        gradle_format_with_type_and_classifier_test,
        gradle_format_with_type_but_no_classifier_test,
        multiple_formats_test,
    )
