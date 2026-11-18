package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin

open class IpcPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("i", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        ipc.sendIpcMessage("other-plugin", byteArrayOf(1, 2, 3))
        ipc.onMessage(j2ktLambda1@ { sender, message ->
            return@j2ktLambda1 message
        })
        return@runCatching Unit
    }

}
