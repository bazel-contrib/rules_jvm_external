#!/usr/bin/python3

import argparse
import subprocess
import re
import os
from collections import OrderedDict

GRADLE_CONFIGURATIONS = [
    "api",
    "implementation",
    "testImplementation",
    "androidTestImplementation",
    "kapt",
    "annotationProcessor",
]

MAVEN_INSTALL_PREFIX = """
load("@rules_jvm_external//:defs.bzl", "maven_install")
maven_install(
    name = "maven",
    artifacts = ["""

MAVEN_INSTALL_SUFFIX = """    ],
    repositories = [
        "https://maven.google.com",
        "https://jcenter.bintray.com",
        "https://repo1.maven.org/maven2",
    ],
)
"""

def generate_gradle(directory, modules, configurations):
    artifacts = []
    artifact_regexp = re.compile(r'^.---\s.+:.+:.+')

    print(MAVEN_INSTALL_PREFIX)

    for module in modules:
        cmd = [os.path.join(directory, "gradlew"), "%s:dependencies" % module, "--console", "plain"]
        if len(configurations) > 0:
            for configuration in configurations:
                configured_cmd = cmd + ["--configuration", configuration]
                artifacts.append("# " + module + ":" + configuration)
                artifacts.extend(
                    map(
                        lambda line: line.split()[1],
                        filter(
                            lambda line: artifact_regexp.search(line),
                            # subprocess.check_output(" ".join(configured_cmd), cwd=directory, shell=True).splitlines())
                            subprocess.check_output(configured_cmd, cwd=directory).splitlines()
                        )
                    )
                )
        else:
            artifacts.append("# " + module)
            artifacts.extend(
                map(
                    lambda line: line.split()[1],
                    filter(
                        lambda line: artifact_regexp.search(line),
                        subprocess.check_output(cmd, cwd=directory).splitlines())
                )
            )
            # Dedupe the list
            artifacts = OrderedDict((x, True) for x in artifacts).keys()

        print("\n".join(map(lambda a: "        " + a if a.startswith("#") else "        \"%s\"," % a, artifacts)))

    print(MAVEN_INSTALL_SUFFIX)


def main():
    parser = argparse.ArgumentParser(description="Generate a maven_install declaration from Gradle projects")
    subparsers = parser.add_subparsers(dest="build_system", help="Select a build system")

    gradle_parser = subparsers.add_parser("gradle", help="Generate for the Gradle build system.")
    gradle_parser.add_argument(
        "-d",
        "--directory",
        help="Path to the root project directory. This is typically the directory containing `gradlew`.",
        type=str,
        required=True
    )
    gradle_parser.add_argument(
        "-m",
        "--module",
        help="The selected module(s) to resolve dependencies for. Defaults to the root module. Can be specified multiple times.",
        action="append",
        type=str,
        default = []
    )
    gradle_parser.add_argument(
        "-c",
        "--configuration",
        help="The configuration of dependencies to resolve. Defaults to all configurations. Can be specified multiple times.",
        action="append",
        type=str,
        choices = GRADLE_CONFIGURATIONS,
        default = []
    )
    gradle_parser.add_argument(
        "-b",
        "--build-system",
        type=str,
        choices = ["gradle", "maven"],
        default = "gradle",
    )
    args = parser.parse_args()
    if args.build_system == "gradle":
        generate_gradle(args.directory, args.module, args.configuration)
    else:
        parser.print_usage()

main()
