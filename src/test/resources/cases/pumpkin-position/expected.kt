package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.Player.Player

open class PositionPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("p", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        val player: Player? = context.getServer().getPlayerByName("x")
        if (player != null) {
            val pos: Triple<Double, Double, Double> = player.getPosition()
            player.teleport(Triple<Double, Double, Double>(pos.first + 1, pos.second, pos.third), null, null, player.getWorld())
        }
        return@runCatching Unit
    }

}
