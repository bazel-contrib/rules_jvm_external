load("@bazel_skylib//lib:unittest.bzl", "asserts", "unittest")
load("//private:java_utilities.bzl", "build_java_argsfile_content", "parse_java_version")

def _parse_java_version_test_impl(ctx):
    env = unittest.begin(ctx)

    asserts.equals(env, None, parse_java_version(""))
    asserts.equals(env, None, parse_java_version("version "))
    asserts.equals(env, None, parse_java_version("java\nversion\n\"1.7.0_44\""))

    asserts.equals(
        env,
        7,
        parse_java_version("""
java version "1.7.0_55"
Java(TM) SE Runtime Environment (build 1.7.0_55-b13)
Java HotSpot(TM) 64-Bit Server VM (build 24.55-b03, mixed mode)
"""),
    )

    asserts.equals(
        env,
        8,
        parse_java_version("""
java version "1.8.0_202"
Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
Java HotSpot(TM) 64-Bit Server VM (build 25.202-b08, mixed mode)
"""),
    )

    asserts.equals(
        env,
        9,
        parse_java_version("""
java version "9"
Java(TM) SE Runtime Environment (build 9+181)
Java HotSpot(TM) 64-Bit Server VM (build 9+181, mixed mode)
"""),
    )

    asserts.equals(
        env,
        10,
        parse_java_version("""
java version "10.0.1" 2018-04-17
Java(TM) SE Runtime Environment 18.3 (build 10.0.1+10)
Java HotSpot(TM) 64-Bit Server VM 18.3 (build 10.0.1+10, mixed mode)
"""),
    )

    asserts.equals(
        env,
        11,
        parse_java_version("""
openjdk version "11.0.9" 2020-10-20
OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.9+11)
OpenJDK 64-Bit Server VM AdoptOpenJDK (build 11.0.9+11, mixed mode)
"""),
    )

    asserts.equals(
        env,
        15,
        parse_java_version("""
openjdk version "15" 2020-09-15
OpenJDK Runtime Environment (build 15+36-1562)
OpenJDK 64-Bit Server VM (build 15+36-1562, mixed mode, sharing)
"""),
    )

    asserts.equals(
        env,
        22,
        parse_java_version("""
openjdk 22-ea 2024-03-19
OpenJDK Runtime Environment (Red_Hat-22.0.0.0.36-1) (build 22-ea+36)
OpenJDK 64-Bit Server VM (Red_Hat-22.0.0.0.36-1) (build 22-ea+36, mixed mode, sharing)
"""),
    )

    return unittest.end(env)

parse_java_version_test = unittest.make(_parse_java_version_test_impl)

def _build_java_argsfile_content_test_impl(ctx):
    env = unittest.begin(ctx)

    asserts.equals(
        env,
        """"--credentials"
"some.private.maven.re johndoe:example-password"
""",
        build_java_argsfile_content(["--credentials", "some.private.maven.re johndoe:example-password"]),
    )

    asserts.equals(
        env,
        """"--credentials"
"some.private.maven.re johndoe:example-password-with-\\"quotation-marks\\""
""",
        build_java_argsfile_content(["--credentials", "some.private.maven.re johndoe:example-password-with-\"quotation-marks\""]),
    )

    return unittest.end(env)

build_java_argsfile_content_test = unittest.make(_build_java_argsfile_content_test_impl)

def java_utilities_test_suite():
    unittest.suite(
        "java_utilities_tests",
        parse_java_version_test,
        build_java_argsfile_content_test,
    )
