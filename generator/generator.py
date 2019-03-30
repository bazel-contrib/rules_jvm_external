#!/usr/bin/python3

import argparse
import subprocess
import re
import os
import sys
from collections import OrderedDict

GRADLE_CONFIGURATIONS = [
    "api",
    "implementation",
    "testImplementation",
    "androidTestImplementation",
    "kapt",
    "annotationProcessor",
]

WORKSPACE_TEMPLATE = """
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "1.2"
RULES_JVM_EXTERNAL_SHA = "e5c68b87f750309a79f59c2b69ead5c3221ffa54ff9496306937bfa1c9c8c86b"

local_repository(
    name = "rules_jvm_external",
    path = "/Users/jin/code/rules_jvm_external",
)

# http_archive(
#     name = "rules_jvm_external",
#     strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
#     sha256 = RULES_JVM_EXTERNAL_SHA,
#     url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
# )

load("@rules_jvm_external//:defs.bzl", "maven_install")
maven_install(
    name = "maven",
    artifacts = [
{artifacts}
    ],
    repositories = [
        "https://maven.google.com",
        "https://jcenter.bintray.com",
        "https://repo1.maven.org/maven2",
    ],
)
"""

def generate_gradle(directory, modules, configurations, write_to_project_directory):
    artifacts = []
    artifact_regexp = re.compile(r'^.---\s.+:.+:.+')

    cmd = [os.path.join(directory, "gradlew"), "--console", "plain"]
    if len(modules) > 0:
        for module in modules:
            module_cmd = cmd + ["%s:dependencies" % module]
            if len(configurations) > 0:
                for configuration in configurations:
                    configured_cmd = module_cmd + ["--configuration", configuration]
                    artifacts.append("# " + module + ":" + configuration)
                    try:
                        raw_gradle_output = subprocess.check_output(configured_cmd, cwd=directory)
                    except subprocess.CalledProcessError as e:
                        print("Execution of \"gradlew dependencies\" failed: ", e)
                        sys.exit(1)
                    artifacts.extend(
                        map(
                            lambda line: line.split()[1],
                            filter(
                                lambda line: artifact_regexp.search(line),
                                raw_gradle_output.splitlines()
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
    else:
        configured_cmd = cmd + ["dependencies"]
        artifacts.extend(
            map(
                lambda line: line.split()[1],
                filter(
                    lambda line: artifact_regexp.search(line),
                    subprocess.check_output(configured_cmd, cwd=directory).splitlines())
            )
        )
        # Dedupe the list
        artifacts = OrderedDict((x, True) for x in artifacts).keys()

    WORKSPACE = WORKSPACE_TEMPLATE.format(
        artifacts = "\n".join(map(lambda a: "        " + a if a.startswith("#") else "        \"%s\"," % a, artifacts))
    )

    if write_to_project_directory:
        cmd = [os.path.join(directory, "gradlew"), "properties"]
        android_plugin_enabled = True if "android" in subprocess.check_output(cmd, cwd=directory) else False
        if android_plugin_enabled:
            os.chdir(directory)
            f = open("WORKSPACE.new", "a")
            f.write(WORKSPACE)
            f.close()
        else:
            raise RuntimeError("Writing to project directory currently only supports Android projects.")
    else:
        print(WORKSPACE)


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
        "-w",
        "--write_to_project_directory",
        action='store_true',
        default = False,
    )
    args = parser.parse_args()
    if args.build_system == "gradle":
        generate_gradle(args.directory, args.module, args.configuration, args.write_to_project_directory)
    else:
        parser.print_usage()

main()
