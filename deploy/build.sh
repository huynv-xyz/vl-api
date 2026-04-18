#!/bin/bash
set -e

cd "$(dirname "$0")/.."

echo "Cleaning and building fat jar..."
./gradlew clean shadowJar

echo "Build done."
ls -lah build/libs/