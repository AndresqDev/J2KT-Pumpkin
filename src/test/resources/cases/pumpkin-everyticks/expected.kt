package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin

open class EveryTaskPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("t", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        tasks.afterTicks((20).toULong(), { server -> server.broadcast("once") })
        compatTasks.everyTicks((0).toULong(), (20).toULong(), { server -> server.broadcast("tick") })
        return@runCatching Unit
    }

}
