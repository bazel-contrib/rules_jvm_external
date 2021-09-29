#!/usr/bin/env bash

set -euo pipefail

if [ -f "private/tools/prebuilt/outdated_deploy.jar" ]; then
    outdated_jar_path=private/tools/prebuilt/outdated_deploy.jar
else
    outdated_jar_path=../rules_jvm_external/private/tools/prebuilt/outdated_deploy.jar
fi

java {proxy_opts} -jar $outdated_jar_path ../{repository_name}/outdated.artifacts ../{repository_name}/outdated.repositories
