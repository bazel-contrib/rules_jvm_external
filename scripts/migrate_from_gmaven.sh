#!/bin/bash

set -euo pipefail

# Replace the load statements
find ./ -type f -name BUILD* -exec sed -i 's/gmaven_rules/rules_jvm_external/g' {} \;

# Replace the loaded gmaven_artifact symbol with maven_artifact
find ./ -type f -name BUILD* -exec sed -i 's/"gmaven_artifact"/maven_artifact = "artifact"/g' {} \;

# Replace the gmaven_artifact macro with maven_artifact
find ./ -type f -name BUILD* -exec sed -i 's/gmaven_artifact(/maven_artifact(/g' {} \;

# Remove the explicit packaging type from the coordinates
find ./ -type f -name BUILD* -exec sed -i '/maven_artifact(/s/:aar//g; /maven_artifact(/s/:jar//g;' {} \;

echo ""
echo "Paste this into your WORKSPACE file:"
echo "-------------------------"
echo ""

cat <<-EOF
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "1.0"
RULES_JVM_EXTERNAL_SHA = "48e0f1aab74fabba98feb8825459ef08dcc75618d381dff63ec9d4dd9860deaa"

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

# Grep for all Maven artifact IDs and uniquify them.
# Steps
# 1. Get the list of maven_artifact declarations
# 2. Remove the explicit packaging type
# 3. Get the string to the left of the right bracket
# 4. Get the string to the right of the left bracket
# 5. Sort and de-duplicate
# 6. Format for WORKSPACE
find ./ -type f -name BUILD* -exec grep "maven_artifact(.*)" {} \; \
  | sed -e "s/:aar//; s/:jar//" \
  | cut -d')' -f1 \
  | cut -d'(' -f2 \
  | sort \
  | uniq \
  | sed 's/^/        /; s/$/,/;'

cat <<-EOF
    ],
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)
EOF

echo ""
echo "-------------------------"
