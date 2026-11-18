# Pumpkin Java to Kotlin to Wasm IntelliJ Template

This is a self-contained IntelliJ IDEA Gradle project for building a Pumpkin plugin from Java.

The source language is Java. The build translates it to Kotlin, compiles the Kotlin against the real Pumpkin API, and packages a WebAssembly component.

## Compatibility

| Component | Version |
|---|---|
| Pumpkin Kotlin/Wasm API | 0.1.2 |
| Pumpkin plugin WIT | `pumpkin:plugin@0.1.0` synchronous |
| Pumpkin Gradle plugin | 0.1.2 |
| Kotlin | 2.4.0 |
| Java | 21 |
| Gradle | 9.8.1 |
| Tested Pumpkin | nightly `0.2.2+26.3-26.63` (tag `87f8822`) |

Official Pumpkin 0.2.1+ and 0.2.2+ servers expose the synchronous v0.1 compatibility ABI.
Strict API `0.2.0` is async and the current Kotlin binding generator does not support async
exports; this template targets the v0.1 compatibility ABI.

## Open in IntelliJ IDEA

1. Open this directory as a Gradle project.
2. Select the **Build Plugin WASM** run configuration.
3. Press Run.

The same build can be started from a terminal:

```bash
./gradlew build --console=plain
```

or, for the focused clean + wasm pipeline:

```bash
./gradlew cleanBuildWasm --console=plain
```

## What the build does

The **Build Plugin WASM** configuration runs the `cleanBuildWasm` Gradle task. IntelliJ's default Gradle **Build** action is also wired to clean previous outputs and build the wasm component.

1. Compile `java-plugin/src/main/java` against the bundled Pumpkin stub.
2. Translate Java to Kotlin with the bundled `j2kt-pumpkin` translator.
3. Compile the generated Kotlin against Pumpkin API `0.1.2`.
4. Package and validate the WebAssembly component.

The build is incremental and uses Gradle's build cache. The translator, stub, and Pumpkin API artifacts are bundled in `libs/`, so the project does not need a separate translator checkout.

## Rename the final wasm

Set the artifact name in `gradle.properties`:

```properties
pumpkinWasmName=pumpkin-java-template
```

Changing it to, for example, `my-plugin` writes `target/wasm/my-plugin.wasm`. Old wasm files in
`target/wasm` are removed before the new artifact is written.

## Output layout

```text
target/
├── java/classes/                 # javac output
├── kotlin/com/example/template/  # generated Kotlin, package-aligned
├── wasm/<pumpkinWasmName>.wasm
└── traces/
    ├── translation-report.md
    ├── build-summary.txt
    ├── java-sources.txt
    └── kotlin-sources.txt
```

`target/traces/build-summary.txt` records the API version, plugin class, wasm size, and SHA-256.
Kotlin output follows Java package paths; classes with the same simple name in different packages are written to different directories.

## Plugin sources

```text
java-plugin/src/main/java/com/example/template/
├── TemplatePlugin.java
├── commands/
│   ├── PingCommand.java          # /ping -> pong
│   └── HiCommand.java            # /hi  -> colored greeting
└── listeners/
    └── JoinListener.java         # player join listener
```

The `hi` message is:

```text
Hello, World, from Java → Kotlin → Wasm!
```

It uses a gold and yellow Pumpkin-style gradient and bold text.

## Java API notes

- Commands must attach a handler ID before registration; the bundled compatibility
  helper already does this.
- Permission nodes must use the plugin metadata namespace, for example
  `pumpkin-java-template:template.use` for `pumpkin-java-template`.
- Generated bindings consume resource handles passed to methods. Do not reuse a
  `Player` or `TextComponent` after passing it to methods such as `BossBar.addPlayer`
  or `BossBar.setTitle`.

## Requirements

- JDK 21
- Gradle through the included wrapper
- Network access the first time Gradle resolves the Kotlin Gradle plugin and Kotlin dependencies

The first build compiles the Pumpkin API sources and can take several minutes. Later incremental builds are much faster.

## Clean

```bash
./gradlew clean
```

This deletes both `build/` and `target/`.
