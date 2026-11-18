# J2KT Pumpkin

Java-to-Kotlin translator for Pumpkin plugins targeting Kotlin/Wasm. It compiles Java source written against the Pumpkin Java stub and emits Kotlin source compatible with the Pumpkin Kotlin/Wasm API.

## Components

- `src/main/java/dev/j2kt`: translator, JavaParser symbol solver, Kotlin emitter, diagnostics, and CLI.
- `pumpkin-compat`: Kotlin helpers for events, commands, tasks, IPC, AI goals, GUI, inventory, forms, recipes, datapacks, scoreboard, display entities, and player helpers.
- `templates/intellij-plugin`: self-contained IntelliJ IDEA project with a `cleanBuildWasm` task.
- `examples/wasm`: five API use cases and five packet examples used by the integration suite.
- `wasm-tests`: Kotlin/Wasm projects that compile translated examples and validate the resulting components.
- `libs/pumpkin-stub.jar`: stub artifact used by tests. The canonical stub project is separate.

## Compatibility

| Component | Version |
|---|---|
| J2KT Pumpkin | 0.1.1 |
| Java | 21 |
| Kotlin | 2.4.0 |
| Gradle | 9.8.1 |
| Pumpkin Kotlin/Wasm API | 0.1.2 |
| Pumpkin plugin WIT | `pumpkin:plugin@0.1.0` (synchronous) |
| Tested Pumpkin | `0.2.2+26.3-26.63` |

The translator targets the synchronous v0.1 WIT ABI. The async `pumpkin:plugin@0.2.0` ABI is not supported.

## Requirements

- JDK 21
- Network access to Maven Central and the Gradle Plugin Portal on the first build
- A `pumpkin-api-kt` checkout when running the full Wasm integration suite

## Build

```bash
./gradlew clean test fatJar
```

Output:

```text
build/libs/j2kt-pumpkin-0.1.1-all.jar
```

Run the tests:

```bash
./gradlew test
```

Run the translation and Wasm integration suite:

```bash
./scripts/test-wasm.sh
```

The Wasm suite publishes `pumpkin-api-kt` to Maven Local when the artifacts are missing. Set `PUMPKIN_API_KT_DIR` to a checkout when it is not located at `/tmp/opencode/pumpkin-api-kt`.

## CLI

Translate a Java source tree:

```bash
java -jar build/libs/j2kt-pumpkin-0.1.1-all.jar \
  --target pumpkin \
  --classpath libs/pumpkin-stub.jar \
  --out generated \
  path/to/java-sources
```

Translation reports are written with `--report <file>`. Use `--no-compat` only when the target API already provides the compatibility helpers.

## Supported Java surface

The translator covers the Java constructs exercised by the test suite, including:

- Classes, interfaces, abstract classes, inheritance, generics, and bounds.
- Fields, constructors, static members, and companion objects.
- Lambdas, method references, anonymous classes, and SAM interfaces.
- Records, enums with constructors, and sealed interfaces.
- Collections, arrays, numeric conversions, strings, and common `Math`/`Objects` APIs.
- `switch`, `try`/`catch`/`finally`, multi-catch, and try-with-resources.
- Java reference equality in strict mode and `.equals` mapping.
- Javadoc and common annotations.

## API coverage

| Area | Status |
|---|---|
| Lifecycle and metadata | Supported |
| Logging and text components | Supported |
| Events and all 273 event variants | Supported |
| Commands and handler registration | Supported; typed arguments and suggestions are partial |
| Permissions | Supported |
| Delayed tasks and repeating tasks | Supported through `tasks` and `pumpkin-compat` |
| IPC | Basic support |
| Boss bars | Supported through the Java stub |
| Player, server, world, inventory, and item operations | Main operations |
| GUI, screens, forms, Java dialogs | Supported |
| Scoreboard and teams | Supported |
| Recipes, datapacks, enchantments, display entities, blocks, mobs, AI | Supported in the test suite |
| Java and Bedrock typed packets, custom payloads | Supported |
| Persistent data | Data path only; filesystem access is not provided by the Kotlin/Wasm bindings |
| World generation | Not supported |

## Known limitations

- Java reflection, dynamic proxies, method handles, threads, `synchronized`, `volatile`, `Executor`, and `CompletableFuture` are not supported.
- `java.time`, `java.io`, `java.nio`, `java.util.concurrent`, `BigDecimal`, `String.format`, and similar JVM-only APIs are not supported in Wasm.
- Complex Java streams and complex `switch` fallthrough with control flow are not translated safely.
- Configuration support and persistent plugin settings are not implemented.
- The generated Kotlin/Wasm wrappers consume resource handles passed to methods. Reusing a `Player` or `TextComponent` after passing it to `BossBar.addPlayer` or `BossBar.setTitle` is invalid. Use `BossBar.removeAll()` to close a bar.
- A Java utility class with only a private no-argument constructor is currently emitted with an ambiguous Kotlin constructor. Use an abstract utility class or a public constructor.
- The stub is maintained separately and must be kept aligned with the generated Kotlin API.

## Tests

Current verification:

- 32 JVM tests, 27 golden translation cases.
- Five API use cases and five packet examples translated to Kotlin/Wasm.
- Translated components validated with the Pumpkin Gradle plugin and `wasm-tools`.
- Runtime validation of TPSBar Java against Pumpkin nightly `0.2.2+26.3-26.63` for commands, boss bars, toggling, and player-leave cleanup.

The Wasm suite validates compilation, linking, and component shape. It does not simulate a live Pumpkin host.

## Project layout

```text
src/main/java/dev/j2kt/
pumpkin-compat/src/wasmWasiMain/kotlin/plugin/
templates/intellij-plugin/
examples/java/
examples/wasm/use-cases/
examples/wasm/packets/
wasm-tests/
test-fixtures/kt/
libs/pumpkin-stub.jar
```

The canonical Java stub is published separately:

```text
https://github.com/AndresqDev/Pumpkin-Stub
```

## Releases

The workflow in `.github/workflows/release.yml` runs on tags matching `v*`. It builds and tests the translator, uploads the final JAR as a GitHub Actions artifact, downloads the same artifact in the release job, and publishes the JAR and its SHA-256 checksum.

Release asset name:

```text
j2kt-pumpkin-<version>-pumpkin-api-<api-version>-wit-<wit-version>-all.jar
```

Example for version `0.1.0`:

```text
j2kt-pumpkin-0.1.1-pumpkin-api-0.1.2-wit-0.1.0-all.jar
```

Publish a release with:

```bash
git tag v0.1.0
git push origin v0.1.0
```

## License

Apache License 2.0. See `LICENSE`.
