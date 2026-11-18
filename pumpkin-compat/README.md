# pumpkin-compat

Kotlin helpers for the generated Pumpkin Kotlin/Wasm API. This module does not replace the generated bindings; it adds registration, builders, and small adapters.

The module is a source set, not a published artifact. Copy the required files into a plugin source set or add the directory as a Kotlin source directory.

```text
pumpkin-compat/src/wasmWasiMain/kotlin/plugin/
```

## Included helpers

| Area | Status |
|---|---|
| Events | `events.listen`, dispatch, `EventSubscription` |
| Commands | `commands.register`, handler ID registration, suggestions |
| Tasks | `tasks.afterTicks`, `compatTasks.everyTicks`, cancellation |
| IPC | `ipc.sendIpcMessage`, `ipc.onMessage` |
| AI goals | `AiGoal`, `AiGoalRegistry` |
| Text | text builders and styles |
| Items and inventories | item helpers, inventories, armor, offhand, hotbar |
| Player | messages, teleport, position |
| GUI and screens | builders, slots, and flags |
| Scoreboard and teams | objectives, scores, slots, teams |
| Forms and dialogs | Bedrock forms, Java Edition dialogs |
| Recipes and datapacks | shaped, shapeless, cooking, pack control |
| Display entities | block, item, and text display helpers |
| Blocks and mobs | lookup, health, navigation, AI goals |
| Enchantments | custom enchantment builders |
| Persistent data | data folder path only |

World generation is not implemented.

## Command registration

`CommandRegistry.register` attaches the command handler ID before calling `context.registerCommand`. The permission must use the plugin metadata namespace:

```text
<plugin-name>:<permission>
```

## Task registration

Use `tasks.afterTicks` for one-shot tasks and `compatTasks.everyTicks` for repeating tasks. Both return a `ScheduledTask` that can be cancelled.

## Resource ownership

Generated API methods consume resource handles. Do not reuse a `Player` or `TextComponent` after passing it to a method such as `BossBar.addPlayer` or `BossBar.setTitle`. Use `BossBar.removeAll()` to close a bar.

## Validation

The helpers are compiled and validated by the J2KT Wasm integration suite and by the IntelliJ template. Runtime validation is performed against Pumpkin nightly `0.2.2+26.3-26.63` by the TPSBar Java plugin.
