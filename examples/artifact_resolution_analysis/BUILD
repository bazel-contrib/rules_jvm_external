load("@maven//:pinned_maven_install.bzl", "dependency_tree")
load("@bazel_skylib//rules:diff_test.bzl", "diff_test")

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
