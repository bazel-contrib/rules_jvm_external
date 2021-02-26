load("@bazel_skylib//:lib.bzl", "asserts", "unittest")
load("//lib:json_parser.bzl", "json_parse")
load(":json_parse_test_data.bzl", "get_pkg_jsons")

def _valid_json_parse_test(ctx):
    env = unittest.begin(ctx)

    asserts.equals(env, json_parse("[]"), [])
    asserts.equals(
        env,
        json_parse('["x", "y", 22, [7], {"z": 1, "y": null}]'),
        ["x", "y", 22, [7], {"z": 1, "y": None}],
    )
    asserts.equals(
        env,
        " ".join(reversed(json_parse('["plain", "the", "on", "mainly", "falls", "spain", "in", "rain", "the"]'))),
        "the rain in spain falls mainly on the plain",
    )
    asserts.equals(
        env,
        json_parse('["a", "b", "c", [1, 2, 3, [4], [5], [6]]]'),
        ["a", "b", "c", [1, 2, 3, [4], [5], [6]]],
    )
    asserts.equals(env, json_parse("{}"), {})
    asserts.equals(env, json_parse('{"a" : "b"}'), {"a": "b"})
    asserts.equals(
        env,
        json_parse('{"key1": [1, 2, ["nested"]], "key2": "val2", "key3": {"nested_key1" : [null, true, false]}}'),
        {
            "key1": [1, 2, ["nested"]],
            "key2": "val2",
            "key3": {
                "nested_key1": [None, True, False],
            },
        },
    )

    asserts.equals(
        env,
        json_parse('{"key:with:colon" : [{ "nested:with:colon" : true }]}'),
        {"key:with:colon": [{"nested:with:colon": True}]},
    )

    expected_escapes = {"escaped": r'\"quotes\"'}

    # Ughh... need to double escape the escape.
    asserts.equals(env, expected_escapes, json_parse('{"escaped" : "\\"quotes\\""}'))

    # Unless it's a raw literal
    asserts.equals(env, expected_escapes, json_parse(r'{"escaped" : "\"quotes\""}'))
    asserts.equals(
        env,
        expected_escapes,
        json_parse(r'''
{"escaped" : "\"quotes\""}
'''),
    )

    unittest.end(env)

def _scalar_types_test(ctx):
    env = unittest.begin(ctx)

    asserts.equals(env, json_parse('[""]')[0], "")
    asserts.equals(env, json_parse('["a string"]')[0], "a string")
    asserts.equals(env, json_parse("[true]")[0], True)
    asserts.equals(env, json_parse("[false]")[0], False)
    asserts.equals(env, json_parse("[null]")[0], None)
    asserts.equals(env, json_parse("[100]")[0], 100)
    asserts.equals(env, json_parse("[-100]")[0], -100)

    unittest.end(env)

def _number_parse_test(ctx):
    env = unittest.begin(ctx)

    # :( this sucks, but technically it's legal JSON:
    # https://tools.ietf.org/html/rfc8259#section-6
    # "This specification allows implementations to set limits on the range
    # and precision of numbers accepted."
    asserts.equals(env, 2147483647, json_parse("[99e100]")[0])  # MAX int
    asserts.equals(env, -2147483647, json_parse("[-99e100]")[0])  # MIN int
    asserts.equals(env, 0, json_parse("[99e-10]")[0])
    asserts.equals(env, 9, json_parse("[999e-2]")[0])

    asserts.equals(env, 43, json_parse("[43.11]")[0])
    asserts.equals(env, 0, json_parse("[0.12345]")[0])
    asserts.equals(env, -120, json_parse("[-120.12345]")[0])

    unittest.end(env)

def _max_depth_json_parse_test(ctx):
    env = unittest.begin(ctx)

    asserts.equals(
        env,
        json_parse("[[[[[[[[[[[[[[[[[[[[20]]]]]]]]]]]]]]]]]]]]"),
        [[[[[[[[[[[[[[[[[[[[20]]]]]]]]]]]]]]]]]]]],
    )

    asserts.equals(
        env,
        json_parse('[[["too_deep"]]]', fail_on_invalid = False, max_depth = 2),
        None,
    )

    unittest.end(env)

def _package_json_parse_test(ctx):
    env = unittest.begin(ctx)

    pkg_jsons_for_testing = get_pkg_jsons()

    # Use rollup to spot check specific values.
    rollup_pkg_json = json_parse(pkg_jsons_for_testing["rollup"])
    asserts.equals(env, "0.57.1", rollup_pkg_json["version"])
    asserts.equals(env, "dist/rollup.browser.js", rollup_pkg_json["files"][0])
    asserts.equals(env, "Oskar Segersvärd <victorystick@gmail.com>", rollup_pkg_json["contributors"][0])

    for project in pkg_jsons_for_testing:
        print("checking %s/package.json" % project)
        parsed_pkg_json = json_parse(pkg_jsons_for_testing[project])
        asserts.equals(env, "dict", type(parsed_pkg_json))

    unittest.end(env)

valid_json_parse_test = unittest.make(_valid_json_parse_test)
scalar_types_test = unittest.make(_scalar_types_test)
number_parse_test = unittest.make(_number_parse_test)
max_depth_json_parse_test = unittest.make(_max_depth_json_parse_test)
package_json_parse_test = unittest.make(_package_json_parse_test)

def json_parse_test_suite():
    """Creates the test targets and test suite for //lib:json_parse.bzl."""
    unittest.suite(
        "json_parse_tests",
        valid_json_parse_test,
        scalar_types_test,
        number_parse_test,
        max_depth_json_parse_test,
        package_json_parse_test,
    )
