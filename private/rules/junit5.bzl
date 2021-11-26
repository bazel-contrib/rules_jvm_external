load("//private/rules:artifact.bzl", "artifact")

"""Dependencies typically required by JUnit 5 tests.

See `java_junit5_test` for more details.
"""
JUNIT5_DEPS = [
    artifact("org.junit.jupiter:junit-jupiter-engine"),
    artifact("org.junit.platform:junit-platform-launcher"),
    artifact("org.junit.platform:junit-platform-reporting"),
]

JUNIT5_VINTAGE_DEPS =  [
    artifact("org.junit.vintage:junit-vintage-engine"),
] + JUNIT5_DEPS


# Common package prefixes, in the order we want to check for them
_PREFIXES = (".com.", ".org.", ".net.", ".io.")

# By default bazel computes the name of test classes based on the
# standard Maven directory structure, which we may not always use,
# so try to compute the correct package name.
def _get_package_name():
    pkg = native.package_name().replace("/", ".")

    for prefix in _PREFIXES:
        idx = pkg.find(prefix)
        if idx != -1:
            return pkg[idx + 1:] + "."

    return ""

def java_junit5_test(name, test_class = None, runtime_deps = [], **kwargs):
    """Run junit5 tests using Bazel.

    This is designed to be a drop-in replacement for `java_test`, but
    rather than using a JUnit4 runner it provides support for using
    JUnit5 directly. The arguments are the same as used by `java_test`.

    The generated target does not include any JUnit5 dependencies. If
    you are using the standard `@maven` namespace for your
    `maven_install` you can add these to your `deps` using `JUNIT5_DEPS`
    or `JUNIT5_VINTAGE_DEPS` loaded from `//:defs.bzl`

    Args:
      name: The name of the test.
      test_class: The Java class to be loaded by the test runner. If not
        specified, the class name will be inferred from a combination of
        the current bazel package and the `name` attribute.
    """
    if test_class:
        clazz = test_class
    else:
        clazz = _get_package_name() + name

    native.java_test(
        name = name,
        main_class = "com.github.bazelbuild.rules_jvm_external.junit5.JUnit5Runner",
        test_class = clazz,
        runtime_deps = runtime_deps + [
            "@rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external/junit5",
        ],
        **kwargs
    )
