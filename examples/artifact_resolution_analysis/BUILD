load("@pinned_maven//:pinned_maven_install.bzl", "dependency_tree")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")
load("@bazel_skylib//rules:build_test.bzl", "build_test")

# Regression tests for particular artifacts that revealed problems for this
# project.

build_test(
    name = "build_test",
    targets = [
        "@pinned_maven//:com_google_guava_guava",
    ],
)

genrule(
    name = "copy_dependency_tree",
    outs = ["dependency_tree.bzl"],
    cmd = "echo %s > $@" % repr(dependency_tree["dependencies"]).replace("\"", "\\\""),
)

diff_test(
    name = "diff_dependency_tree",
    file1 = ":dependency_tree.bzl",
    file2 = ":golden_dependency_tree.bzl",
)

genrule(
    name = "resolved_transitive_artifacts",
    outs = ["resolved_transitive_artifacts.txt"],
    cmd = """
    cat <<EOF > $@
%s""" % "\n".join([artifact["coord"] for artifact in dependency_tree["dependencies"]]),
)

diff_test(
    name = "diff_transitive_artifact_resolution",
    file1 = ":resolved_transitive_artifacts.txt",
    file2 = ":golden_transitive_artifacts.txt",
)
