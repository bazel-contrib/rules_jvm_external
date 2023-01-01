def _remove_auth_from_url(url):
    """Returns url without `user:pass@` or `user@`."""
    if "@" not in url:
        return url
    protocol, url_parts = _split_url(url)
    host = url_parts[0]
    if "@" not in host:
        return url
    last_index = host.rfind("@", 0, None)
    userless_host = host[last_index + 1:]
    new_url = "{}://{}".format(protocol, "/".join([userless_host] + url_parts[1:]))
    return new_url

def _is_valid_lock_file(lock_file_contents):
    return lock_file_contents.get("dependency_tree") != None

def _get_input_artifacts_hash(lock_file_contents):
    dep_tree = lock_file_contents.get("dependency_tree", {})
    return dep_tree.get("__INPUT_ARTIFACTS_HASH")

def _get_lock_file_hash(lock_file_contents):
    dep_tree = lock_file_contents.get("dependency_tree", {})
    return dep_tree.get("__RESOLVED_ARTIFACTS_HASH")

# The representation of a Windows path when read from the parsed Coursier JSON
# is delimited by 4 back slashes. Replace them with 1 forward slash.
def _normalize_to_unix_path(path):
    return path.replace("\\", "/")

def _compute_lock_file_hash(lock_file_contents):
    dep_tree = lock_file_contents.get("dependency_tree", {})
    artifacts = dep_tree["dependencies"]

    # A collection of elements from the dependency tree to be sorted and hashed
    # into a signature for maven_install.json.
    signature_inputs = []
    for artifact in artifacts:
        artifact_group = []
        artifact_group.append(artifact["coord"])
        if artifact["file"] != None:
            artifact_group.extend([
                artifact["sha256"],
                _normalize_to_unix_path(artifact["file"]),  # Make sure we represent files in a stable way cross-platform
            ])
            if artifact["url"]:
                artifact_group.append(artifact["url"])
        if len(artifact["dependencies"]) > 0:
            artifact_group.append(",".join(sorted(artifact["dependencies"])))
        signature_inputs.append(":".join(artifact_group))
    return hash(repr(sorted(signature_inputs)))

def create_dependency(dep):
    if not dep.get("url") and not dep.get("file"):
        return None

    url = dep.get("url")
    if url:
        urls = [_remove_auth_from_url(url)]
        urls.extend([_remove_auth_from_url(u) for u in dep.get("mirror_urls", []) if u != url])
    else:
        urls = ["file://%s" % dep["file"]]

    return {
        "coordinates": dep["coord"],
        "file": dep["file"],
        "sha256": dep["sha256"],
        "deps": dep["directDependencies"],
        "urls": urls,
    }

def _get_artifacts(lock_file_contents):
    dep_tree = lock_file_contents.get("dependency_tree", {})
    raw_deps = dep_tree.get("dependencies", [])

    to_return = []
    for dep in raw_deps:
        created = create_dependency(dep)

        if created:
            to_return.append(created)

    return to_return

def _split_url(url):
    protocol = url[:url.find("://")]
    url_without_protocol = url[url.find("://") + 3:]
    url_parts = url_without_protocol.split("/")
    return protocol, url_parts

def _extract_netrc_from_auth_url(url):
    """Return a dict showing the netrc machine, login, and password extracted from a url.

    Returns:
        A dict that is empty if there were no credentials in the url.
        A dict that has three keys -- machine, login, password -- with their respective values. These values should be
        what is needed for the netrc entry of the same name except for password whose value may be empty meaning that
        there is no password for that login.
    """
    if "@" not in url:
        return {}
    protocol, url_parts = _split_url(url)
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

def _add_netrc_entries_from_mirror_urls(netrc_entries, mirror_urls):
    """Add a url's auth credentials into a netrc dict of form return[machine][login] = password."""
    for url in mirror_urls:
        entry = _extract_netrc_from_auth_url(url)
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

def _get_netrc_entries(lock_file_contents):
    dep_tree = lock_file_contents.get("dependency_tree", {})
    raw_deps = dep_tree.get("dependencies", [])

    netrc_entries = {}
    for dep in raw_deps:
        if dep.get("mirror_urls"):
            netrc_entries = _add_netrc_entries_from_mirror_urls(netrc_entries, dep["mirror_urls"])

    return netrc_entries

v1_lock_file = struct(
    is_valid_lock_file = _is_valid_lock_file,
    get_input_artifacts_hash = _get_input_artifacts_hash,
    get_lock_file_hash = _get_lock_file_hash,
    compute_lock_file_hash = _compute_lock_file_hash,
    get_artifacts = _get_artifacts,
    get_netrc_entries = _get_netrc_entries,
)
