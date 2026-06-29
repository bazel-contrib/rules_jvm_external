"""Tests for generated dependency tree BUILD target declarations."""

load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private:dependency_tree_parser.bzl", "parser")

def _repo_ctx(maven_install_json = False):
    return struct(
        attr = struct(
            fetch_javadoc = False,
            fetch_sources = False,
            generate_compat_repositories = False,
            maven_install_json = maven_install_json,
            repositories = ["{ \"repo_url\": \"https://repo1.maven.org/maven2/\" }"],
            strict_visibility = False,
            strict_visibility_value = ["//visibility:public"],
        ),
    )

def _generate_imports(dependencies, exclusions, override_targets = {}, maven_install_json = False):
    result = parser.generate_imports(
        repository_ctx = _repo_ctx(maven_install_json = maven_install_json),
        dependencies = dependencies,
        explicit_artifacts = {},
        neverlink_artifacts = {},
        testonly_artifacts = {},
        exclusions = exclusions,
        override_targets = override_targets,
        override_target_visibilities = {},
        skip_maven_local_dependencies = False,
    )
    return result[0]

def _artifact(group_artifact, version, deps = []):
    group, artifact = group_artifact.split(":")
    return {
        "coordinates": "%s:%s" % (group_artifact, version),
        "deps": deps,
        "file": "v1/https/repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar" % (
            group.replace(".", "/"),
            artifact,
            version,
            artifact,
            version,
        ),
        "urls": [
            "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar" % (
                group.replace(".", "/"),
                artifact,
                version,
                artifact,
                version,
            ),
        ],
    }

def _exclusion_removes_generated_dep_impl(ctx):
    env = unittest.begin(ctx)

    generated_imports = _generate_imports(
        dependencies = [
            _artifact(
                "com.example:parent",
                "1.0",
                deps = [
                    "com.example:excluded:1.0",
                    "com.example:kept:1.0",
                ],
            ),
            _artifact("com.example:excluded", "1.0"),
            _artifact("com.example:kept", "1.0"),
        ],
        exclusions = {
            "com.example:parent": [
                "com.example:not-it",
                "com.example:excluded",
            ],
        },
    )

    asserts.true(env, "\"maven_exclusion=com.example:not-it\"," in generated_imports)
    asserts.true(env, "\"maven_exclusion=com.example:excluded\"," in generated_imports)
    asserts.false(env, "\t\t\":com_example_excluded\",\n" in generated_imports)
    asserts.true(env, "\t\t\":com_example_kept\",\n" in generated_imports)

    return unittest.end(env)

exclusion_removes_generated_dep_test = unittest.make(_exclusion_removes_generated_dep_impl)

def _wildcard_exclusion_removes_all_generated_deps_impl(ctx):
    env = unittest.begin(ctx)

    generated_imports = _generate_imports(
        dependencies = [
            _artifact(
                "com.example:parent",
                "1.0",
                deps = [
                    "com.example:first:1.0",
                    "com.example:second:1.0",
                ],
            ),
            _artifact("com.example:first", "1.0"),
            _artifact("com.example:second", "1.0"),
        ],
        exclusions = {
            "com.example:parent": ["*:*"],
        },
    )

    asserts.true(env, "\"maven_exclusion=*:*\"," in generated_imports)
    asserts.false(env, "\t\t\":com_example_first\",\n" in generated_imports)
    asserts.false(env, "\t\t\":com_example_second\",\n" in generated_imports)

    return unittest.end(env)

wildcard_exclusion_removes_all_generated_deps_test = unittest.make(_wildcard_exclusion_removes_all_generated_deps_impl)

def _pom_only_exclusion_removes_generated_export_impl(ctx):
    env = unittest.begin(ctx)

    generated_imports = _generate_imports(
        dependencies = [
            {
                "coordinates": "com.example:parent-pom:1.0@pom",
                "deps": [
                    "com.example:excluded:1.0",
                    "com.example:kept:1.0",
                ],
                "file": None,
                "urls": [],
            },
            _artifact("com.example:excluded", "1.0"),
            _artifact("com.example:kept", "1.0"),
        ],
        exclusions = {
            "com.example:parent-pom": ["com.example:excluded"],
        },
    )

    asserts.false(env, "\t\t\":com_example_excluded\",\n" in generated_imports)
    asserts.true(env, "\t\t\":com_example_kept\",\n" in generated_imports)

    return unittest.end(env)

pom_only_exclusion_removes_generated_export_test = unittest.make(_pom_only_exclusion_removes_generated_export_impl)

def _overridden_aar_original_artifact_uses_real_http_file_repo_impl(ctx):
    env = unittest.begin(ctx)

    generated_imports = _generate_imports(
        dependencies = [
            {
                "coordinates": "com.google.android.gms:play-services-tasks:18.1.0@aar",
                "deps": [],
                "file": "v1/https/maven.google.com/com/google/android/gms/play-services-tasks/18.1.0/play-services-tasks-18.1.0.aar",
                "urls": [
                    "https://maven.google.com/com/google/android/gms/play-services-tasks/18.1.0/play-services-tasks-18.1.0.aar",
                ],
            },
        ],
        exclusions = {},
        override_targets = {
            "com.google.android.gms:play-services-tasks": "//third_party:play_services_tasks",
        },
        maven_install_json = True,
    )

    asserts.true(env, "name = \"original_com_google_android_gms_play_services_tasks_aar_18_1_0_extension\"," in generated_imports)
    asserts.true(env, "src = \"@com_google_android_gms_play_services_tasks_aar_18_1_0//file\"," in generated_imports)
    asserts.false(env, "src = \"@original_com_google_android_gms_play_services_tasks_aar_18_1_0//file\"," in generated_imports)

    return unittest.end(env)

overridden_aar_original_artifact_uses_real_http_file_repo_test = unittest.make(_overridden_aar_original_artifact_uses_real_http_file_repo_impl)

def dependency_tree_parser_test_suite():
    unittest.suite(
        "dependency_tree_parser_tests",
        exclusion_removes_generated_dep_test,
        wildcard_exclusion_removes_all_generated_deps_test,
        pom_only_exclusion_removes_generated_export_test,
        overridden_aar_original_artifact_uses_real_http_file_repo_test,
    )
