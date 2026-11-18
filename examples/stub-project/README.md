# Pumpkin Stub Consumer Example

Standalone Java project that compiles a plugin against the bundled stub artifact.

```bash
# from the repository root
./gradlew clean build
cd examples/stub-project
gradle build
```

The example uses:

```text
../../libs/pumpkin-stub.jar
```

For use outside this repository, download the stub JAR from:

```text
https://github.com/AndresqDev/Pumpkin-Stub/releases
```

The example only validates Java compilation against the stub. Translation and Wasm packaging are performed by J2KT.
