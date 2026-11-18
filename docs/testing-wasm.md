# Testing Java to Kotlin to Wasm

The repository contains two Wasm integration projects:

- `wasm-tests/uc1`: five API use cases.
- `wasm-tests/packets`: five packet examples.

Each project compiles translated Java against Pumpkin API `0.1.2` and the Pumpkin Gradle plugin `0.1.2`.

## Run

```bash
./scripts/test-wasm.sh
```

The script publishes `pumpkin-api-kt` to Maven Local when it is not already present. Set `PUMPKIN_API_KT_DIR` to the API checkout when it is not at `/tmp/opencode/pumpkin-api-kt`.

It then:

1. Builds the translator and the bundled stub.
2. Translates the Java examples to Kotlin.
3. Builds both Kotlin/Wasm projects.
4. Validates the resulting components with the Pumpkin Gradle plugin.

Artifacts:

```text
wasm-tests/uc1/build/uc1.wasm
wasm-tests/packets/build/packets.wasm
```

## Coverage

| Area | Project |
|---|---|
| Lifecycle, metadata, logging | Core |
| Permissions, commands, suggestions | Core |
| Events, cancellation, unregister | Core / Moderation |
| Tasks and cancellation | Core / Moderation |
| IPC and AI goals | Core / Moderation |
| Text, player, server, world | Core / World |
| Inventory, GUI, forms, dialogs | Inventory |
| Scoreboard and teams | World |
| Blocks, entities, mobs, display entities | World |
| Recipes, datapacks, enchantments | Content |
| Boss bars | Runtime validation in TPSBar Java |
| Java and Bedrock packets | Packets |
| Data folder path | Core |

World generation is outside the project scope.

## Limits

The suite validates translation, Kotlin compilation, WIT component generation, and Wasm validation. It does not run a Pumpkin server or simulate a Minecraft client. Runtime behavior is tested separately by the TPSBar Java plugin against a Pumpkin nightly server.
