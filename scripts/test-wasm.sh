#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

API_VERSION="${PUMPKIN_API_VERSION:-0.1.2}"
export PUMPKIN_API_VERSION="$API_VERSION"
API_DIR="${PUMPKIN_API_KT_DIR:-/tmp/opencode/pumpkin-api-kt}"
TRANSLATOR_JAR="build/libs/j2kt-pumpkin-0.1.0-all.jar"

echo "==> Pumpkin API version: $API_VERSION (latest tagged dev release at the time of writing)"

if [[ ! -d "$HOME/.m2/repository/org/pumpkinmc/pumpkin-api-kt/$API_VERSION" ]]; then
  if [[ -x "$API_DIR/gradlew" ]]; then
    echo "==> Publishing Pumpkin API and Gradle plugin $API_VERSION from $API_DIR"
    "$API_DIR/gradlew" -p "$API_DIR" \
      :api:publishToMavenLocal :gradle-plugin:publishToMavenLocal
  else
    echo "Pumpkin API $API_VERSION is not in Maven Local." >&2
    echo "Set PUMPKIN_API_KT_DIR to a pumpkin-api-kt checkout and rerun." >&2
    exit 1
  fi
fi

echo "==> Build translator"
./gradlew fatJar

echo "==> Java -> Kotlin -> wasm: five use cases"
rm -rf wasm-tests/uc1/build/generated/kotlin
mkdir -p wasm-tests/uc1/build/generated/kotlin
java -jar "$TRANSLATOR_JAR" \
  --out wasm-tests/uc1/build/generated/kotlin \
  --classpath libs/pumpkin-stub.jar \
  examples/wasm/use-cases/uc1
./gradlew -p wasm-tests/uc1 build

echo "==> Java -> Kotlin -> wasm: five packet examples"
rm -rf wasm-tests/packets/build/generated/kotlin
mkdir -p wasm-tests/packets/build/generated/kotlin
java -jar "$TRANSLATOR_JAR" \
  --out wasm-tests/packets/build/generated/kotlin \
  --classpath libs/pumpkin-stub.jar \
  examples/wasm/packets
./gradlew -p wasm-tests/packets build

echo "==> Components"
ls -lh wasm-tests/uc1/build/uc1.wasm wasm-tests/packets/build/packets.wasm
echo "OK"
