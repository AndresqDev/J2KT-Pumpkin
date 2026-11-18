package plugin

import pumpkin.Context.Context

/**
 * Returns the plugin's private data folder as provided by the host.
 *
 * Kotlin/Wasm in these bindings does not expose a standard filesystem API, so
 * this module intentionally stops at path resolution. Read/write operations
 * must use whatever WASI filesystem capability the plugin host grants (or a
 * host-provided IPC/API). Keeping the path helper here avoids hard-coding a
 * folder layout in generated plugins.
 */
fun Context.dataFolder(): String = getDataFolder()

/** Joins a file name to the plugin data folder without assuming a trailing slash. */
fun Context.dataPath(name: String): String {
    val base = getDataFolder().trimEnd('/')
    return if (base.isEmpty()) name else "$base/$name"
}
