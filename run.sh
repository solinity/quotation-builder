#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

driver_version=3.54.1.0
driver="lib/sqlite-jdbc-$driver_version.jar"
if [[ ! -f "$driver" ]]; then
    mkdir -p lib
    download=$(mktemp "$driver.XXXXXX")
    trap 'rm -f -- "$download"' EXIT
    curl -fsSL -o "$download" "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/$driver_version/${driver##*/}"
    printf '%s  %s\n' 'f8dce420a6016278497f4f0560f65602b9db858de141b541cf91a4644ebe4639' "$download" | sha256sum -c
    mv -- "$download" "$driver"
fi

mkdir -p out
shopt -s globstar
javac --release 11 -encoding UTF-8 -d out Main.java src/**/*.java

java_version=$(java --version)
java_options=()
if [[ $java_version =~ ^(openjdk|java)\ ([0-9]+) ]] && (( BASH_REMATCH[2] >= 24 )); then
    java_options+=(--enable-native-access=ALL-UNNAMED)
fi
exec java "${java_options[@]}" -cp "out:$driver" Main
