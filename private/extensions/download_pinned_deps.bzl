load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

def escape(string):
    for char in [".", "-", ":", "/", "+"]:
        string = string.replace(char, "_")
    return string.replace("[", "").replace("]", "").split(",")[0]

def download_pinned_deps(
        dep_tree,
        artifacts,
        existing_repos):
    seen_repo_names = []

    for artifact in dep_tree["dependencies"]:
        if not artifact.get("mirror_urls"):
            continue

        http_file_repository_name = escape(artifact["coord"])
        if http_file_repository_name in seen_repo_names:
            continue

        if http_file_repository_name in existing_repos:
            continue

        if artifact.get("mirror_urls"):
            urls = artifact.get("mirror_urls")

        seen_repo_names.append(http_file_repository_name)

        http_file(
            name = http_file_repository_name,
            sha256 = artifact["sha256"],
            urls = urls,
            downloaded_file_path = artifact["file"],
        )
    return seen_repo_names
