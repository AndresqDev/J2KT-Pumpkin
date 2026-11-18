# Pumpkin Stub

The canonical Java stub is maintained in a separate repository:

```text
https://github.com/AndresqDev/Pumpkin-Stub
```

J2KT uses `libs/pumpkin-stub.jar` for translator and test compilation. The stub is a compile-time API only; it does not execute plugin code.

## Local use in this repository

The bundled JAR is used by the translator tests:

```text
libs/pumpkin-stub.jar
```

Rebuild it from the stub repository:

```bash
./gradlew clean build
```

Copy the resulting `build/libs/pumpkin-stub-0.1.0.jar` into this repository as `libs/pumpkin-stub.jar` when the stub API changes.

## Validation

After replacing the stub JAR, run:

```bash
./gradlew clean test
./scripts/test-wasm.sh
```

The stub must be validated against the generated Pumpkin Kotlin/Wasm API before it is used for a release.
