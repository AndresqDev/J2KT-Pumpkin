package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.Command
import pumpkin.Text.TextComponent

open class CommandPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("c", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        val command: Command.Command = Command.Command(listOf("hello"), "Greets")
        commands.register(context, command, "example.hello", j2ktLambda1@ { sender, server, args ->
            sender.sendMessage(TextComponent.text("Hello"))
            return@j2ktLambda1 Result.success(1)
        })
        return@runCatching Unit
    }

}
