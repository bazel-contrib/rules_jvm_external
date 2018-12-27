load("@bazel_skylib//lib:unittest.bzl", "unittest", "asserts")
load("//third_party/bazel_json/lib:json_parser.bzl", "json_parse")
load(":coursier_testdata.bzl", "TEST_PAIRS")
load("//:coursier.bzl", "generate_imports")

def _mock_repository_ctx_os():
    return struct(
        name = "foo",
    )

def _mock_1_arity_fn(unused):
    pass

def _mock_2_arity_fn(unused1, unused2):
    pass

def _mock_repository_ctx():
    return struct(
        path = _mock_1_arity_fn,
        symlink = _mock_2_arity_fn,
        os = _mock_repository_ctx_os(),
    )

def _coursier_test_impl(ctx):
    env = unittest.begin(ctx)
    mock_repository_ctx = _mock_repository_ctx()

    for (json_inputs, expected_build_file) in TEST_PAIRS:
        (json_input, srcs_json_input) = json_inputs
        srcs_dep_tree = None
        if srcs_json_input != None:
            srcs_dep_tree = json_parse(srcs_json_input)

        (GUAVA_ACTUAL_BUILD, unused_checksums) = generate_imports(
            dep_tree = json_parse(json_input),
            repository_ctx = mock_repository_ctx,
            srcs_dep_tree = srcs_dep_tree,
        )
        asserts.equals(env, expected_build_file, GUAVA_ACTUAL_BUILD)

    unittest.end(env)

coursier_test = unittest.make(_coursier_test_impl)

def coursier_test_suite():
    unittest.suite(
        "coursier_tests",
        coursier_test,
    )
