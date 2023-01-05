load("//private:dependency_tree_parser.bzl", "JETIFY_INCLUDE_LIST_JETIFY_ALL")
load("//private/rules:v1_lock_file.bzl", "v1_lock_file")
load("//private/rules:v2_lock_file.bzl", "v2_lock_file")
load("//:specs.bzl", "parse", _json = "json")
load("//:coursier.bzl", "DEFAULT_AAR_IMPORT_LABEL", "coursier_fetch", "pinned_coursier_fetch")
load(":download_pinned_deps.bzl", "download_pinned_deps")

DEFAULT_REPOSITORIES = [
    "https://repo1.maven.org/maven2",
]

DEFAULT_NAME = "maven"

_artifact = tag_class(
    attrs = {
        "name": attr.string(default = DEFAULT_NAME),
        "group": attr.string(mandatory = True),
        "artifact": attr.string(mandatory = True),
        "version": attr.string(),
        "packaging": attr.string(),
        "classifier": attr.string(),
        "neverlink": attr.bool(),
        "testonly": attr.bool(),
        "exclusions": attr.string_list(doc = "Maven artifact tuples, in `artifactId:groupId` format", allow_empty = True),
        "repositories": attr.string_list(default = DEFAULT_REPOSITORIES),
    },
)

_install = tag_class(
    attrs = {
        "name": attr.string(default = DEFAULT_NAME),

        # Actual artifacts and overrides
        "artifacts": attr.string_list(doc = "Maven artifact tuples, in `artifactId:groupId:version` format", allow_empty = True),
        "exclusions": attr.string_list(doc = "Maven artifact tuples, in `artifactId:groupId` format", allow_empty = True),

        # What do we fetch?
        "fetch_javadoc": attr.bool(default = False),
        "fetch_sources": attr.bool(default = True),

        # Controlling visibility
        "strict_visibility": attr.bool(
            doc = """Controls visibility of transitive dependencies.

            If "True", transitive dependencies are private and invisible to user's rules.
            If "False", transitive dependencies are public and visible to user's rules.
            """,
            default = False,
        ),
        "strict_visibility_value": attr.label_list(default = ["//visibility:private"]),

        # Android support
        "aar_import_bzl_label": attr.string(default = DEFAULT_AAR_IMPORT_LABEL, doc = "The label (as a string) to use to import aar_import from"),
        "jetify": attr.bool(doc = "Runs the AndroidX [Jetifier](https://developer.android.com/studio/command-line/jetifier) tool on artifacts specified in jetify_include_list. If jetify_include_list is not specified, run Jetifier on all artifacts.", default = False),
        "jetify_include_list": attr.string_list(doc = "List of artifacts that need to be jetified in `groupId:artifactId` format. By default all artifacts are jetified if `jetify` is set to True.", default = JETIFY_INCLUDE_LIST_JETIFY_ALL),
        "use_starlark_android_rules": attr.bool(default = False, doc = "Whether to use the native or Starlark version of the Android rules."),

        # Configuration "stuff"
        "additional_netrc_lines": attr.string_list(doc = "Additional lines prepended to the netrc file used by `http_file` (with `maven_install_json` only).", default = []),
        "duplicate_version_warning": attr.string(
            doc = """What to do if there are duplicate artifacts

            If "error", then print a message and fail the build.
            If "warn", then print a warning and continue.
            If "none", then do nothing.
            """,
            default = "warn",
            values = [
                "error",
                "warn",
                "none",
            ],
        ),
        "fail_if_repin_required": attr.bool(doc = "Whether to fail the build if the maven_artifact inputs have changed but the lock file has not been repinned.", default = False),
        "lock_file": attr.label(),
        "repositories": attr.string_list(default = DEFAULT_REPOSITORIES),

        # When using an unpinned repo
        "excluded_artifacts": attr.string_list(doc = "Artifacts to exclude, in `artifactId:groupId` format. Only used on unpinned installs", default = []),  # list of artifacts to exclude
        "fail_on_missing_checksum": attr.bool(default = True),
        "resolve_timeout": attr.int(default = 600),
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
    },
)

_override = tag_class(
    attrs = {
        "name": attr.string(default = DEFAULT_NAME),
        "coordinates": attr.string(doc = "Maven artifact tuple in `artifactId:groupId` format", mandatory = True),
        "target": attr.label(doc = "Target to use in place of maven coordinates", mandatory = True),
    },
)

def _logical_or(source, key, default_value, new_value):
    current = source.get(key, default_value)
    source[key] = current or new_value

def _fail_if_different(attribute, current, next, allowed_default_values):
    if current == next:
        return current

    if next in allowed_default_values:
        return current

    if current in allowed_default_values:
        return next

    fail("Expected values for '%s' to be either default or the same. Instead got: %s and %s" % (attribute, current, next))

def _add_exclusions(exclusions, excluded_artifacts):
    to_return = [] + excluded_artifacts

    for exclusion in parse.parse_exclusion_spec_list(exclusions):
        if exclusion not in to_return:
            to_return.append(exclusion)
    return to_return

