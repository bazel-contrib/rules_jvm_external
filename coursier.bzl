# Copyright 2019 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and

load("//third_party/bazel_json/lib:json_parser.bzl", "json_parse")
load("//:specs.bzl", "utils")
load("//:private/proxy.bzl" , "get_java_proxy_args")
load(
    "//:private/versions.bzl",
    "COURSIER_CLI_GITHUB_ASSET_URL",
    "COURSIER_CLI_BAZEL_MIRROR_URL",
    "COURSIER_CLI_SHA256",
)

_BUILD = """
package(default_visibility = ["//visibility:{visibility}"])

exports_files(["pin"])

load("@{repository_name}//:jvm_import.bzl", "jvm_import")

{imports}
"""

# Coursier uses these types to determine what files it should resolve and fetch.
# For example, some jars have the type "eclipse-plugin", and Coursier would not
# download them if it's not asked to to resolve "eclipse-plugin".
_COURSIER_PACKAGING_TYPES = [
    "jar",
    "aar",
    "bundle",
    "eclipse-plugin",
    "orbit",
    "test-jar",
    "hk2-jar",
    "maven-plugin",
    "scala-jar",
]

def _strip_packaging_and_classifier(coord):
    # We add "pom" into _COURSIER_PACKAGING_TYPES here because "pom" is not a
    # packaging type that Coursier CLI accepts.
    for packaging_type in _COURSIER_PACKAGING_TYPES + ["pom"]:
        coord = coord.replace(":%s:" % packaging_type, ":")
    for classifier_type in ["sources", "natives"]:
        coord = coord.replace(":%s:" % classifier_type, ":")

    return coord

def _strip_packaging_and_classifier_and_version(coord):
    return ":".join(_strip_packaging_and_classifier(coord).split(":")[:-1])

def _escape(string):
    for char in [".", "-", ":", "/", "+"]:
        string = string.replace(char, "_")
    return string.replace("[", "").replace("]", "").split(",")[0]

def _is_windows(repository_ctx):
    return repository_ctx.os.name.find("windows") != -1

def _is_linux(repository_ctx):
    return repository_ctx.os.name.find("linux") != -1

def _is_macos(repository_ctx):
    return repository_ctx.os.name.find("mac") != -1

# The representation of a Windows path when read from the parsed Coursier JSON
# is delimited by 4 back slashes. Replace them with 1 forward slash.
def _normalize_to_unix_path(path):
    return path.replace("\\\\", "/")

# Relativize an absolute path to an artifact in coursier's default cache location.
# After relativizing, also symlink the path into the workspace's output base.
# Then return the relative path for further processing
def _relativize_and_symlink_file(repository_ctx, absolute_path):
    # The path manipulation from here on out assumes *nix paths, not Windows.
    # for artifact_absolute_path in artifact_absolute_paths:
    #
    # Also replace '\' with '/` to normalize windows paths to *nix style paths
    # BUILD files accept only *nix paths, so we normalize them here.
    #
    # We assume that coursier uses the default cache location
    # TODO(jin): allow custom cache locations
    absolute_path_parts = absolute_path.split("v1/")
    if len(absolute_path_parts) != 2:
        fail("Error while trying to parse the path of file in the coursier cache: " + absolute_path)
    else:
        # Make a symlink from the absolute path of the artifact to the relative
        # path within the output_base/external.
        artifact_relative_path = "v1/" + absolute_path_parts[1]
        repository_ctx.symlink(absolute_path, repository_ctx.path(artifact_relative_path))
    return artifact_relative_path

def _genrule_copy_artifact_from_http_file(artifact):
    http_file_repository = _escape(artifact["coord"])
    return "\n".join([
        "genrule(",
        "     name = \"%s_extension\"," % http_file_repository,
        "     srcs = [\"@%s//file\"]," % http_file_repository,
        "     outs = [\"%s\"]," % artifact["file"],
        "     cmd = \"cp $< $@\",",
        ")",
    ])

