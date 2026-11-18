package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.Command

open class SuggestionPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("s", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        val node: Command.CommandNode = Command.CommandNode.literal("hello")
        commands.onSuggestions(node, { sender, server, request -> Command.CommandSuggestions((0).toUInt(), (0).toUInt(), listOf()) })
        return@runCatching Unit
    }

}
