package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.*
import pumpkin.ItemStack.ItemStack
import pumpkin.Player.Player
import pumpkin.Text.TextComponent

open class HelperPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("h", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        val player: Player? = context.getServer().getPlayerByName("x")
        val stack: ItemStack = ItemStack("minecraft:stone", (2).toUByte())
        if (player != null) {
            player.sendSystemMessage(TextComponent.text("Hello"), false)
            player.getInventory().setItem((0).toUInt(), stack)
        }
        return@runCatching Unit
    }

}