# Generate BUILD file with java_import and aar_import for each artifact in
# the transitive closure, with their respective deps mapped to the resolved
# tree.
#
# Made function public for testing.
def _generate_imports(repository_ctx, dep_tree, explicit_artifacts, neverlink_artifacts, override_targets):
    # The list of java_import/aar_import declaration strings to be joined at the end
    all_imports = []

    # A dictionary (set) of coordinates. This is to ensure we don't generate
    # duplicate labels
    #
    # seen_imports :: string -> bool
    seen_imports = {}

    # A list of versionless target labels for jar artifacts. This is used for
    # generating a compatibility layer for repositories. For example, if we generate
    # @maven//:junit_junit, we also generate @junit_junit//jar as an alias to it.
    jar_versionless_target_labels = []

    labels_to_override = {}
    for coord in override_targets:
        labels_to_override.update({_escape(coord): override_targets.get(coord)})

    # First collect a map of target_label to their srcjar relative paths, and symlink the srcjars if needed.
    # We will use this map later while generating target declaration strings with the "srcjar" attr.
    srcjar_paths = None
    if repository_ctx.attr.fetch_sources:
        srcjar_paths = {}
        for artifact in dep_tree["dependencies"]:
            if ":sources:" in artifact["coord"]:
                artifact_path = artifact["file"]
                if artifact_path != None and artifact_path not in seen_imports:
                    seen_imports[artifact_path] = True
                    target_label = _escape(_strip_packaging_and_classifier_and_version(artifact["coord"]))
                    srcjar_paths[target_label] = artifact_path
                    if repository_ctx.attr.maven_install_json:
                        all_imports.append(_genrule_copy_artifact_from_http_file(artifact))

    # Iterate through the list of artifacts, and generate the target declaration strings.
    for artifact in dep_tree["dependencies"]:
        artifact_path = artifact["file"]
        simple_coord = _strip_packaging_and_classifier_and_version(artifact["coord"])
        target_label = _escape(simple_coord)
        alias_visibility = ""

        if target_label in seen_imports:
            # Skip if we've seen this target label before. Every versioned artifact is uniquely mapped to a target label.
            pass
        elif repository_ctx.attr.fetch_sources and ":sources:" in artifact["coord"]:
            # We already processed the sources above, so skip them here.
            pass
        elif target_label in labels_to_override:
            # Override target labels with the user provided mapping, instead of generating
            # a jvm_import/aar_import based on information in dep_tree.
            seen_imports[target_label] = True
            all_imports.append(
                "alias(\n\tname = \"%s\",\n\tactual = \"%s\",\n)" % (target_label, labels_to_override.get(target_label)))
            if repository_ctx.attr.maven_install_json:
                # Provide the downloaded artifact as a file target.
                all_imports.append(_genrule_copy_artifact_from_http_file(artifact))
        elif artifact_path != None:
            seen_imports[target_label] = True

            # 1. Generate the rule class.
            #
            # java_import(
            #
            packaging = artifact_path.split(".").pop()
            if packaging == "jar":
                # Regular `java_import` invokes ijar on all JARs, causing some Scala and
                # Kotlin compile interface JARs to be incorrect. We replace java_import
                # with a simple jvm_import Starlark rule that skips ijar.
                target_import_string = ["jvm_import("]
                jar_versionless_target_labels.append(target_label)
            elif packaging == "aar":
                target_import_string = ["aar_import("]
            else:
                fail("Unsupported packaging type: " + packaging)

            # 2. Generate the target label.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library",
            #
            target_import_string.append("\tname = \"%s\"," % target_label)

            # 3. Generate the jars/aar attribute to the relative path of the artifact.
            #    Optionally generate srcjar attr too.
            #
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            #
            if packaging == "jar":
                target_import_string.append("\tjars = [\"%s\"]," % artifact_path)
                if srcjar_paths != None and target_label in srcjar_paths:
                    target_import_string.append("\tsrcjar = \"%s\"," % srcjar_paths[target_label])
            elif packaging == "aar":
                target_import_string.append("\taar = \"%s\"," % artifact_path)

            # 4. Generate the deps attribute with references to other target labels.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core",
            # 	],
            #
            target_import_string.append("\tdeps = [")

            # Dedupe dependencies here. Sometimes coursier will return "x.y:z:aar:version" and "x.y:z:version" in the
            # same list of dependencies.
            target_import_labels = []
            for dep in artifact["dependencies"]:
                dep_target_label = _escape(_strip_packaging_and_classifier_and_version(dep))
                # Coursier returns cyclic dependencies sometimes. Handle it here.
                # See https://github.com/bazelbuild/rules_jvm_external/issues/172
                if dep_target_label != target_label:
                    if dep_target_label in labels_to_override:
                        dep_target_label = labels_to_override.get(dep_target_label)
                    else:
                        dep_target_label = ":" + dep_target_label
                    target_import_labels.append("\t\t\"%s\",\n" % dep_target_label)
            target_import_labels = _deduplicate_list(target_import_labels)

            target_import_string.append("".join(target_import_labels) + "\t],")

            # 5. Add a tag with the original maven coordinates for use generating pom files
            # For use with this rule https://github.com/google/bazel-common/blob/f1115e0f777f08c3cdb115526c4e663005bec69b/tools/maven/pom_file.bzl#L177
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core",
            # 	],
            #   tags = ["maven_coordinates=org.hamcrest:hamcrest.library:1.3"],
            target_import_string.append("\ttags = [\"maven_coordinates=%s\"]," % artifact["coord"])

            # 6. If `neverlink` is True in the artifact spec, add the neverlink attribute to make this artifact
            #    available only as a compile time dependency.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core",
            # 	],
            #   tags = ["maven_coordinates=org.hamcrest:hamcrest.library:1.3"],
            #   neverlink = True,
            if neverlink_artifacts.get(simple_coord):
                target_import_string.append("\tneverlink = True,")

            # 7. If `strict_visibility` is True in the artifact spec, define public
            #    visibility only for non-transitive dependencies.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core",
            # 	],
            #   tags = ["maven_coordinates=org.hamcrest:hamcrest.library:1.3"],
            #   neverlink = True,
            #   visibility = ["//visibility:public"],
            if repository_ctx.attr.strict_visibility and explicit_artifacts.get(simple_coord):
                target_import_string.append("\tvisibility = [\"//visibility:public\"],")
                alias_visibility = "\tvisibility = [\"//visibility:public\"],\n"

            # 8. Finish the java_import rule.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core",
            # 	],
            #   tags = ["maven_coordinates=org.hamcrest:hamcrest.library:1.3"],
            #   neverlink = True,
            # )
            target_import_string.append(")")

            all_imports.append("\n".join(target_import_string))

            # 9. Create a versionless alias target
            #
            # alias(
            #   name = "org_hamcrest_hamcrest_library_1_3",
            #   actual = "org_hamcrest_hamcrest_library",
            # )
            versioned_target_alias_label = _escape(_strip_packaging_and_classifier(artifact["coord"]))
            all_imports.append("alias(\n\tname = \"%s\",\n\tactual = \"%s\",\n%s)" %
                    (versioned_target_alias_label, target_label, alias_visibility))

            # 10. If using maven_install.json, use a genrule to copy the file from the http_file
            # repository into this repository.
            #
            # genrule(
            #     name = "org_hamcrest_hamcrest_library_1_3_extension",
            #     srcs = ["@org_hamcrest_hamcrest_library_1_3//file"],
            #     outs = ["@maven//:v1/https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            #     cmd = "cp $< $@",
            # )
            if repository_ctx.attr.maven_install_json:
                all_imports.append(_genrule_copy_artifact_from_http_file(artifact))

        else: # artifact_path == None:
            # Special case for certain artifacts that only come with a POM file. Such artifacts "aggregate" their dependencies,
            # so they don't have a JAR for download.
            # Note that there are other possible reasons that the artifact_path is None:
            #
            # https://github.com/bazelbuild/rules_jvm_external/issues/70
            # https://github.com/bazelbuild/rules_jvm_external/issues/74
            #
            # This can be due to the artifact being of a type that's unknown to Coursier. This is increasingly
            # rare as we add more types to _COURSIER_PACKAGING_TYPES. It's also increasingly uncommon relatively
            # to POM-only / parent artifacts. So when we encounter an artifact without a filepath, we assume
            # that it's a parent artifact that just exports its dependencies, instead of failing.
            seen_imports[target_label] = True
            target_import_string = ["java_library("]
            target_import_string.append("\tname = \"%s\"," % target_label)
            target_import_string.append("\texports = [")

            target_import_labels = []
            for dep in artifact["dependencies"]:
                dep_target_label = _escape(_strip_packaging_and_classifier_and_version(dep))
                # Coursier returns cyclic dependencies sometimes. Handle it here.
                # See https://github.com/bazelbuild/rules_jvm_external/issues/172
                if dep_target_label != target_label:
                    target_import_labels.append("\t\t\":%s\",\n" % dep_target_label)
            target_import_labels = _deduplicate_list(target_import_labels)

            target_import_string.append("".join(target_import_labels) + "\t],")
            target_import_string.append("\ttags = [\"maven_coordinates=%s\"]," % artifact["coord"])

            if repository_ctx.attr.strict_visibility and explicit_artifacts.get(simple_coord):
                target_import_string.append("\tvisibility = [\"//visibility:public\"],")
                alias_visibility = "\tvisibility = [\"//visibility:public\"],\n"

            target_import_string.append(")")

            all_imports.append("\n".join(target_import_string))

            versioned_target_alias_label = _escape(_strip_packaging_and_classifier(artifact["coord"]))
            all_imports.append("alias(\n\tname = \"%s\",\n\tactual = \"%s\",\n%s)" %
                    (versioned_target_alias_label, target_label, alias_visibility))

    return ("\n".join(all_imports), jar_versionless_target_labels)

