#!/usr/bin/env bash
# Build emberwild.jar from source (needs JDK 21+). Works on macOS and Linux.
set -e
cd "$(dirname "$0")"
rm -rf build && mkdir -p build/classes
javac --release 21 -encoding UTF-8 -Xlint:-unchecked -d build/classes $(find src -name '*.java')
cp -R resources/. build/classes/
printf 'Main-Class: emberwild.Main\n' > build/manifest.txt
jar --create --file emberwild.jar --manifest build/manifest.txt -C build/classes .
echo "Built emberwild.jar — run it with: java -jar emberwild.jar"
