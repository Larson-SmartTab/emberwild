#!/usr/bin/env bash
# Double-click to host a game on macOS (first time: right-click > Open).
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then echo "Java 21+ is required: https://adoptium.net/"; read -r; exit 1; fi
java -jar emberwild.jar "$@"