def _check_repo_name(repo_name_2_module_name, repo_name, module_name):
    known_name = repo_name_2_module_name.get(repo_name)
    if known_name == None:
        repo_name_2_module_name[repo_name] = module_name
        return

    if module_name != known_name:
        print("The maven repository '%s' is used in two different bazel modules, originally in '%s' and now in '%s'" % (
            repo_name,
            known_name,
            module_name,
        ))

def _maven_impl(mctx):
    repos = {}
    overrides = {}
    exclusions = {}

    # Iterate over all the tags we care about. For each `name` we want to construct
    # a dict with the following keys:

    # - aar_import_bzl_label: string. Build will fail if this is duplicated and different.
    # - additional_netrc_lines: string list. Accumulated from all `install` tags
    # - artifacts: the exploded tuple for each artifact we want to include.
    # - duplicate_version_warning: string. Build will fail if duplicated and different
    # - fail_if_repin_required: bool. A logical OR over all `fail_if_repin_required` for all `install` tags with the same name.
    # - fail_on_missing_checksum: bool. A logical OR over all `fail_on_missing_checksum` for all `install` tags with the same name.
    # - fetch_javadoc: bool. A logical OR over all `fetch_javadoc` for all `install` tags with the same name.
    # - fetch_sources: bool. A logical OR over all `fetch_sources` for all `install` tags with the same name.
    # - jetify: bool. A logical OR over all `jetify` for all `install` tags with the same name.
    # - jetify_include_list: string list. Accumulated from all `install` tags
    # - lock_file: the lock file to use, if present. Multiple lock files will cause the build to fail.
    # - overrides: a dict mapping `artfifactId:groupId` to Bazel label.
    # - repositories: the list of repositories to pull files from.
    # - strict_visibility: bool. A logical OR over all `strict_visibility` for all `install` tags with the same name.
    # - strict_visibility_value: a string list. Build will fail is duplicated and different.
    # - use_starlark_android_rules: bool. A logical OR over all `use_starlark_android_rules` for all `install` tags with the same name.
    # - version_conflict_policy: string. Fails build if different and not a default.

    # Mapping of `name`s to `bazel_module.name` This will allow us to warn users when more than
    # module attempts to update a maven repo (which is normally undesired behaviour)
    repo_name_2_module_name = {}

    for mod in mctx.modules:
        for override in mod.tags.override:
            value = str(override.target)
            current = overrides.get(override.coordinates, None)
            to_use = _fail_if_different("Target of override for %s" % override.coordinates, current, value, [None])
            overrides.update({override.coordinates: to_use})

        for artifact in mod.tags.artifact:
            _check_repo_name(repo_name_2_module_name, artifact.name, mod.name)

            repo = repos.get(artifact.name, {})
            existing_artifacts = repo.get("artifacts", [])

            to_add = {
                "group": artifact.group,
                "artifact": artifact.artifact,
            }

            if artifact.version:
                to_add.update({"version": artifact.version})

            if artifact.packaging:
                to_add.update({"packaging": artifact.packaging})

            if artifact.classifier:
                to_add.update({"classifier": artifact.classifier})

            if artifact.neverlink:
                to_add.update({"neverlink": artifact.neverlink})

            if artifact.testonly:
                to_add.update({"version": artifact.testonly})

            if artifact.exclusions:
                artifact_exclusions = []
                artifact_exclusions = _add_exclusions(artifact.exclusions, artifact_exclusions)
                to_add.update({"exclusions": artifact_exclusions})

            existing_artifacts.append(to_add)
            repo["artifacts"] = existing_artifacts
            repos[artifact.name] = repo

        for install in mod.tags.install:
            _check_repo_name(repo_name_2_module_name, install.name, mod.name)

            repo = repos.get(install.name, {})

            artifacts = repo.get("artifacts", [])
            repo["artifacts"] = artifacts + install.artifacts

            existing_repos = repo.get("repositories", [])
            for repository in parse.parse_repository_spec_list(install.repositories):
                repo_string = _json.write_repository_spec(repository)
                if repo_string not in existing_repos:
                    existing_repos.append(repo_string)
            repo["repositories"] = existing_repos

            if install.lock_file:
                lock_file = repo.get("lock_file")
                if lock_file and lock_file != install.lock_file:
                    fail("There can only be one lock file for the repo %s. Lock files seen were %s and %s" % (
                        install.name,
                        lock_file,
                        install.lock_file,
                    ))
                repo["lock_file"] = install.lock_file

            repo["excluded_artifacts"] = _add_exclusions(exclusions, install.excluded_artifacts)

            _logical_or(repo, "fail_if_repin_required", False, install.fail_if_repin_required)
            _logical_or(repo, "fail_on_missing_checksum", False, install.fail_on_missing_checksum)
            _logical_or(repo, "fetch_sources", False, install.fetch_sources)
            _logical_or(repo, "fetch_javadoc", False, install.fetch_javadoc)
            _logical_or(repo, "jetify", False, install.jetify)
            _logical_or(repo, "strict_visibility", False, install.strict_visibility)
            _logical_or(repo, "use_starlark_android_rules", False, install.use_starlark_android_rules)

            repo["version_conflict_policy"] = _fail_if_different(
                "version_conflict_policy",
                repo.get("version_conflict_policy"),
                install.version_conflict_policy,
                [None, "default"],
            )

            repo["strict_visibility_value"] = _fail_if_different(
                "strict_visibility_value",
                repo.get("strict_visibility_value", []),
                install.strict_visibility_value,
                [None, []],
            )

            additional_netrc_lines = repo.get("additional_netrc_lines", []) + getattr(install, "additional_netrc_lines", [])
            repo["additional_netrc_lines"] = additional_netrc_lines

            jetify_include_list = repo.get("jetify_include_list", [])
            for include in getattr(install, "jetify_include_list", []):
                if include not in jetify_include_list:
                    jetify_include_list.append(include)
            repo["jetify_include_list"] = jetify_include_list

            repo["aar_import_bzl_label"] = _fail_if_different(
                "aar_import_bzl_label",
                repo.get("aar_import_bzl_label"),
                install.aar_import_bzl_label,
                [DEFAULT_AAR_IMPORT_LABEL, None],
            )

            repo["duplicate_version_warning"] = _fail_if_different(
                "duplicate_version_warning",
                repo.get("duplicate_version_warning"),
                install.duplicate_version_warning,
                [None, "warn"],
            )

            # Get the longest timeout
            timeout = repo.get("resolve_timeout", install.resolve_timeout)
            if install.resolve_timeout > timeout:
                timout = install.resolve_timeout
            repo["resolve_timeout"] = timeout

            repos[install.name] = repo

    existing_repos = []
    for (name, repo) in repos.items():
        artifacts = parse.parse_artifact_spec_list(repo["artifacts"])
        artifacts_json = [_json.write_artifact_spec(a) for a in artifacts]

        coursier_fetch(
            # Name this repository "unpinned_{name}" if the user specified a
            # maven_install.json file. The actual @{name} repository will be
            # created from the maven_install.json file in the coursier_fetch
            # invocation after this.
            name = "unpinned_" + name if repo.get("lock_file") else name,
            repositories = repo.get("repositories"),
            artifacts = artifacts_json,
            fail_on_missing_checksum = repo.get("fail_on_missing_checksum"),
            fetch_sources = repo.get("fetch_sources"),
            fetch_javadoc = repo.get("fetch_javadoc"),
            excluded_artifacts = repo.get("excluded_artifacts"),
            generate_compat_repositories = False,
            version_conflict_policy = repo.get("version_conflict_policy"),
            override_targets = overrides,
            strict_visibility = repo.get("strict_visibility"),
            strict_visibility_value = repo.get("strict_visibility_value"),
            maven_install_json = repo.get("lock_file"),
            resolve_timeout = repo.get("resolve_timeout"),
            jetify = repo.get("jetify"),
            jetify_include_list = repo.get("jetify_include_list"),
            use_starlark_android_rules = repo.get("use_starlark_android_rules"),
            aar_import_bzl_label = repo.get("aar_import_bzl_label"),
            duplicate_version_warning = repo.get("duplicate_version_warning"),
        )

        if repo.get("lock_file"):
            lock_file = json.decode(mctx.read(mctx.path(repo.get("lock_file"))))

            if v2_lock_file.is_valid_lock_file(lock_file):
                artifacts = v2_lock_file.get_artifacts(lock_file)
            elif v1_lock_file.is_valid_lock_file(lock_file):
                artifacts = v1_lock_file.get_artifacts(lock_file)
            else:
                fail("Unable to determine lock file version: %s" % repo.get("lock_file"))

            created = download_pinned_deps(artifacts = artifacts, existing_repos = existing_repos)
            existing_repos.extend(created)

            pinned_coursier_fetch(
                name = name,
                repositories = repo.get("repositories"),
                artifacts = artifacts_json,
                fetch_sources = repo.get("fetch_sources"),
                fetch_javadoc = repo.get("fetch_javadoc"),
                generate_compat_repositories = False,
                maven_install_json = repo.get("lock_file"),
                override_targets = overrides,
                strict_visibility = repo.get("strict_visibility"),
                strict_visibility_value = repo.get("strict_visibility_value"),
                jetify = repo.get("jetify"),
                jetify_include_list = repo.get("jetify_include_list"),
                additional_netrc_lines = repo.get("additional_netrc_lines"),
                fail_if_repin_required = repo.get("fail_if_repin_required"),
                use_starlark_android_rules = repo.get("use_starlark_android_rules"),
                aar_import_bzl_label = repo.get("aar_import_bzl_label"),
                duplicate_version_warning = repo.get("duplicate_version_warning"),
            )

maven = module_extension(
    _maven_impl,
    tag_classes = {
        "artifact": _artifact,
        "install": _install,
        "override": _override,
    },
)
