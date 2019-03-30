#!/usr/bin/python3

import argparse
import subprocess
import os

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

def main():
    parser = argparse.ArgumentParser(description="TODO")
    parser.add_argument("-d", "--directory", type=str, required=True)
    parser.add_argument("-m", "--module", action="append", type=str, default = [])
    parser.add_argument(
        "-c",
        "--configuration",
        action="append",
        type=str,
        choices = GRADLE_CONFIGURATIONS,
        default = []
    )
    args = parser.parse_args()
    directory = args.directory
    print(MAVEN_INSTALL_PREFIX)
    for module in args.module:
        if len(args.configuration) > 0:
            for configuration in args.configuration:
                print("        # " + module + ":" + configuration)
                os.system("""cd {directory} && \
                ./gradlew {module}:dependencies --console plain --configuration {configuration} \
                | grep "\-\-\-\ " \
                | grep -v " |" \
                | grep -v " +" \
                | grep -v " \\\\\\\\" \
                | cut -d' ' -f2 \
                | sort \
                | uniq \
                | sed -e 's/\\(.*\\)/"\\1",/' \
                | sed -e 's/^/        /'
                """.format(
                    directory = args.directory,
                    module = module,
                    configuration = configuration
                ))
        else:
            os.system("""cd {directory} && \
            ./gradlew {module}:dependencies --console plain \
            | grep "\-\-\-\ " \
            | grep -v " |" \
            | grep -v " +" \
            | grep -v " \\\\\\\\" \
            | cut -d' ' -f2 \
            | sort \
            | uniq \
            | sed -e 's/\\(.*\\)/"\\1",/' \
            | sed -e 's/^/        /'
            """.format(
                directory = args.directory,
                module = module
            ))
    print(MAVEN_INSTALL_SUFFIX)

main()
