load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//:specs.bzl", "json", "maven", "parse", "utils")

#
# Spec tests
#

def _maven_repository_test_impl(ctx):
    repo_url = "https://maven.google.com/"

    env = unittest.begin(ctx)
    asserts.equals(
        env,
        {"repo_url": repo_url},
        maven.repository(repo_url),
    )
    asserts.equals(
        env,
        {"repo_url": repo_url, "credentials": {"user": "bob", "password": "l0bl4w"}},
        maven.repository(repo_url, "bob", "l0bl4w"),
    )
    return unittest.end(env)

maven_repository_test = unittest.make(_maven_repository_test_impl)

def _maven_artifact_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        {"group": "junit", "artifact": "junit", "version": "4.12"},
        maven.artifact("junit", "junit", "4.12"),
    )
    asserts.equals(
        env,
        {"group": "junit", "artifact": "junit", "version": "4.12", "packaging": "jar"},
        maven.artifact("junit", "junit", "4.12", packaging = "jar"),
    )
    asserts.equals(
        env,
        {"group": "junit", "artifact": "junit", "version": "4.12", "packaging": "jar", "neverlink": True},
        maven.artifact("junit", "junit", "4.12", packaging = "jar", neverlink = True),
    )
    return unittest.end(env)

maven_artifact_test = unittest.make(_maven_artifact_test_impl)

def _maven_exclusion_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        {"group": "junit", "artifact": "junit"},
        maven.exclusion("junit", "junit"),
    )
    return unittest.end(env)

maven_exclusion_test = unittest.make(_maven_exclusion_test_impl)

#
# Parse tests
#

def _parse_coordinate_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        {"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0"},
        parse.parse_maven_coordinate("org.eclipse.aether:aether-api:1.1.0"),
    )
    asserts.equals(
        env,
        {"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0", "packaging": "jar"},
        parse.parse_maven_coordinate("org.eclipse.aether:aether-api:jar:1.1.0"),
    )
    asserts.equals(
        env,
        {"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0", "packaging": "jar", "classifier": "javadoc"},
        parse.parse_maven_coordinate("org.eclipse.aether:aether-api:jar:javadoc:1.1.0"),
    )
    return unittest.end(env)

parse_coordinate_test = unittest.make(_parse_coordinate_test_impl)

def _parse_repository_spec_list_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        [
            {"repo_url": "https://maven.google.com"},
            {"repo_url": "https://repo1.maven.org/maven2"},
        ],
        parse.parse_repository_spec_list([
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ]),
    )
    asserts.equals(
        env,
        [
            {"repo_url": "https://maven.google.com"},
            {"repo_url": "https://repo1.maven.org/maven2", "credentials": {"user": "bob", "password": "l0bl4w"}},
        ],
        parse.parse_repository_spec_list([
            "https://maven.google.com",
            maven.repository("https://repo1.maven.org/maven2", "bob", "l0bl4w"),
        ]),
    )
    return unittest.end(env)

parse_repository_spec_list_test = unittest.make(_parse_repository_spec_list_test_impl)

def _parse_artifact_spec_list_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        [
            {"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0"},
            {"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0", "packaging": "jar", "classifier": "javadoc"},
        ],
        parse.parse_artifact_spec_list([
            "org.eclipse.aether:aether-api:1.1.0",
            "org.eclipse.aether:aether-api:jar:javadoc:1.1.0",
        ]),
    )
    asserts.equals(
        env,
        [
            {"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0"},
            {"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0", "packaging": "jar", "classifier": "javadoc", "neverlink": True},
        ],
        parse.parse_artifact_spec_list([
            "org.eclipse.aether:aether-api:1.1.0",
            maven.artifact("org.eclipse.aether", "aether-api", "1.1.0", packaging = "jar", classifier = "javadoc", neverlink = True),
        ]),
    )
    return unittest.end(env)

parse_artifact_spec_list_test = unittest.make(_parse_artifact_spec_list_test_impl)

#
# JSON tests
#

def _repository_credentials_spec_to_json_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "{ \"user\": \"bob\", \"password\": \"l0bl4w\" }",
        json.write_repository_credentials_spec({"user": "bob", "password": "l0bl4w"}),
    )
    asserts.equals(
        env,
        None,
        json.write_repository_credentials_spec(None),
    )
    return unittest.end(env)