def _deduplicate_list(items):
    seen_items = {}
    unique_items = []
    for item in items:
        if item not in seen_items:
            seen_items[item] = True
            unique_items.append(item)
    return unique_items

# Generate the base `coursier` command depending on the OS, JAVA_HOME or the
# location of `java`.
def _generate_coursier_command(repository_ctx):
    coursier = repository_ctx.path("coursier")
    java_home = repository_ctx.os.environ.get("JAVA_HOME")

    if java_home != None:
        # https://github.com/coursier/coursier/blob/master/doc/FORMER-README.md#how-can-the-launcher-be-run-on-windows-or-manually-with-the-java-program
        # The -noverify option seems to be required after the proguarding step
        # of the main JAR of coursier.
        java = repository_ctx.path(java_home + "/bin/java")
        cmd = [java, "-noverify", "-jar"] + _get_java_proxy_args(repository_ctx) + [coursier]
    elif repository_ctx.which("java") != None:
        # Use 'java' from $PATH
        cmd = [repository_ctx.which("java"), "-noverify", "-jar"] + _get_java_proxy_args(repository_ctx) + [coursier]
    else:
        # Try to execute coursier directly
        cmd = [coursier] + ["-J%s" % arg for arg in _get_java_proxy_args(repository_ctx)]

    return cmd

