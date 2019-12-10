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
load("//:private/proxy.bzl", "get_java_proxy_args")
load("//:private/dependency_tree_parser.bzl", "parser")
load("//:private/coursier_utilities.bzl", "SUPPORTED_PACKAGING_TYPES", "escape")
load(
    "//:private/versions.bzl",
    "COURSIER_CLI_BAZEL_MIRROR_URL",
    "COURSIER_CLI_GITHUB_ASSET_URL",
    "COURSIER_CLI_SHA256",
)

_BUILD = """
package(default_visibility = ["//visibility:{visibility}"])

exports_files(["pin"])

load("@{repository_name}//:jvm_import.bzl", "jvm_import")

{imports}
"""

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

# Generate the base `coursier` command depending on the OS, JAVA_HOME or the
# location of `java`.
def _generate_java_jar_command(repository_ctx, jar_path):
    java_home = repository_ctx.os.environ.get("JAVA_HOME")

    if java_home != None:
        # https://github.com/coursier/coursier/blob/master/doc/FORMER-README.md#how-can-the-launcher-be-run-on-windows-or-manually-with-the-java-program
        # The -noverify option seems to be required after the proguarding step
        # of the main JAR of coursier.
        java = repository_ctx.path(java_home + "/bin/java")
        cmd = [java, "-noverify", "-jar"] + _get_java_proxy_args(repository_ctx) + [jar_path]
    elif repository_ctx.which("java") != None:
        # Use 'java' from $PATH
        cmd = [repository_ctx.which("java"), "-noverify", "-jar"] + _get_java_proxy_args(repository_ctx) + [jar_path]
    else:
        # Try to execute coursier directly
        cmd = [jar_path] + ["-J%s" % arg for arg in _get_java_proxy_args(repository_ctx)]

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
                artifact["url"],
            ])
        if len(artifact["dependencies"]) > 0:
            artifact_group.append(",".join(sorted(artifact["dependencies"])))
        signature_inputs.append(":".join(artifact_group))
    return hash(repr(sorted(signature_inputs)))

def extract_netrc_from_auth_url(url):
    """Return a dict showing the netrc machine, login, and password extracted from a url.

    Returns:
        A dict that is empty if there were no credentials in the url.
        A dict that has three keys -- machine, login, password -- with their respective values. These values should be
        what is needed for the netrc entry of the same name except for password whose value may be empty meaning that
        there is no password for that login.
    """
    if "@" not in url:
        return {}
    protocol, url_parts = split_url(url)
    login_password_host = url_parts[0]
    if "@" not in login_password_host:
        return {}
    login_password, host = login_password_host.rsplit("@", 1)
    login_password_split = login_password.split(":", 1)
    login = login_password_split[0]

    # If password is not provided, then this will be a 1-length split
    if len(login_password_split) < 2:
        password = None
    else:
        password = login_password_split[1]
    if not host:
        fail("Got a blank host from: {}".format(url))
    if not login:
        fail("Got a blank login from: {}".format(url))

    # Do not fail for blank password since that is sometimes a thing
    return {
        "machine": host,
        "login": login,
        "password": password,
    }

def add_netrc_entries_from_mirror_urls(netrc_entries, mirror_urls):
    """Add a url's auth credentials into a netrc dict of form return[machine][login] = password."""
    for url in mirror_urls:
        entry = extract_netrc_from_auth_url(url)
        if not entry:
            continue
        machine = entry["machine"]
        login = entry["login"]
        password = entry["password"]
        if machine not in netrc_entries:
            netrc_entries[machine] = {}
        if login not in netrc_entries[machine]:
            if netrc_entries[machine]:
                print("Received multiple logins for machine '{}'! Only using '{}'".format(
                    machine,
                    netrc_entries[machine].keys()[0],
                ))
                continue
            netrc_entries[machine][login] = password
        elif netrc_entries[machine][login] != password:
            print("Received different passwords for {}@{}! Only using the first".format(login, machine))
    return netrc_entries

def get_netrc_lines_from_entries(netrc_entries):
    netrc_lines = []
    for machine, login_dict in sorted(netrc_entries.items()):
        for login, password in sorted(login_dict.items()):
            netrc_lines.append("machine {}".format(machine))
            netrc_lines.append("login {}".format(login))
            if password:
                netrc_lines.append("password {}".format(password))
    return netrc_lines

def get_home_netrc_contents(repository_ctx):
    # Copied with a ctx -> repository_ctx rename from tools/build_defs/repo/http.bzl's _get_auth.
    # Need to keep updated with improvements in source since we cannot load private methods.
    if "HOME" in repository_ctx.os.environ:
        if not repository_ctx.os.name.startswith("windows"):
            netrcfile = "%s/.netrc" % (repository_ctx.os.environ["HOME"],)
            if repository_ctx.execute(["test", "-f", netrcfile]).return_code == 0:
                return repository_ctx.read(netrcfile)
    return ""

