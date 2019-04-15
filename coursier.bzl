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
load("//:specs.bzl", "parse", "utils")
load("//:private/versions.bzl",
     "COURSIER_CLI_MAVEN_PATH",
     "COURSIER_CLI_SHA256",
)
load("//:private/special_artifacts.bzl",
     "POM_ONLY_ARTIFACTS",
)

_BUILD = """
package(default_visibility = ["//visibility:public"])

load("@{repository_name}//:jvm_import.bzl", "jvm_import")

{imports}
"""

# Coursier uses these types to determine what files it should resolve and fetch.
# For example, some jars have the type "eclipse-plugin", and Coursier would not
# download them if it's not asked to to resolve "eclipse-plugin".
_COURSIER_PACKAGING_TYPES = ["jar", "aar", "bundle", "eclipse-plugin"]

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
    return string.replace(".", "_").replace("-", "_").replace(":", "_").replace("/", "_").replace("[", "").replace("]", "").split(",")[0]

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
    absolute_path_parts = _normalize_to_unix_path(absolute_path).split("v1/")
    if len(absolute_path_parts) != 2:
        fail("Error while trying to parse the path of file in the coursier cache: " + absolute_path)
    else:
        # Make a symlink from the absolute path of the artifact to the relative
        # path within the output_base/external.
        artifact_relative_path = "v1/" + absolute_path_parts[1]
        repository_ctx.symlink(absolute_path, repository_ctx.path(artifact_relative_path))
    return artifact_relative_path

# Get the reverse dependencies of an artifact from the Coursier parsed
# dependency tree.
def _get_reverse_deps(coord, dep_tree):
    reverse_deps = []

    # For all potential reverse dep artifacts,
    for maybe_rdep in dep_tree["dependencies"]:
        # For all dependencies of this artifact,
        for maybe_rdep_coord in maybe_rdep["dependencies"]:
            # If this artifact depends on the missing artifact,
            if maybe_rdep_coord == coord:
                # Then this artifact is an rdep :-)
                reverse_deps.append(maybe_rdep)
    return reverse_deps

