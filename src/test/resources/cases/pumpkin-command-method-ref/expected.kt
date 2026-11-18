package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.Command
import pumpkin.Server.Server
import pumpkin.Text.TextComponent

open class MethodRefPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("method-ref", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        commands.register(context, Command.Command(listOf("method"), "Method reference command"), "example.method", { sender, server, args -> Result.success(MethodRefPlugin.handle(sender, server, args)) })
        return@runCatching Unit
    }

    companion object {
        fun handle(sender: Command.CommandSender, server: Server, args: Command.ConsumedArgs): Int {
            sender.sendMessage(TextComponent.text("method reference"))
            return 1
        }

    }
}