def _pinned_coursier_fetch_impl(repository_ctx):
    if not repository_ctx.attr.maven_install_json:
        fail("Please specify the file label to maven_install.json (e.g." +
             "//:maven_install.json).")

    _windows_check(repository_ctx)

    artifacts = []
    for a in repository_ctx.attr.artifacts:
        artifacts.append(json_parse(a))

    # Read Coursier state from maven_install.json.
    repository_ctx.symlink(
        repository_ctx.path(repository_ctx.attr.maven_install_json),
        repository_ctx.path("imported_maven_install.json"),
    )
    maven_install_json_content = json_parse(
        repository_ctx.read(
            repository_ctx.path("imported_maven_install.json"),
        ),
        fail_on_invalid = False,
    )

    # Validation steps for maven_install.json.

    # First, validate that we can parse the JSON file.
    if maven_install_json_content == None:
        fail("Failed to parse %s. Is this file valid JSON? The file may have been corrupted." % repository_ctx.path(repository_ctx.attr.maven_install_json) +
             "Consider regenerating maven_install.json with the following steps:\n" +
             "  1. Remove the maven_install_json attribute from your `maven_install` declaration for `@%s`.\n" % repository_ctx.name +
             "  2. Regenerate `maven_install.json` by running the command: bazel run @%s//:pin" % repository_ctx.name +
             "  3. Add `maven_install_json = \"//:maven_install.json\"` into your `maven_install` declaration.")

    # Then, validate that there's a dependency_tree element in the parsed JSON.
    if maven_install_json_content.get("dependency_tree") == None:
        fail("Failed to parse %s. " % repository_ctx.path(repository_ctx.attr.maven_install_json) +
             "It is not a valid maven_install.json file. Has this " +
             "file been modified manually?")

    dep_tree = maven_install_json_content["dependency_tree"]

    dep_tree_signature = dep_tree.get("__AUTOGENERATED_FILE_DO_NOT_MODIFY_THIS_FILE_MANUALLY")

    if dep_tree_signature == None:
        print("NOTE: %s_install.json does not contain a signature entry of the dependency tree. " % repository_ctx.name +
              "This feature ensures that the file is not modified manually. To generate this " +
              "signature, run 'bazel run @unpinned_%s//:pin'." % repository_ctx.name)
    elif _compute_dependency_tree_signature(dep_tree["dependencies"]) != dep_tree_signature:
        # Then, validate that the signature provided matches the contents of the dependency_tree.
        # This is to stop users from manually modifying maven_install.json.
        fail("%s_install.json contains an invalid signature and may be corrupted. " % repository_ctx.name +
             "PLEASE DO NOT MODIFY THIS FILE DIRECTLY! To generate a new " +
             "%s_install.json and re-pin the artifacts, follow these steps: \n\n" % repository_ctx.name +
             "  1) In your WORKSPACE file, comment or remove the 'maven_install_json' attribute in 'maven_install'.\n" +
             "  2) Run 'bazel run @%s//:pin'.\n" % repository_ctx.name +
             "  3) Uncomment or re-add the 'maven_install_json' attribute in 'maven_install'.\n\n")

    # Create the list of http_file repositories for each of the artifacts
    # in maven_install.json. This will be loaded additionally like so:
    #
    # load("@maven//:defs.bzl", "pinned_maven_install")
    # pinned_maven_install()
    http_files = [
        "load(\"@bazel_tools//tools/build_defs/repo:http.bzl\", \"http_file\")",
        "def pinned_maven_install():",
    ]
    netrc_entries = {}
    for artifact in dep_tree["dependencies"]:
        if artifact.get("url") != None:
            http_file_repository_name = escape(artifact["coord"])
            http_files.extend([
                "    http_file(",
                "        name = \"%s\"," % http_file_repository_name,
                "        sha256 = \"%s\"," % artifact["sha256"],
                # repository_ctx should point to external/$repository_ctx.name
                # The http_file should point to external/$http_file_repository_name
                # File-path is relative defined from http_file traveling to repository_ctx.
                "        netrc = \"../%s/netrc\"," % (repository_ctx.name),
            ])
            if artifact.get("mirror_urls") != None:
                http_files.append("        urls = %s," % repr(
                    [remove_auth_from_url(url) for url in artifact["mirror_urls"]],
                ))
                netrc_entries = add_netrc_entries_from_mirror_urls(netrc_entries, artifact["mirror_urls"])
            else:
                # For backwards compatibility. mirror_urls is a field added in a
                # later version than the url field, so not all maven_install.json
                # contains the mirror_urls field.
                http_files.append("        urls = [\"%s\"]," % artifact["url"])
            http_files.append("    )")
    repository_ctx.file("defs.bzl", "\n".join(http_files), executable = False)
    repository_ctx.file("netrc", "\n".join(
        get_home_netrc_contents(repository_ctx).splitlines() + get_netrc_lines_from_entries(netrc_entries)),
        executable = False)

    repository_ctx.report_progress("Generating BUILD targets..")
    (generated_imports, jar_versionless_target_labels) = parser.generate_imports(
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

def split_url(url):
    protocol = url[:url.find("://")]
    url_without_protocol = url[url.find("://") + 3:]
    url_parts = url_without_protocol.split("/")
    return protocol, url_parts

def remove_auth_from_url(url):
    """Returns url without `user:pass@` or `user@`."""
    if "@" not in url:
        return url
    protocol, url_parts = split_url(url)
    host = url_parts[0]
    if "@" not in host:
        return url
    userless_host = host[host.find("@") + 1:]
    new_url = "{}://{}".format(protocol, "/".join([userless_host] + url_parts[1:]))
    return new_url

def infer_artifact_path_from_primary_and_repos(primary_url, repository_urls):
    """Returns the artifact path inferred by comparing primary_url with urls in repository_urls.

    When given a list of repository urls and a primary url that has a repository url as a prefix and a maven artifact
    path as a suffix, this method will try to determine what the maven artifact path is and return it.

    This method has some handling for basic http-based auth parsing and will do a url comparison with the user:pass@
    portion stripped.

    Ex.
    infer_artifact_path_from_primary_and_repos(
        "http://a:b@c/group/path/to/artifact/file.jar",
        ["http://c"])
    == "group/path/to/artifact/file.jar"

    Returns:
        String of the artifact path used by maven to find a particular artifact. Does not have a leading slash (`/`).
    """
    userless_repository_urls = [remove_auth_from_url(r.rstrip("/")) for r in repository_urls]
    userless_primary_url = remove_auth_from_url(primary_url)
    primary_artifact_path = None
    for url in userless_repository_urls:
        if userless_primary_url.find(url) != -1:
            primary_artifact_path = userless_primary_url[len(url) + 1:]
            break
    return primary_artifact_path

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
    exec_result = repository_ctx.execute(
        _generate_java_jar_command(repository_ctx, repository_ctx.path("coursier")),
    )
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

    cmd = _generate_java_jar_command(repository_ctx, repository_ctx.path("coursier"))
    cmd.extend(["fetch"])
    cmd.extend(artifact_coordinates)
    if repository_ctx.attr.version_conflict_policy == "pinned":
        for coord in artifact_coordinates:
            # Undo any `,classifier=` suffix from `utils.artifact_coordinate`.
            cmd.extend(["--force-version", coord.split(",classifier=")[0]])
    cmd.extend(["--artifact-type", ",".join(SUPPORTED_PACKAGING_TYPES + ["src"])])
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
    exec_result = repository_ctx.execute(cmd, timeout = repository_ctx.attr.resolve_timeout)
    if (exec_result.return_code != 0):
        fail("Error while fetching artifact with coursier: " + exec_result.stderr)

    # Once coursier finishes a fetch, it generates a tree of artifacts and their
    # transitive dependencies in a JSON file. We use that as the source of truth
    # to generate the repository's BUILD file.
    dep_tree = json_parse(repository_ctx.read(repository_ctx.path("dep-tree.json")))

    # Reconstruct the original URLs from the relative path to the artifact,
    # which encodes the URL components for the protocol, domain, and path to
    # the file.

    hasher_command = _generate_java_jar_command(
        repository_ctx,
        repository_ctx.path(repository_ctx.attr._sha256_hasher),
    )

    for artifact in dep_tree["dependencies"]:
        # Some artifacts don't contain files; they are just parent artifacts
        # to other artifacts.
        if artifact["file"] == None:
            continue

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
                break
        if protocol == None:
            fail("Only artifacts downloaded over http(s) are supported: %s" % artifact["coord"])
        primary_url_parts.extend([protocol, "://"])
        for part in filepath_parts[filepath_parts.index(protocol) + 1:]:
            primary_url_parts.extend([part, "/"])
        primary_url_parts.pop()  # pop the final "/"

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
        repository_urls = [r["repo_url"].rstrip("/") for r in repositories]
        primary_artifact_path = infer_artifact_path_from_primary_and_repos(primary_url, repository_urls)

        mirror_urls = [url + "/" + primary_artifact_path for url in repository_urls]
        artifact.update({"mirror_urls": mirror_urls})

        hasher_command.append(repository_ctx.path(artifact["file"]))

    exec_result = repository_ctx.execute(hasher_command)
    if exec_result.return_code != 0:
        fail("Error while obtaining the sha256 checksums: " + exec_result.stderr)

    shas = {}
    for line in exec_result.stdout.splitlines():
        parts = line.split(" ")
        path = str(repository_ctx.path(parts[1]))
        shas[path] = parts[0]

    for artifact in dep_tree["dependencies"]:
        file = artifact["file"]
        if file == None:
            continue
        artifact.update({"sha256": shas[str(repository_ctx.path(file))]})

    dep_tree.update({
        "__AUTOGENERATED_FILE_DO_NOT_MODIFY_THIS_FILE_MANUALLY": _compute_dependency_tree_signature(dep_tree["dependencies"]),
    })

    repository_ctx.report_progress("Generating BUILD targets..")
    (generated_imports, jar_versionless_target_labels) = parser.generate_imports(
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
            maven_install_location = file_name  # e.g. some.json
        else:
            maven_install_location = "/".join([package_path, file_name])  # e.g. path/to/some.json
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
    repository_ctx.template(
        "pin",
        repository_ctx.attr._pin,
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
        executable = False,
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
        "_sha256_hasher": attr.label(default = "//private/tools/prebuilt:hasher_deploy.jar"),
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