# Generate BUILD file with java_import and aar_import for each artifact in
# the transitive closure, with their respective deps mapped to the resolved
# tree.
#
# Made function public for testing.
def generate_imports(repository_ctx, dep_tree, srcs_dep_tree = None):
    # The list of java_import/aar_import declaration strings to be joined at the end
    all_imports = []

    # A dictionary (set) of coordinates. This is to ensure we don't generate
    # duplicate labels
    #
    # seen_imports :: string -> bool
    seen_imports = {}

    # First collect a map of target_label to their srcjar relative paths, and symlink the srcjars if needed.
    # We will use this map later while generating target declaration strings with the "srcjar" attr.
    srcjar_paths = None
    if srcs_dep_tree != None:
        srcjar_paths = {}
        for artifact in srcs_dep_tree["dependencies"]:
            artifact_path = artifact["file"]
            if artifact_path != None and artifact_path not in seen_imports:
                seen_imports[artifact_path] = True
                if repository_ctx.attr.use_unsafe_shared_cache:
                    # If using unsafe shared cache, the path is absolute to the artifact in $COURSIER_CACHE
                    artifact_relative_path = _relativize_and_symlink_file(repository_ctx, artifact_path)
                else:
                    # If not, it's a relative path to the one in output_base/external/$maven/v1/...
                    artifact_relative_path = _normalize_to_unix_path(artifact_path)
                target_label = _escape(_strip_packaging_and_classifier(artifact["coord"]))
                srcjar_paths[target_label] = artifact_relative_path
    # Iterate through the list of artifacts, and generate the target declaration strings.
    for artifact in dep_tree["dependencies"]:
        artifact_path = artifact["file"]
        target_label = _escape(_strip_packaging_and_classifier(artifact["coord"]))

        # Skip if we've seen this target label before. Every versioned artifact is uniquely mapped to a target label.
        if target_label not in seen_imports and artifact_path != None:
            seen_imports[target_label] = True

            if repository_ctx.attr.use_unsafe_shared_cache:
                # If using unsafe shared cache, the path is absolute to the artifact in $COURSIER_CACHE
                artifact_relative_path = _relativize_and_symlink_file(repository_ctx, artifact_path)
            else:
                # If not, it's a relative path to the one in output_base/external/$maven/v1/...
                artifact_relative_path = _normalize_to_unix_path(artifact_path)

            # 1. Generate the rule class.
            #
            # java_import(
            #
            packaging = artifact_relative_path.split(".").pop()
            if packaging == "jar":
                # Regular `java_import` invokes ijar on all JARs, causing some Scala and
                # Kotlin compile interface JARs to be incorrect. We replace java_import
                # with a simple jvm_import Starlark rule that skips ijar.
                target_import_string = ["jvm_import("]
            elif packaging == "aar":
                target_import_string = ["aar_import("]
            else:
                fail("Unsupported packaging type: " + packaging)

            # 2. Generate the target label.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library_1_3",
            #
            target_import_string.append("\tname = \"%s\"," % target_label)

            # 3. Generate the jars/aar attribute to the relative path of the artifact.
            #    Optionally generate srcjar attr too.
            #
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library_1_3",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            #
            if packaging == "jar":
                target_import_string.append("\tjars = [\"%s\"]," % artifact_relative_path)
                if srcjar_paths != None and target_label in srcjar_paths:
                    target_import_string.append("\tsrcjar = \"%s\"," % srcjar_paths[target_label])
            elif packaging == "aar":
                target_import_string.append("\taar = \"%s\"," % artifact_relative_path)

            # 4. Generate the deps attribute with references to other target labels.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library_1_3",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core_1_3",
            # 	],
            #
            target_import_string.append("\tdeps = [")

            # Dedupe dependencies here. Sometimes coursier will return "x.y:z:aar:version" and "x.y:z:version" in the
            # same list of dependencies.
            target_import_labels = []
            for dep in artifact["dependencies"]:
                dep_target_label = _escape(_strip_packaging_and_classifier(dep))
                target_import_labels.append("\t\t\":%s\",\n" % dep_target_label)
            target_import_labels = _deduplicate_list(target_import_labels)

            target_import_string.append("".join(target_import_labels) + "\t],")

            # 5. Add a tag with the original maven coordinates for use generating pom files
            # For use with this rule https://github.com/google/bazel-common/blob/f1115e0f777f08c3cdb115526c4e663005bec69b/tools/maven/pom_file.bzl#L177
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library_1_3",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core_1_3",
            # 	],
            #   tags = ["maven_coordinates=org.hamcrest:hamcrest.library:1.3"],
            target_import_string.append("\ttags = [\"maven_coordinates=%s\"]," % artifact["coord"])

            # 6. Finish the java_import rule.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library_1_3",
            # 	jars = ["https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3.jar"],
            # 	srcjar = "https/repo1.maven.org/maven2/org/hamcrest/hamcrest-library/1.3/hamcrest-library-1.3-sources.jar",
            # 	deps = [
            # 		":org_hamcrest_hamcrest_core_1_3",
            # 	],
            # )
            target_import_string.append(")")

            all_imports.append("\n".join(target_import_string))

            # 7. Create a versionless alias target
            #
            # alias(
            #   name = "org_hamcrest_hamcrest_library",
            #   actual = "org_hamcrest_hamcrest_library_1_3",
            # )
            versionless_target_alias_label = _escape(_strip_packaging_and_classifier_and_version(artifact["coord"]))
            all_imports.append("alias(\n\tname = \"%s\",\n\tactual = \"%s\",\n)" % (versionless_target_alias_label, target_label))

        elif artifact_path == None and POM_ONLY_ARTIFACTS.get(_strip_packaging_and_classifier_and_version(artifact["coord"])):
            # Special case for certain artifacts that only come with a POM file. Such artifacts "aggregate" their dependencies,
            # so they don't have a JAR for download.
            if target_label not in seen_imports:
                seen_imports[target_label] = True
                target_import_string = ["java_library("]
                target_import_string.append("\tname = \"%s\"," % target_label)
                target_import_string.append("\texports = [")

                target_import_labels = []
                for dep in artifact["dependencies"]:
                    dep_target_label = _escape(_strip_packaging_and_classifier(dep))
                    target_import_labels.append("\t\t\":%s\",\n" % dep_target_label)
                target_import_labels = _deduplicate_list(target_import_labels)

                target_import_string.append("".join(target_import_labels) + "\t],")
                target_import_string.append("\ttags = [\"maven_coordinates=%s\"]," % artifact["coord"])
                target_import_string.append(")")

                all_imports.append("\n".join(target_import_string))

                versionless_target_alias_label = _escape(_strip_packaging_and_classifier_and_version(artifact["coord"]))
                all_imports.append("alias(\n\tname = \"%s\",\n\tactual = \"%s\",\n)" % (versionless_target_alias_label, target_label))

        elif artifact_path == None:
            # Possible reasons that the artifact_path is None:
            #
            # https://github.com/bazelbuild/rules_jvm_external/issues/70
            # https://github.com/bazelbuild/rules_jvm_external/issues/74

            # Get the reverse deps of the missing artifact.
            reverse_deps = _get_reverse_deps(artifact["coord"], dep_tree)
            reverse_dep_coords = [reverse_dep["coord"] for reverse_dep in reverse_deps]
            reverse_dep_pom_paths = [
                repository_ctx.path(reverse_dep["file"].replace(".jar", ".pom").replace(".aar", ".pom"))
                for reverse_dep in reverse_deps
            ]

            error_message = """
The artifact for {artifact} was not downloaded. Perhaps its packaging type is
not one of: {packaging_types}?

It is also possible that the packaging type of {artifact} is specified
incorrectly in the POM file of an artifact that depends on it. For example,
{artifact} may be an AAR, but the dependent's POM file specified its `<type>`
value to be a JAR.

The artifact(s) depending on {artifact} are:

{reverse_dep_coords}

and their POM files are located at:

{reverse_dep_pom_paths}

---

Parsed artifact data: {parsed_artifact}""".format(
                artifact = artifact["coord"],
                packaging_types = ",".join(_COURSIER_PACKAGING_TYPES),
                reverse_dep_coords = "\n".join(reverse_dep_coords),
                reverse_dep_pom_paths = "\n".join(reverse_dep_pom_paths),
                parsed_artifact = repr(artifact),
            )

            fail(error_message)

    return "\n".join(all_imports)

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

    proxy_args = []

    # Extract the host and port from a standard proxy URL:
    # http://proxy.example.com:3128 -> ["proxy.example.com", "3128"]
    if http_proxy != None:
        proxy = http_proxy.split("://", 1)[1].split(":", 1)
        proxy_args.extend([
            "-Dhttp.proxyHost=%s" % proxy[0],
            "-Dhttp.proxyPort=%s" % proxy[1],
        ])

    if https_proxy != None:
        proxy = https_proxy.split("://", 1)[1].split(":", 1)
        proxy_args.extend([
            "-Dhttps.proxyHost=%s" % proxy[0],
            "-Dhttps.proxyPort=%s" % proxy[1],
        ])

    # Convert no_proxy-style exclusions, including base domain matching, into java.net nonProxyHosts:
    # localhost,example.com,foo.example.com,.otherexample.com -> "localhost|example.com|foo.example.com|*.otherexample.com"
    if no_proxy != None:
        proxy_args.append("-Dhttp.nonProxyHosts=%s" % no_proxy.replace(",", "|").replace("|.", "|*."))

    return proxy_args