# Extract the well-known environment variables http_proxy, https_proxy and
# no_proxy and convert them to java.net-compatible property arguments.
def _get_java_proxy_args(repository_ctx):
    # Check both lower- and upper-case versions of the environment variables, preferring the former
    http_proxy = repository_ctx.os.environ.get("http_proxy", repository_ctx.os.environ.get("HTTP_PROXY"))
    https_proxy = repository_ctx.os.environ.get("https_proxy", repository_ctx.os.environ.get("HTTPS_PROXY"))
    no_proxy = repository_ctx.os.environ.get("no_proxy", repository_ctx.os.environ.get("NO_PROXY"))
    return get_java_proxy_args(http_proxy, https_proxy, no_proxy)

def _windows_check(repository_ctx):
    # TODO(jin): Remove BAZEL_SH usage ASAP. Bazel is going bashless, so BAZEL_SH
    # will not be around for long.
    #
    # On Windows, run msys once to bootstrap it
    # https://github.com/bazelbuild/rules_jvm_external/issues/53
    if (_is_windows(repository_ctx)):
        bash = repository_ctx.os.environ.get("BAZEL_SH")
        if (bash == None):
            fail("Please set the BAZEL_SH environment variable to the path of MSYS2 bash. " +
                 "This is typically `c:\\msys64\\usr\\bin\\bash.exe`. For more information, read " +
                 "https://docs.bazel.build/versions/master/install-windows.html#getting-bazel")

# Deterministically compute a signature of a list of artifacts in the dependency tree.
# This is to prevent users from manually editing maven_install.json.
def _compute_dependency_tree_signature(artifacts):
    # A collection of elements from the dependency tree to be sorted and hashed
    # into a signature for maven_install.json.
    signature_inputs = []
    for artifact in artifacts:
        artifact_group = []
        artifact_group.append(artifact["coord"])
        if artifact["file"] != None:
            artifact_group.extend([
                artifact["sha256"],
                artifact["file"],
                artifact["url"]
            ])
        if len(artifact["dependencies"]) > 0:
            artifact_group.append(",".join(sorted(artifact["dependencies"])))
        signature_inputs.append(":".join(artifact_group))
    return hash(repr(sorted(signature_inputs)))

