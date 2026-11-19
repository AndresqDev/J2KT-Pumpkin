# Java to Kotlin edge cases

This document records translation behavior that differs from Java or requires a deliberate Pumpkin API choice.

## Nullability

Java reference types may be null. Kotlin types are non-null by default. The translator maps annotated nullable values when possible and emits diagnostics when the result requires review.

## Equality

Java `==` on references is translated to Kotlin `===` in strict mode. Java `.equals` is translated to Kotlin `==`. Use `--value-equality` to translate Java reference equality as Kotlin value equality when that is the intended behavior.

## Numeric conversions

Kotlin does not perform implicit numeric widening. The translator inserts explicit conversions for mixed-width arithmetic and primitive casts.

## Static members

Java static fields and methods map to Kotlin companion objects. `static final` literals map to `const val` when possible.

## Constructors and final fields

A single constructor assigning final fields from same-named parameters may become a Kotlin primary constructor. Multiple constructors or constructor-side effects may require manual review.

A utility class with only a private no-argument constructor currently produces an ambiguous Kotlin constructor. Prefer an abstract utility class or a normal class with a public constructor.

Private promoted fields are not accessible through generated getters from another generated class. Keep internal data holders package-private or expose fields explicitly.

## Collections and arrays

Java collections map to mutable Kotlin collections. `List.of`, `Set.of`, and `Map.of` map to immutable Kotlin collections where the target type allows it. `remove(int)` maps to `removeAt(int)`; `remove(Object)` maps to `remove`.

## Strings

Common string methods are mapped. JVM-only methods such as `String.format` and `printf` are not available in Kotlin/Wasm and produce diagnostics or `TODO`.

## Switch

Simple statement and expression switches map to `when`. Fallthrough is supported for assignments and simple branches. Fallthrough containing control flow is rejected with a diagnostic instead of being translated incorrectly.

## Exceptions

`try`/`catch`/`finally`, multi-catch, and try-with-resources are translated. `try-with-resources` uses Kotlin `use`.

## Synchronization and threads

`Thread`, `Executor`, `CompletableFuture`, `synchronized`, and `volatile` are not supported in Wasm. Replace them with Pumpkin scheduler tasks or coroutine-based designs.

## Unsupported JVM APIs

The following are not available in Kotlin/Wasm and require replacement or manual review:

- `java.lang.reflect`
- `java.io` and `java.nio`
- `java.time`
- `java.util.concurrent`
- `BigDecimal`
- JVM `UUID` APIs
- JVM `String.format`

## Pumpkin commands

Command registration is supported. The compatibility layer attaches a handler ID before calling `context.registerCommand`. Command permissions must use the plugin metadata namespace.

Raw command arguments are supported through the Pumpkin Abs greedy-argument adapter. Typed argument nodes and suggestions remain partially mapped.

## Pumpkin events and tasks

All 273 event variants are generated in the stub. Event registration and dispatch use `PumpkinCompatPlugin`. Delayed tasks map to the generated `tasks` API. Repeating tasks use `pumpkin-compat`.

## Resource ownership

Generated Kotlin/Wasm wrappers consume resource handles passed to methods. Do not reuse a `Player` or `TextComponent` after passing it to an API method. Use `BossBar.removeAll()` to close a boss bar without reusing a consumed player handle.

## Async WIT

`pumpkin:plugin@0.2.0` uses an async ABI. The Kotlin binding generator currently fails on async exports. J2KT targets the synchronous `pumpkin:plugin@0.1.0` ABI only.

## World generation

World generation, chunk generators, and generation phase hooks are outside the project scope.
