#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> translator + stub tests"
./gradlew test

echo "==> publish local artifacts"
./gradlew publishToMavenLocal

echo "==> external Java consumer"
(cd examples/stub-project && gradle build)

echo "==> compile golden Java inputs against stub"
java_tmp="$(mktemp -d)"
mkdir -p "$java_tmp/src" "$java_tmp/out"
for case_dir in src/test/resources/cases/*/; do
  input="${case_dir}input.java"
  [[ -f "$input" ]] || continue
  name="$(sed -n 's/.*public \(class\|interface\|enum\) \([A-Za-z0-9_]*\).*/\2/p' "$input" | head -1)"
  if [[ -z "$name" ]]; then
    name="$(basename "$case_dir" | tr -d '-')"
  fi
  cp "$input" "$java_tmp/src/$name.java"
done
javac -cp libs/pumpkin-stub.jar -d "$java_tmp/out" "$java_tmp"/src/*.java

# The Java sources used by the wasm integration suite must also compile against the stub.
mapfile -t wasm_java < <(find examples/wasm templates/intellij-plugin/java-plugin/src/main/java -name '*.java' -print)
if [[ ${#wasm_java[@]} -gt 0 ]]; then
  javac -cp libs/pumpkin-stub.jar -d "$java_tmp/out" "${wasm_java[@]}"
fi
rm -rf "$java_tmp"

if command -v kotlinc >/dev/null 2>&1; then
  echo "==> compile JVM golden outputs"
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  for case_dir in src/test/resources/cases/*/; do
    target="$(sed -n 's/^target=//p' "${case_dir}options.properties" 2>/dev/null || true)"
    if [[ "$target" == "jvm" && -f "${case_dir}expected.kt" ]]; then
      cp "${case_dir}expected.kt" "$tmp/$(basename "$case_dir").kt"
    fi
    if [[ "$target" == "pumpkin" && -f "${case_dir}expected.kt" ]]; then
      cp "${case_dir}expected.kt" "$tmp/$(basename "$case_dir")-pumpkin.kt"
    fi
  done
  kotlinc "$tmp"/*.kt \
    test-fixtures/kt/*.kt \
    pumpkin-compat/src/wasmWasiMain/kotlin/plugin/*.kt \
    -d "$tmp/out.jar"
else
  echo "==> kotlinc not found; skipping Kotlin compilation check"
fi

echo "OK"