def _pinned_coursier_fetch_impl(repository_ctx):
    if not repository_ctx.attr.maven_install_json:
        fail("Please specify the file label to maven_install.json (e.g."
             + "//:maven_install.json).")

    _windows_check(repository_ctx)

    artifacts = []
    for a in repository_ctx.attr.artifacts:
        artifacts.append(json_parse(a))

    # Read Coursier state from maven_install.json.
    repository_ctx.symlink(
        repository_ctx.path(repository_ctx.attr.maven_install_json),
        repository_ctx.path("imported_maven_install.json")
    )
    maven_install_json_content = json_parse(
        repository_ctx.read(
            repository_ctx.path("imported_maven_install.json")),
        fail_on_invalid = False,
    )

    # Validation steps for maven_install.json.

    # First, validate that we can parse the JSON file.
    if maven_install_json_content == None:
        fail("Failed to parse %s. Is this file valid JSON? The file may have been corrupted." % repository_ctx.path(repository_ctx.attr.maven_install_json)
             + "Consider regenerating maven_install.json with the following steps:\n"
             + "  1. Remove the maven_install_json attribute from your `maven_install` declaration for `@%s`.\n" % repository_ctx.name
             + "  2. Regenerate `maven_install.json` by running the command: bazel run @%s//:pin" % repository_ctx.name
             + "  3. Add `maven_install_json = \"//:maven_install.json\"` into your `maven_install` declaration.")

    # Then, validate that there's a dependency_tree element in the parsed JSON.
    if maven_install_json_content.get("dependency_tree") == None:
        fail("Failed to parse %s. " % repository_ctx.path(repository_ctx.attr.maven_install_json)
                + "It is not a valid maven_install.json file. Has this "
                + "file been modified manually?")

    dep_tree = maven_install_json_content["dependency_tree"]

    dep_tree_signature = dep_tree.get("__AUTOGENERATED_FILE_DO_NOT_MODIFY_THIS_FILE_MANUALLY")

    if dep_tree_signature == None:
        print("NOTE: %s_install.json does not contain a signature entry of the dependency tree. " % repository_ctx.name
              + "This feature ensures that the file is not modified manually. To generate this "
              + "signature, run 'bazel run @unpinned_%s//:pin'." % repository_ctx.name)
    elif _compute_dependency_tree_signature(dep_tree["dependencies"]) != dep_tree_signature:
        # Then, validate that the signature provided matches the contents of the dependency_tree.
        # This is to stop users from manually modifying maven_install.json.
        fail("%s_install.json contains an invalid signature and may be corrupted. " % repository_ctx.name
            + "PLEASE DO NOT MODIFY THIS FILE DIRECTLY! To generate a new "
            + "%s_install.json and re-pin the artifacts, follow these steps: \n\n" % repository_ctx.name
            + "  1) In your WORKSPACE file, comment or remove the 'maven_install_json' attribute in 'maven_install'.\n"
            + "  2) Run 'bazel run @%s//:pin'.\n" % repository_ctx.name
            + "  3) Uncomment or re-add the 'maven_install_json' attribute in 'maven_install'.\n\n")

    # Create the list of http_file repositories for each of the artifacts
    # in maven_install.json. This will be loaded additionally like so:
    #
    # load("@maven//:defs.bzl", "pinned_maven_install")
    # pinned_maven_install()
    http_files = [
        "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")",
        "def pinned_maven_install():",
    ]
    for artifact in dep_tree["dependencies"]:
        if artifact.get("url") != None:
            http_file_repository_name = _escape(artifact["coord"])
            http_files.extend([
                "    http_file(",
                "        name = \"%s\"," % http_file_repository_name,
                "        sha256 = \"%s\"," % artifact["sha256"],
            ])
            if artifact.get("mirror_urls") != None:
                http_files.append("        urls = %s," % repr(artifact["mirror_urls"]))
            else:
                # For backwards compatibility. mirror_urls is a field added in a
                # later version than the url field, so not all maven_install.json
                # contains the mirror_urls field.
                http_files.append("        urls = [\"%s\"]," % artifact["url"])
            http_files.append("    )")
    repository_ctx.file("defs.bzl", "\n".join(http_files), executable = False)

    repository_ctx.report_progress("Generating BUILD targets..")
    (generated_imports, jar_versionless_target_labels) = _generate_imports(
        repository_ctx = repository_ctx,
        dep_tree = dep_tree,
        explicit_artifacts = {
            a["group"] + ":" + a["artifact"] + (":" + a["classifier"] if "classifier" in a else ""): True for a in artifacts
        },
        neverlink_artifacts = {
            a["group"] + ":" + a["artifact"] + (":" + a["classifier"] if "classifier" in a else ""): True
            for a in artifacts
            if a.get("neverlink", False)
        },
        override_targets = repository_ctx.attr.override_targets,
    )

    repository_ctx.template(
        "jvm_import.bzl",
        repository_ctx.attr._jvm_import,
        substitutions = {},
        executable = False,  # not executable
    )

    repository_ctx.template(
        "compat_repository.bzl",
        repository_ctx.attr._compat_repository,
        substitutions = {},
        executable = False,  # not executable
    )

    repository_ctx.file(
        "BUILD",
        _BUILD.format(
            visibility = "private" if repository_ctx.attr.strict_visibility else "public",
            repository_name = repository_ctx.name,
            imports = generated_imports,
        ),
        False,  # not executable
    )

    # Generate a compatibility layer of external repositories for all jar artifacts.
    if repository_ctx.attr.generate_compat_repositories:
        compat_repositories_bzl = ["load(\"@%s//:compat_repository.bzl\", \"compat_repository\")" % repository_ctx.name]
        compat_repositories_bzl.append("def compat_repositories():")
        for versionless_target_label in jar_versionless_target_labels:
            compat_repositories_bzl.extend([
                "    compat_repository(",
                "        name = \"%s\"," % versionless_target_label,
                "        generating_repository = \"%s\"," % repository_ctx.name,
                "    )",
            ])
            repository_ctx.file(
                "compat.bzl",
                "\n".join(compat_repositories_bzl) + "\n",
                False,  # not executable
            )