repository_credentials_spec_to_json_test = unittest.make(_repository_credentials_spec_to_json_test_impl)

def _repository_spec_to_json_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "{ \"repo_url\": \"https://maven.google.com\", \"credentials\": { \"user\": \"bob\", \"password\": \"l0bl4w\" } }",
        json.write_repository_spec({"repo_url": "https://maven.google.com", "credentials": {"user": "bob", "password": "l0bl4w"}}),
    )
    asserts.equals(
        env,
        "{ \"repo_url\": \"https://maven.google.com\" }",
        json.write_repository_spec({"repo_url": "https://maven.google.com"}),
    )
    return unittest.end(env)

repository_spec_to_json_test = unittest.make(_repository_spec_to_json_test_impl)

def _exclusion_spec_to_json_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "{ \"group\": \"org.eclipse.aether\", \"artifact\": \"aether-api\" }",
        json.write_exclusion_spec({"group": "org.eclipse.aether", "artifact": "aether-api"}),
    )
    return unittest.end(env)

exclusion_spec_to_json_test = unittest.make(_exclusion_spec_to_json_test_impl)

def _override_license_types_to_json_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "[\"notify\", \"restricted\"]",
        json.write_override_license_types_spec(["notify", "restricted"]),
    )
    asserts.equals(
        env,
        "[]",
        json.write_override_license_types_spec([]),
    )
    return unittest.end(env)

override_license_types_spec_to_json_test = unittest.make(_override_license_types_to_json_test_impl)

def _artifact_to_json_test_impl(ctx):
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "{ \"group\": \"org.eclipse.aether\", \"artifact\": \"aether-api\", \"version\": \"1.1.0\" }",
        json.write_artifact_spec({"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0"}),
    )
    asserts.equals(
        env,
        "{ \"group\": \"org.eclipse.aether\", \"artifact\": \"aether-api\", \"version\": \"1.1.0\", \"exclusions\": [{ \"group\": \"baddep\", \"artifact\": \"goaway\" }] }",
        json.write_artifact_spec({"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0", "exclusions": [{"group": "baddep", "artifact": "goaway"}]}),
    )
    asserts.equals(
        env,
        "{ \"group\": \"org.eclipse.aether\", \"artifact\": \"aether-api\", \"version\": \"1.1.0\", \"packaging\": \"jar\" }",
        json.write_artifact_spec({"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0", "packaging": "jar"}),
    )
    asserts.equals(
        env,
        "{ \"group\": \"org.eclipse.aether\", \"artifact\": \"aether-api\", \"version\": \"1.1.0\", \"packaging\": \"jar\", \"classifier\": \"javadoc\" }",
        json.write_artifact_spec({"group": "org.eclipse.aether", "artifact": "aether-api", "version": "1.1.0", "packaging": "jar", "classifier": "javadoc"}),
    )
    return unittest.end(env)

artifact_spec_to_json_test = unittest.make(_artifact_to_json_test_impl)

def _repo_credentials_test_impl(ctx):
    repo_url = "https://maven.google.com/"
    env = unittest.begin(ctx)
    asserts.equals(
        env,
        "maven.google.com bob:l0bl4w",
        utils.repo_credentials({"repo_url": repo_url, "credentials": {"user": "bob", "password": "l0bl4w"}}),
    )
    return unittest.end(env)

repo_credentials_test = unittest.make(_repo_credentials_test_impl)

def artifact_specs_test_suite():
    unittest.suite(
        "artifact_specs_tests",
        parse_coordinate_test,
        maven_repository_test,
        maven_artifact_test,
        maven_exclusion_test,
    )

    unittest.suite(
        "artifact_parse_tests",
        parse_repository_spec_list_test,
        parse_artifact_spec_list_test,
    )

    unittest.suite(
        "artifact_json_tests",
        repository_credentials_spec_to_json_test,
        repository_spec_to_json_test,
        exclusion_spec_to_json_test,
        override_license_types_spec_to_json_test,
        artifact_spec_to_json_test,
    )

    unittest.suite(
        "util_tests",
        repo_credentials_test,
    )