def _cat_file(repository_ctx, filepath):
    if (_is_windows(repository_ctx)):
        exec_result = repository_ctx.execute([
            repository_ctx.os.environ.get("BAZEL_SH"),
            "-lc",
            "cat " + str(repository_ctx.path(filepath)),
        ])
    else:
        exec_result = repository_ctx.execute([
            repository_ctx.which("cat"),
            repository_ctx.path(filepath),
        ])
    if (exec_result.return_code != 0):
        fail("Error while trying to read %s: %s" % (filepath, exec_result.stderr))
    return exec_result.stdout

def _coursier_fetch_impl(repository_ctx):
    # Download Coursier's standalone (deploy) jar from Maven repositories.
    repository_ctx.download([
        "https://jcenter.bintray.com/" + COURSIER_CLI_MAVEN_PATH,
        "http://central.maven.org/maven2/" + COURSIER_CLI_MAVEN_PATH
    ], "coursier", sha256 = COURSIER_CLI_SHA256, executable = True)

    # Try running coursier once
    exec_result = repository_ctx.execute(_generate_coursier_command(repository_ctx))
    if exec_result.return_code != 0:
        fail("Unable to run coursier: " + exec_result.stderr)

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
        repository_ctx.execute([bash, "-lc", "echo", "works"])

    # Deserialize the spec blobs
    repositories = []
    for repository in repository_ctx.attr.repositories:
        repositories.append(json_parse(repository))

    artifacts = []
    for a in repository_ctx.attr.artifacts:
        artifacts.append(json_parse(a))

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
    cmd.extend(["--artifact-type", ",".join(_COURSIER_PACKAGING_TYPES + ["src"])])
    cmd.append("--quiet")
    cmd.append("--no-default")
    cmd.extend(["--json-output-file", "dep-tree.json"])
    if len(exclusion_lines) > 0:
        repository_ctx.file("exclusion-file.txt", "\n".join(exclusion_lines), False)
        cmd.extend(["--local-exclude-file", "exclusion-file.txt"])
    for repository in repositories:
        cmd.extend(["--repository", utils.repo_url(repository)])
    if not repository_ctx.attr.use_unsafe_shared_cache:
        cmd.extend(["--cache", "v1"])  # Download into $output_base/external/$maven_repo_name/v1
    if _is_windows(repository_ctx):
        # Unfortunately on Windows, coursier crashes while trying to acquire the
        # cache's .structure.lock file while running in parallel. This does not
        # happen on *nix.
        cmd.extend(["--parallel", "1"])

    repository_ctx.report_progress("Resolving and fetching the transitive closure of %s artifact(s).." % len(artifact_coordinates))
    exec_result = repository_ctx.execute(cmd)
    if (exec_result.return_code != 0):
        fail("Error while fetching artifact with coursier: " + exec_result.stderr)

    # Once coursier finishes a fetch, it generates a tree of artifacts and their
    # transitive dependencies in a JSON file. We use that as the source of truth
    # to generate the repository's BUILD file.
    dep_tree = json_parse(_cat_file(repository_ctx, "dep-tree.json"))

    srcs_dep_tree = None
    if repository_ctx.attr.fetch_sources:
        cmd = _generate_coursier_command(repository_ctx)
        cmd.extend(["fetch"])
        cmd.extend(artifact_coordinates)
        cmd.append("--quiet")
        cmd.append("--no-default")
        cmd.extend(["--sources", "true"])
        cmd.extend(["--json-output-file", "src-dep-tree.json"])
        if len(exclusion_lines) > 0:
            cmd.extend(["--local-exclude-file", "exclusion-file.txt"])
            repository_ctx.file("exclusion-file.txt", "\n".join(exclusion_lines), False)
        for repository in repositories:
            cmd.extend(["--repository", utils.repo_url(repository)])
        if not repository_ctx.attr.use_unsafe_shared_cache:
            cmd.extend(["--cache", "v1"])  # Download into $output_base/external/$maven_repo_name/v1
        exec_result = repository_ctx.execute(cmd)
        if (exec_result.return_code != 0):
            fail("Error while fetching artifact sources with coursier: " +
                 exec_result.stderr)
        srcs_dep_tree = json_parse(_cat_file(repository_ctx, "src-dep-tree.json"))

    repository_ctx.report_progress("Generating BUILD targets..")
    generated_imports = generate_imports(
        repository_ctx = repository_ctx,
        dep_tree = dep_tree,
        srcs_dep_tree = srcs_dep_tree,
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
            repository_name = repository_ctx.name,
            imports = generated_imports,
        ),
        False,  # not executable
    )

coursier_fetch = repository_rule(
    attrs = {
        "_jvm_import": attr.label(default = "//:private/jvm_import.bzl"),
        "repositories": attr.string_list(),  # list of repository objects, each as json
        "artifacts": attr.string_list(),  # list of artifact objects, each as json
        "fetch_sources": attr.bool(default = False),
        "use_unsafe_shared_cache": attr.bool(default = False),
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
