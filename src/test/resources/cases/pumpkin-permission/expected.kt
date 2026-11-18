package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.Permission

open class PermissionPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("p", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        context.registerPermission(Permission.Permission("example.hello", "Allows hello", Permission.PermissionDefault.Allow, listOf()))
        return@runCatching Unit
    }

}
