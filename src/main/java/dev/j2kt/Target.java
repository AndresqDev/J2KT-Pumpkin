package dev.j2kt;

/** The runtime contract the generated Kotlin is intended for. */
public enum Target {
    /** Ordinary Kotlin/JVM. Keeps java.* APIs and JVM-only constructs. */
    JVM,
    /**
     * Java code written against the project's Pumpkin stub API, translated to the
     * Kotlin/Wasm-WASI API shape documented by Pumpkin.
     */
    PUMPKIN
}
