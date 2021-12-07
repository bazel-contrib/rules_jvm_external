load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private:artifact_utilities.bzl", "deduplicate_and_sort_artifacts")


def _empty_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(env, {"dependencies": []}, deduplicate_and_sort_artifacts({"dependencies": []}, [], [], False))
    return unittest.end(env)

empty_test = unittest.make(_empty_test_impl)

def _one_artifact_no_exclusions_test_impl(ctx):
    env = unittest.begin(ctx)

    dep_tree = {
        "conflict_resolution": {},
        "dependencies": [
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            }
        ],
        "version": "0.1.0",
    }

    artifacts = [{
        "group": "com.google.guava",
        "artifact": "guava",
        "version": "27.0-jre",
        "exclusions": []
    }]

    sorted_dep_tree = deduplicate_and_sort_artifacts(dep_tree, artifacts, [], False)

    asserts.equals(env, len(sorted_dep_tree["dependencies"]), 1)
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["coord"], "com.google.guava:guava:27.0-jre")
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["exclusions"], [])

    return unittest.end(env)

one_artifact_no_exclusions_test = unittest.make(_one_artifact_no_exclusions_test_impl)

def _one_artifact_no_exclusions_with_nulls_test_impl(ctx):
    env = unittest.begin(ctx)

    dep_tree = {
        "conflict_resolution": {},
        "dependencies": [
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            },
            {
                "coord": "org.checkerframework:checker-qual:2.5.2",
                "file": None,
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            },
        ],
        "version": "0.1.0",
    }

    artifacts = [{
        "group": "com.google.guava",
        "artifact": "guava",
        "version": "27.0-jre",
        "exclusions": []
    }]

    sorted_dep_tree = deduplicate_and_sort_artifacts(dep_tree, artifacts, [], False)

    asserts.equals(env, len(sorted_dep_tree["dependencies"]), 2)
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["coord"], "com.google.guava:guava:27.0-jre")
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["exclusions"], [])
    asserts.equals(env, sorted_dep_tree["dependencies"][1]["coord"], "org.checkerframework:checker-qual:2.5.2")

    return unittest.end(env)

one_artifact_no_exclusions_with_nulls_test = unittest.make(_one_artifact_no_exclusions_with_nulls_test_impl)

def _one_artifact_duplicate_no_exclusions_test_impl(ctx):
    env = unittest.begin(ctx)

    dep_tree = {
        "conflict_resolution": {},
        "dependencies": [
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            }
        ],
        "version": "0.1.0",
    }

    artifacts = [{
        "group": "com.google.guava",
        "artifact": "guava",
        "version": "27.0-jre",
        "exclusions": []
    }]

    sorted_dep_tree = deduplicate_and_sort_artifacts(dep_tree, artifacts, [], False)

    asserts.equals(env, len(sorted_dep_tree["dependencies"]), 1)
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["coord"], "com.google.guava:guava:27.0-jre")
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["exclusions"], [])

    return unittest.end(env)

one_artifact_duplicate_no_exclusions_test = unittest.make(_one_artifact_duplicate_no_exclusions_test_impl)

def _one_artifact_duplicate_matches_exclusions_test_impl(ctx):
    env = unittest.begin(ctx)

    dep_tree = {
        "conflict_resolution": {},
        "dependencies": [
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                    "*:*"
                ],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                     "org.codehaus.mojo:animal-sniffer-annotations",
                     "com.google.j2objc:j2objc-annotations",
                 ]
            }
        ],
        "version": "0.1.0",
    }

    artifacts = [{
        "group": "com.google.guava",
        "artifact": "guava",
        "version": "27.0-jre",
        "exclusions": [
            {"group": "*", "artifact": "*"}
        ]
    }]

    sorted_dep_tree = deduplicate_and_sort_artifacts(dep_tree, artifacts, [], False)

    asserts.equals(env, len(sorted_dep_tree["dependencies"]), 1)
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["coord"], "com.google.guava:guava:27.0-jre")
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["exclusions"], ["*:*"])

    dep_tree = {
        "conflict_resolution": {},
        "dependencies": [
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                    "*:*"
                ],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                     "org.codehaus.mojo:animal-sniffer-annotations",
                     "com.google.j2objc:j2objc-annotations",
                 ]
            }
        ],
        "version": "0.1.0",
    }

    artifacts = [{
        "group": "com.google.guava",
        "artifact": "guava",
        "version": "27.0-jre",
        "exclusions": [
            {"group": "org.codehaus.mojo", "artifact": "animal-sniffer-annotations"},
            {"group": "com.google.j2objc", "artifact": "j2objc-annotations"},
        ]
    }]

    sorted_dep_tree = deduplicate_and_sort_artifacts(dep_tree, artifacts, [], False)

    asserts.equals(env, len(sorted_dep_tree["dependencies"]), 1)
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["coord"], "com.google.guava:guava:27.0-jre")
    asserts.equals(
        env,
        sorted_dep_tree["dependencies"][0]["exclusions"],
        ["org.codehaus.mojo:animal-sniffer-annotations", "com.google.j2objc:j2objc-annotations"]
    )

    return unittest.end(env)

