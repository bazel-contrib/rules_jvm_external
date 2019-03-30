#!/bin/bash

set -euo pipefail

cd $1; shift;
module=${1-""};

if [ "$module" != "" ]; then
    module="$module:"
fi

cat <<-EOF
load("@rules_jvm_external//:defs.bzl", "maven_install")
maven_install(
    name = "maven",
    artifacts = [
EOF

./gradlew "$module"dependencies --console plain \
    | grep "+---" \
    | grep -v "    " \
    | sed -e 's/{strictly //' \
    | sed -e 's/}//' \
    | cut -d' ' -f2 \
    | sort \
    | uniq \
    | sed -e 's/\(.*\)/"\1",/' \
    | sed -e 's/^/        /'

cat <<-EOF
    ],
    repositories = [
        "https://maven.google.com",
        "https://jcenter.bintray.com",
        "https://repo1.maven.org/maven2",
    ],
)
EOF
