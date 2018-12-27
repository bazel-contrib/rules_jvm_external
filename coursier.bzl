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

_BUILD = """
package(default_visibility = ["//visibility:public"])
{imports}
"""

# Super hacky :(
def _strip_packaging_and_classifier(coord):
    return coord.replace(":jar:", ":").replace(":aar:", ":").replace(":sources:", ":")

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
    absolute_path_parts = absolute_path.replace("\\\\", "/").split("v1/")
    if len(absolute_path_parts) != 2:
        fail("Error while trying to parse the path of file in the coursier cache: " + absolute_path)
    else:
        # Make a symlink from the absolute path of the artifact to the relative
        # path within the output_base/external.
        artifact_relative_path = absolute_path_parts[1]
        repository_ctx.symlink(absolute_path, repository_ctx.path(artifact_relative_path))
    return artifact_relative_path

# Generate BUILD file with java_import and aar_import for each artifact in
# the transitive closure, with their respective deps mapped to the resolved
# tree.
#
# Made function public for testing.
def generate_imports(repository_ctx, dep_tree, srcs_dep_tree = None):
    # The list of java_import/aar_import declaration strings to be joined at the end
    all_imports = []

    # A mapping of FQN to the artifact's sha256 checksum
    checksums = {}

    # A dictionary (set) of coordinates. This is to ensure we don't generate
    # duplicate labels
    #
    # seen_imports :: string -> bool
    seen_imports = {}

    # First collect a map of target_label to their srcjar relative paths, and symlink the srcjars.
    # We will use this map later while generating target declaration strings with the "srcjar" attr.
    srcjar_paths = None
    if srcs_dep_tree != None:
        srcjar_paths = {}
        for artifact in srcs_dep_tree["dependencies"]:
            absolute_path_to_artifact = artifact["file"]
            if absolute_path_to_artifact != None and absolute_path_to_artifact not in seen_imports:
                seen_imports[absolute_path_to_artifact] = True
                artifact_relative_path = _relativize_and_symlink_file(repository_ctx, absolute_path_to_artifact)
                target_label = _escape(_strip_packaging_and_classifier(artifact["coord"]))
                srcjar_paths[target_label] = artifact_relative_path

    # Iterate through the list of artifacts, and generate the target declaration strings.
    for artifact in dep_tree["dependencies"]:
        absolute_path_to_artifact = artifact["file"]
        # Skip if we've seen this absolute path before.
        if absolute_path_to_artifact not in seen_imports and absolute_path_to_artifact != None:
            seen_imports[absolute_path_to_artifact] = True

            # We don't set the path of the artifact in resolved.bzl because it's different on everyone's machines
            checksums[artifact["coord"]] = {}

            if _is_macos(repository_ctx):
                sha256 = repository_ctx.execute([
                    "bash", "-c",
                    "shasum -a256 "
                    + artifact["file"]
                    + "| cut -d\" \" -f1 | tr -d '\n'"
                ]).stdout
                checksums[artifact["coord"]]["sha256"] = sha256

            artifact_relative_path = _relativize_and_symlink_file(repository_ctx, absolute_path_to_artifact)

            # 1. Generate the rule class.
            #
            # java_import(
            #
            packaging = artifact_relative_path.split(".").pop()
            if packaging == "jar":
                target_import_string = ["java_import("]
            elif packaging == "aar":
                target_import_string = ["aar_import("]
            else:
                fail("Unsupported packaging type: " + packaging)

            # 2. Generate the target label.
            #
            # java_import(
            # 	name = "org_hamcrest_hamcrest_library_1_3",
            #
            target_label = _escape(_strip_packaging_and_classifier(artifact["coord"]))
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
            artifact_deps = artifact["dependencies"]
            # Dedupe dependencies here. Sometimes coursier will return "x.y:z:aar:version" and "x.y:z:version" in the
            # same list of dependencies.
            seen_dep_labels = {}
            for dep in artifact_deps:
                dep_target_label = _escape(_strip_packaging_and_classifier(dep))
                if dep_target_label not in seen_dep_labels:
                    seen_dep_labels[dep_target_label] = True
                    target_import_string.append("\t\t\":%s\"," % dep_target_label)
            target_import_string.append("\t],")

            # 5. Conclude.
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

            # Also create a versionless alias target
            target_alias_label = _escape(_strip_packaging_and_classifier_and_version(artifact["coord"]))
            all_imports.append("alias(\n\tname = \"%s\",\n\tactual = \"%s\",\n)" % (target_alias_label, target_label))


        elif absolute_path_to_artifact == None:
            fail("The artifact for " +
                 artifact["coord"] +
                 " was not downloaded. Perhaps the packaging type is not one of: jar, aar, bundle?\n" +
                 "Parsed artifact data:" + repr(artifact))

    return ("\n".join(all_imports), checksums)