def _coursier_fetch_impl(repository_ctx):
    # Not using maven_install.json, so we resolve and fetch from scratch.
    # This takes significantly longer as it doesn't rely on any local
    # caches and uses Coursier's own download mechanisms.

    # Download Coursier's standalone (deploy) jar from Maven repositories.
    repository_ctx.download([
        COURSIER_CLI_GITHUB_ASSET_URL,
        COURSIER_CLI_BAZEL_MIRROR_URL,
    ], "coursier", sha256 = COURSIER_CLI_SHA256, executable = True)

    # Try running coursier once
    exec_result = repository_ctx.execute(_generate_coursier_command(repository_ctx))
    if exec_result.return_code != 0:
        fail("Unable to run coursier: " + exec_result.stderr)

    _windows_check(repository_ctx)

    # Deserialize the spec blobs
    repositories = []
    for repository in repository_ctx.attr.repositories:
        repositories.append(json_parse(repository))

    artifacts = []
    for a in repository_ctx.attr.artifacts:
        artifacts.append(json_parse(a))

    excluded_artifacts = []
    for a in repository_ctx.attr.excluded_artifacts:
        excluded_artifacts.append(json_parse(a))

    artifact_coordinates = []

    # Set up artifact exclusion, if any. From coursier fetch --help:
    #
    # Path to the local exclusion file. Syntax: <org:name>--<org:name>. `--` means minus. Example file content:
    # com.twitter.penguin:korean-text--com.twitter:util-tunable-internal_2.11
    # org.apache.commons:commons-math--com.twitter.search:core-query-nodes
    # Behavior: If root module A excludes module X, but root module B requires X, module X will still be fetched.
    exclusion_lines = []
    for a in artifacts:
        artifact_coordinates.append(utils.artifact_coordinate(a))
        if "exclusions" in a:
            for e in a["exclusions"]:
                exclusion_lines.append(":".join([a["group"], a["artifact"]]) +
                                       "--" +
                                       ":".join([e["group"], e["artifact"]]))

    cmd = _generate_coursier_command(repository_ctx)
    cmd.extend(["fetch"])
    cmd.extend(artifact_coordinates)
    if repository_ctx.attr.version_conflict_policy == "pinned":
        for coord in artifact_coordinates:
            # Undo any `,classifier=` suffix from `utils.artifact_coordinate`.
            cmd.extend(["--force-version", coord.split(",classifier=")[0]])
    cmd.extend(["--artifact-type", ",".join(_COURSIER_PACKAGING_TYPES + ["src"])])
    cmd.append("--quiet")
    cmd.append("--no-default")
    cmd.extend(["--json-output-file", "dep-tree.json"])

    if repository_ctx.attr.fail_on_missing_checksum:
        cmd.extend(["--checksum", "SHA-1,MD5"])
    else:
        cmd.extend(["--checksum", "SHA-1,MD5,None"])

    if len(exclusion_lines) > 0:
        repository_ctx.file("exclusion-file.txt", "\n".join(exclusion_lines), False)
        cmd.extend(["--local-exclude-file", "exclusion-file.txt"])
    for repository in repositories:
        cmd.extend(["--repository", repository["repo_url"]])
        if "credentials" in repository:
            cmd.extend(["--credentials", utils.repo_credentials(repository)])
    for a in excluded_artifacts:
        cmd.extend(["--exclude", ":".join([a["group"], a["artifact"]])])
    if not repository_ctx.attr.use_unsafe_shared_cache:
        cmd.extend(["--cache", "v1"])  # Download into $output_base/external/$maven_repo_name/v1
    if repository_ctx.attr.fetch_sources:
        cmd.append("--sources")
        cmd.append("--default=true")
    if _is_windows(repository_ctx):
        # Unfortunately on Windows, coursier crashes while trying to acquire the
        # cache's .structure.lock file while running in parallel. This does not
        # happen on *nix.
        cmd.extend(["--parallel", "1"])

    repository_ctx.report_progress("Resolving and fetching the transitive closure of %s artifact(s).." % len(artifact_coordinates))
    exec_result = repository_ctx.execute(cmd, timeout=repository_ctx.attr.resolve_timeout)
    if (exec_result.return_code != 0):
        fail("Error while fetching artifact with coursier: " + exec_result.stderr)

    # Once coursier finishes a fetch, it generates a tree of artifacts and their
    # transitive dependencies in a JSON file. We use that as the source of truth
    # to generate the repository's BUILD file.
    dep_tree = json_parse(repository_ctx.read(repository_ctx.path("dep-tree.json")))

    # Reconstruct the original URLs from the relative path to the artifact,
    # which encodes the URL components for the protocol, domain, and path to
    # the file.
    for artifact in dep_tree["dependencies"]:
        # Some artifacts don't contain files; they are just parent artifacts
        # to other artifacts.
        if artifact["file"] != None:
            # Normalize paths in place here.
            artifact.update({"file": _normalize_to_unix_path(artifact["file"])})

            if repository_ctx.attr.use_unsafe_shared_cache:
                artifact.update({"file": _relativize_and_symlink_file(repository_ctx, artifact["file"])})

            # Coursier saves the artifacts into a subdirectory structure
            # that mirrors the URL where the artifact's fetched from. Using
            # this, we can reconstruct the original URL.
            primary_url_parts = []
            filepath_parts = artifact["file"].split("/")
            protocol = None
            # Only support http/https transports
            for part in filepath_parts:
                if part == "http" or part == "https":
                     protocol = part
            if protocol == None:
                fail("Only artifacts downloaded over http(s) are supported: %s" % artifact["coord"])
            primary_url_parts.extend([protocol, "://"])
            for part in filepath_parts[filepath_parts.index(protocol) + 1:]:
                primary_url_parts.extend([part, "/"])
            primary_url_parts.pop() # pop the final "/"

            # Coursier encodes:
            # - ':' as '%3A'
            # - '@' as '%40'
            #
            # The primary_url is the url from which Coursier downloaded the jar from. It looks like this:
            # https://repo1.maven.org/maven2/org/threeten/threetenbp/1.3.3/threetenbp-1.3.3.jar
            primary_url = "".join(primary_url_parts).replace("%3A", ":").replace("%40", "@")
            artifact.update({"url": primary_url})

            # The repository for the primary_url has to be one of the repositories provided through
            # maven_install. Since Maven artifact URLs are standardized, we can make the `http_file`
            # targets more robust by replicating the primary url for each specified repository url.
            #
            # It does not matter if the artifact is on a repository or not, since http_file takes
            # care of 404s.
            #
            # If the artifact does exist, Bazel's HttpConnectorMultiplexer enforces the SHA-256 checksum
            # is correct. By applying the SHA-256 checksum verification across all the mirrored files,
            # we get increased robustness in the case where our primary artifact has been tampered with,
            # and we somehow ended up using the tampered checksum. Attackers would need to tamper *all*
            # mirrored artifacts.
            #
            # See https://github.com/bazelbuild/bazel/blob/77497817b011f298b7f3a1138b08ba6a962b24b8/src/main/java/com/google/devtools/build/lib/bazel/repository/downloader/HttpConnectorMultiplexer.java#L103
            # for more information on how Bazel's HTTP multiplexing works.
            #
            # TODO(https://github.com/bazelbuild/rules_jvm_external/issues/186): Make this work with
            # basic auth.
            repository_urls = [r["repo_url"] for r in repositories]
            for url in repository_urls:
                if primary_url.find(url) != -1:
                    primary_artifact_path = primary_url[len(url):]
                elif url.find("@") != -1 and primary_url.find(url[url.rindex("@"):]) != -1:
                    # Maybe this is a url-encoded private repository url.
                    #
                    # A private repository url looks like this:
                    # http://admin:passw@rd@localhost/artifactory/jcenter
                    #
                    # Or, in its URL encoded form:
                    # http://admin:passw%40rd@localhost/artifactory/jcenter
                    #
                    # However, in the primary_url we've reconstructed using the
                    # downloaded relative file path earlier on, the password is
                    # removed by Coursier (as it should). So we end up working
                    # with: http://admin@localhost/artifactory/jcenter
                    #
                    # So, we use rfind to get the index of the final '@' in the
                    # repository url instead.
                    primary_artifact_path = primary_url[len(url):]
                elif url.find("@") == -1 and primary_url.find("@") != -1:
                    username_len = primary_url.find("//") + 2 + primary_url.find("@") + 1
                    primary_artifact_path = primary_url[len(url) + username_len:]

            mirror_urls = [url + primary_artifact_path for url in repository_urls]
            artifact.update({"mirror_urls": mirror_urls})

            # Compute the sha256 of the file.
            exec_result = repository_ctx.execute([
                "python",
                repository_ctx.path(repository_ctx.attr._sha256_tool),
                repository_ctx.path(artifact["file"]),
                "artifact.sha256",
            ])

            if exec_result.return_code != 0:
                fail("Error while obtaining the sha256 checksum of "
                        + artifact["file"] + ": " + exec_result.stderr)

            # Update the SHA-256 checksum in-place.
            artifact.update({"sha256": repository_ctx.read("artifact.sha256")})

    dep_tree.update({
        "__AUTOGENERATED_FILE_DO_NOT_MODIFY_THIS_FILE_MANUALLY": _compute_dependency_tree_signature(dep_tree["dependencies"])
    })

    repository_ctx.report_progress("Generating BUILD targets..")
    (generated_imports, jar_versionless_target_labels) = _generate_imports(
        repository_ctx = repository_ctx,
        dep_tree = dep_tree,
        explicit_artifacts = {
            a["group"] + ":" + a["artifact"] + (":" + a["classifier"] if "classifier" in a else ""): True
            for a in artifacts
        },
        neverlink_artifacts = {
            a["group"] + ":" + a["artifact"] + (":" + a["classifier"] if "classifier" in a else ""): True
            for a in artifacts
            if a.get("neverlink", False)
        },
        override_targets = repository_ctx.attr.override_targets,
    )

    repository_ctx.template(
        "jvm_import.bzl",
        repository_ctx.attr._jvm_import,
        substitutions = {},
        executable = False,  # not executable
    )

    repository_ctx.file(
        "BUILD",
        _BUILD.format(
            visibility = "private" if repository_ctx.attr.strict_visibility else "public",
            repository_name = repository_ctx.name,
            imports = generated_imports,
        ),
        False,  # not executable
    )


    # This repository rule can be either in the pinned or unpinned state, depending on when
    # the user invokes artifact pinning. Normalize the repository name here.
    if repository_ctx.name.startswith("unpinned_"):
        repository_name = repository_ctx.name[len("unpinned_"):]
    else:
        repository_name = repository_ctx.name

    # If maven_install.json has already been used in maven_install,
    # we don't need to instruct user to update WORKSPACE and load pinned_maven_install.
    # If maven_install.json is not used yet, provide complete instructions.
    #
    # Also support custom locations for maven_install.json and update the pin.sh script
    # accordingly.
    predefined_maven_install = bool(repository_ctx.attr.maven_install_json)
    if predefined_maven_install:
        package_path = repository_ctx.attr.maven_install_json.package
        file_name = repository_ctx.attr.maven_install_json.name
        if package_path == "":
            maven_install_location = file_name # e.g. some.json
        else:
            maven_install_location = "/".join([package_path, file_name]) # e.g. path/to/some.json
    else:
        # Default maven_install.json file name.
        maven_install_location = "{repository_name}_install.json"

    # Expose the script to let users pin the state of the fetch in
    # `<workspace_root>/maven_install.json`.
    #
    # $ bazel run @unpinned_maven//:pin
    #
    # Create the maven_install.json export script for unpinned repositories.
    dependency_tree_json = "{ \"dependency_tree\": " + repr(dep_tree).replace("None", "null") + "}"
    repository_ctx.template("pin", repository_ctx.attr._pin,
        {
            "{maven_install_location}": "$BUILD_WORKSPACE_DIRECTORY/" + maven_install_location,
            "{predefined_maven_install}": str(predefined_maven_install),
            "{dependency_tree_json}": dependency_tree_json,
            "{repository_name}": repository_name,
        },
        executable = True,
    )

    # Generate a dummy 'defs.bzl' in the case where users have this
    # in their WORKSPACE:
    #
    # maven_install(
    #     name = "maven",
    #     # maven_install_json = "//:maven_install.json",
    # )
    # load("@maven//:defs.bzl", "pinned_maven_install")
    # pinned_maven_install()
    #
    # This scenario happens when users have a modified/corrupted
    # 'maven_install.json' and wishes to re-generate one. Instead
    # of asking users to also remove the load statement for
    # pinned_maven_install(), we generate a dummy defs.bzl
    # here so that builds will not fail due to a missing load.
    repository_ctx.file(
        "defs.bzl",
        "def pinned_maven_install():\n    pass",
        executable = False
    )

    # Generate a compatibility layer of external repositories for all jar artifacts.
    if repository_ctx.attr.generate_compat_repositories:
        repository_ctx.template(
            "compat_repository.bzl",
            repository_ctx.attr._compat_repository,
            substitutions = {},
            executable = False,  # not executable
        )

        compat_repositories_bzl = ["load(\"@%s//:compat_repository.bzl\", \"compat_repository\")" % repository_ctx.name]
        compat_repositories_bzl.append("def compat_repositories():")
        for versionless_target_label in jar_versionless_target_labels:
            compat_repositories_bzl.extend([
                "    compat_repository(",
                "        name = \"%s\"," % versionless_target_label,
                "        generating_repository = \"%s\"," % repository_ctx.name,
                "    )",
            ])
        repository_ctx.file(
            "compat.bzl",
            "\n".join(compat_repositories_bzl) + "\n",
            False,  # not executable
        )

