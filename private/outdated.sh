#!/usr/bin/env bash

set -euo pipefail

outdated_jar_path=$1
artifacts=$2
repositories=$3

java {proxy_opts} -jar "$outdated_jar_path" "$artifacts" "$repositories"