# Generate the base `coursier` command depending on the OS, JAVA_HOME or the
# location of `java`.
def _generate_coursier_command(repository_ctx):
    coursier = repository_ctx.path(repository_ctx.attr._coursier)
    java_home = repository_ctx.os.environ.get("JAVA_HOME")

    if java_home != None:
        # https://github.com/coursier/coursier/blob/master/doc/FORMER-README.md#how-can-the-launcher-be-run-on-windows-or-manually-with-the-java-program
        # The -noverify option seems to be required after the proguarding step
        # of the main JAR of coursier.
        java = repository_ctx.path(java_home + "/bin/java")
        cmd = [java, "-noverify", "-jar", coursier]
    elif repository_ctx.which("java") != None:
        # Use 'java' from $PATH
        cmd = [repository_ctx.which("java"), "-noverify", "-jar", coursier]
    else:
        # Try to execute coursier directly
        cmd = [coursier]

    return cmd

def _cat_file(repository_ctx, filepath):
    # For Windows, use cat from msys.
    # TODO(jin): figure out why we can't just use "type". CreateProcessW complains that "type" can't be found.
    cat = "C:\\msys64\\usr\\bin\\cat" if (_is_windows(repository_ctx)) else repository_ctx.which("cat")
    exec_result = repository_ctx.execute([cat, repository_ctx.path(filepath)])
    if (exec_result.return_code != 0):
        fail("Error while trying to read %s: %s" % (filepath, exec_result.stderr))
    return exec_result.stdout

def _coursier_fetch_impl(repository_ctx):
    # Try running coursier once
    exec_result = repository_ctx.execute(_generate_coursier_command(repository_ctx))
    if exec_result.return_code != 0:
        fail("Unable to run coursier: " + exec_result.stderr)

    cmd = _generate_coursier_command(repository_ctx)
    cmd.extend(["fetch"])
    cmd.extend(repository_ctx.attr.artifacts)
    cmd.extend(["--artifact-type", "jar,aar,bundle"])
    cmd.append("--quiet")
    cmd.append("--no-default")
    cmd.extend(["--json-output-file", "dep-tree.json"])
    for repository in repository_ctx.attr.repositories:
        cmd.extend(["--repository", repository])
    if _is_windows(repository_ctx):
        # Unfortunately on Windows, coursier crashes while trying to acquire the
        # cache's .structure.lock file while running in parallel. This does not
        # happen on *nix.
        cmd.extend(["--parallel", "1"])

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
        cmd.extend(repository_ctx.attr.artifacts)
        cmd.extend(["--artifact-type", "jar,aar,bundle,src"])
        cmd.append("--quiet")
        cmd.append("--no-default")
        cmd.extend(["--sources", "true"])
        cmd.extend(["--json-output-file", "src-dep-tree.json"])
        for repository in repository_ctx.attr.repositories:
            cmd.extend(["--repository", repository])
        exec_result = repository_ctx.execute(cmd)
        if (exec_result.return_code != 0):
            fail("Error while fetching artifact sources with coursier: "
                 + exec_result.stderr)
        srcs_dep_tree = json_parse(_cat_file(repository_ctx, "src-dep-tree.json"))

    (generated_imports, checksums) = generate_imports(
        repository_ctx = repository_ctx,
        dep_tree = dep_tree,
        srcs_dep_tree = srcs_dep_tree,
    )

    repository_ctx.file(
        "BUILD",
        _BUILD.format(imports = generated_imports),
        False,  # not executable
    )

    # Disable repository resolution behind a private feature flag
    if repository_ctx.attr._verify_checksums:
        return {
            "name": repository_ctx.attr.name,
            "repositories": repository_ctx.attr.repositories,
            "artifacts": repository_ctx.attr.artifacts,
            "fetch_sources": repository_ctx.attr.fetch_sources,
            "checksums": checksums,
        }

coursier_fetch = repository_rule(
    attrs = {
        "_coursier": attr.label(default = "//:third_party/coursier/coursier"),  # vendor coursier, it's just a jar
        "repositories": attr.string_list(),  # list of repositories
        "artifacts": attr.string_list(),
        "fetch_sources": attr.bool(default = False),
        "_verify_checksums": attr.bool(default = False),
    },
    environ = ["JAVA_HOME"],
    implementation = _coursier_fetch_impl,
)