one_artifact_duplicate_matches_exclusions_test = unittest.make(_one_artifact_duplicate_matches_exclusions_test_impl)

def _one_artifact_duplicate_with_global_exclusions_test_impl(ctx):
    env = unittest.begin(ctx)

    dep_tree = {
        "conflict_resolution": {},
        "dependencies": [
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                    "*:*"
                ],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                     "org.codehaus.mojo:animal-sniffer-annotations",
                     "com.google.j2objc:j2objc-annotations",
                     "org.checkerframework:checker-qual",
                 ]
            }
        ],
        "version": "0.1.0",
    }

    artifacts = [{
        "group": "com.google.guava",
        "artifact": "guava",
        "version": "27.0-jre",
        "exclusions": [
            {"group": "*", "artifact": "*"}
        ]
    }]

    excluded_artifacts = [
        {"group": "com.google.j2objc", "artifact": "j2objc-annotations"},
        {"group": "org.checkerframework", "artifact": "checker-qual"},
    ]

    sorted_dep_tree = deduplicate_and_sort_artifacts(dep_tree, artifacts, excluded_artifacts, False)

    asserts.equals(env, len(sorted_dep_tree["dependencies"]), 1)
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["coord"], "com.google.guava:guava:27.0-jre")
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["exclusions"], ["*:*"])

    dep_tree = {
        "conflict_resolution": {},
        "dependencies": [
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                    "*:*"
                ],
            },
            {
                "coord": "com.google.guava:guava:27.0-jre",
                "file": "v1/https/repo1.maven.org/maven2/com/google/guava/guava/27.0-jre/guava-27.0-jre.jar",
                "directDependencies": [],
                "dependencies": [],
                "exclusions": [
                     "org.codehaus.mojo:animal-sniffer-annotations",
                     "com.google.j2objc:j2objc-annotations",
                     "org.checkerframework:checker-qual",
                 ]
            }
        ],
        "version": "0.1.0",
    }

    artifacts = [{
        "group": "com.google.guava",
        "artifact": "guava",
        "version": "27.0-jre",
        "exclusions": [
            {"group": "org.codehaus.mojo", "artifact": "animal-sniffer-annotations"},
            {"group": "com.google.j2objc", "artifact": "j2objc-annotations"},
        ]
    }]

    sorted_dep_tree = deduplicate_and_sort_artifacts(dep_tree, artifacts, excluded_artifacts, False)

    asserts.equals(env, len(sorted_dep_tree["dependencies"]), 1)
    asserts.equals(env, sorted_dep_tree["dependencies"][0]["coord"], "com.google.guava:guava:27.0-jre")
    asserts.equals(
        env,
        sorted_dep_tree["dependencies"][0]["exclusions"],
        [
            "org.codehaus.mojo:animal-sniffer-annotations",
            "com.google.j2objc:j2objc-annotations",
            "org.checkerframework:checker-qual",
        ]
    )

    return unittest.end(env)

one_artifact_duplicate_with_global_exclusions_test = unittest.make(_one_artifact_duplicate_with_global_exclusions_test_impl)

def artifact_utilities_test_suite():
    unittest.suite(
        "artifact_utilities_tests",
        empty_test,
        one_artifact_no_exclusions_test,
        one_artifact_no_exclusions_with_nulls_test,
        one_artifact_duplicate_no_exclusions_test,
        one_artifact_duplicate_matches_exclusions_test,
        one_artifact_duplicate_with_global_exclusions_test,
    )
