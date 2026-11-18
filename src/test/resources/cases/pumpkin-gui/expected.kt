package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.*
import pumpkin.Gui
import pumpkin.ItemStack.ItemStack
import pumpkin.Player.Player
import pumpkin.Screens
import pumpkin.Text.TextComponent

open class GuiPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("g", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        val player: Player? = context.getServer().getPlayerByName("x")
        val gui: Gui.Gui = Gui.Gui(Screens.Screen.GENERIC_9X3, TextComponent.text("Menu"))
        gui.setItem((0).toUInt(), ItemStack("minecraft:stone", (1).toUByte()))
        gui.setAllowGrabItems(false)
        if (player != null) {
            player.openGui(gui)
        }
        return@runCatching Unit
    }

}
