#!/usr/local/bin/bash

set -euo pipefail

echo ""
echo "Paste this into your WORKSPACE file:"
echo "-------------------------"
echo ""


cat <<-EOF
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "2.6"
RULES_JVM_EXTERNAL_SHA = "064b9085b21c349c8bd8be015a73efd6226dd2ff7d474797b3507ceca29544bb"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    name = "maven",
    artifacts = [
EOF

{
    pushd $BUILD_WORKSPACE_DIRECTORY 1>/dev/null
    bazel query 'kind(maven_jar, //external:all)' --output=build --noshow_progress | grep artifact | awk '{print $3}' | sort | sed 's/^/        /;' 2>/dev/null
    popd 1>/dev/null
}

cat <<-EOF
    ],
    repositories = [
        "https://jcenter.bintray.com",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
    fetch_sources = True,
)
EOF

echo ""
echo "-------------------------"
echo ""

{
    pushd $BUILD_WORKSPACE_DIRECTORY 1>/dev/null
    declare -a artifact_list=($(bazel query 'kind(maven_jar, //external:all)' --output=build --noshow_progress | grep artifact | awk '{print $3}' | sed 's/"//g; s/,//g'))
    declare -a repo_name_list=($(bazel query 'kind(maven_jar, //external:all)' --output=build --noshow_progress | grep name | awk '{print $3}' | sed 's/"//g; s/,//g'))

    for i in "${!artifact_list[@]}"; do
        target=$(echo "${artifact_list[$i]}" | sed "s/-/_/g; s/\./_/g;" | cut -d":" -f1,2 | sed "s/:/_/g;")
        maven_install_target="@maven//:$target"
        maven_jar_target="@${repo_name_list[$i]}//jar"
        echo "Converting: $maven_jar_target -> $maven_install_target"
        buildozer "substitute * $maven_jar_target $maven_install_target" //...:* || true
    done

    popd 1>/dev/null
}
