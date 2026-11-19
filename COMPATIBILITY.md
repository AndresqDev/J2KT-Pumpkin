# Version compatibility

## Supported matrix

| Component | Version |
|---|---|
| J2KT Pumpkin | 0.1.4 |
| Pumpkin Stub | 0.2.0 |
| Pumpkin Kotlin/Wasm API | 0.1.2 |
| Pumpkin plugin WIT | `pumpkin:plugin@0.1.0` (synchronous ABI) |
| Kotlin | 2.4.0 |
| Java | 21 |
| Gradle | 9.8.1 |

## Tested Pumpkin server

| Pumpkin | Channel | Result |
|---|---|---|
| `0.2.2+26.3-26.63` | nightly tag `87f8822` | TPSBar Java loaded; commands, boss bar, and cleanup validated |

The v0.1 WIT is the supported ABI for this release.

## Async API 0.2

`pumpkin:plugin@0.2.0` is an async ABI. The Kotlin binding generator currently fails with:

```text
not implemented: async unimplemented
```

J2KT 0.1.x does not produce components for the async v0.2 ABI.

## Versioning

- `j2kt-pumpkin 0.1.x` -> `pumpkin-api-kt 0.1.x` -> WIT v0.1 synchronous ABI.
- `j2kt-pumpkin 0.2.x` is reserved for WIT v0.2 async support.

## Resource ownership

The generated Kotlin/Wasm wrappers consume resource handles passed to methods. A `Player` or `TextComponent` passed to `BossBar.addPlayer` or `BossBar.setTitle` must not be reused. Use `BossBar.removeAll()` to close a bar and create fresh text components for each update.

## Command and permission registration

Commands must attach a handler ID before `context.registerCommand`. Permissions must use the plugin metadata namespace, for example:

```text
tpsbar-java:command.toggle
```

## Known translator constraints

- A Java utility class with only a private no-argument constructor currently produces an ambiguous Kotlin constructor. Use an abstract utility class or a public constructor.
- A private final field promoted to a Kotlin constructor property is not accessible through a getter from another generated class. Keep small internal data holders package-private or expose fields explicitly.