pinned_coursier_fetch = repository_rule(
    attrs = {
        "_jvm_import": attr.label(default = "//:private/jvm_import.bzl"),
        "_compat_repository": attr.label(default = "//:private/compat_repository.bzl"),
        "artifacts": attr.string_list(),  # list of artifact objects, each as json
        "fetch_sources": attr.bool(default = False),
        "generate_compat_repositories": attr.bool(default = False),  # generate a compatible layer with repositories for each artifact
        "maven_install_json": attr.label(allow_single_file = True),
        "override_targets": attr.string_dict(default = {}),
        "strict_visibility": attr.bool(
            doc = """Controls visibility of transitive dependencies.

            If "True", transitive dependencies are private and invisible to user's rules.
            If "False", transitive dependencies are public and visible to user's rules.
            """,
            default = False,
        ),
    },
    implementation = _pinned_coursier_fetch_impl,
)

coursier_fetch = repository_rule(
    attrs = {
        "_sha256_tool": attr.label(default = "@bazel_tools//tools/build_defs/hash:sha256.py"),
        "_jvm_import": attr.label(default = "//:private/jvm_import.bzl"),
        "_pin": attr.label(default = "//:private/pin.sh"),
        "_compat_repository": attr.label(default = "//:private/compat_repository.bzl"),
        "repositories": attr.string_list(),  # list of repository objects, each as json
        "artifacts": attr.string_list(),  # list of artifact objects, each as json
        "fail_on_missing_checksum": attr.bool(default = True),
        "fetch_sources": attr.bool(default = False),
        "use_unsafe_shared_cache": attr.bool(default = False),
        "excluded_artifacts": attr.string_list(default = []),  # list of artifacts to exclude
        "generate_compat_repositories": attr.bool(default = False),  # generate a compatible layer with repositories for each artifact
        "version_conflict_policy": attr.string(
            doc = """Policy for user-defined vs. transitive dependency version conflicts

            If "pinned", choose the user-specified version in maven_install unconditionally.
            If "default", follow Coursier's default policy.
            """,
            default = "default",
            values = [
                "default",
                "pinned",
            ],
        ),
        "maven_install_json": attr.label(allow_single_file = True),
        "override_targets": attr.string_dict(default = {}),
        "strict_visibility": attr.bool(
            doc = """Controls visibility of transitive dependencies

            If "True", transitive dependencies are private and invisible to user's rules.
            If "False", transitive dependencies are public and visible to user's rules.
            """,
            default = False,
        ),
        "resolve_timeout": attr.int(default = 600),
    },
    environ = [
        "JAVA_HOME",
        "http_proxy",
        "HTTP_PROXY",
        "https_proxy",
        "HTTPS_PROXY",
        "no_proxy",
        "NO_PROXY",
    ],
    implementation = _coursier_fetch_impl,
)
