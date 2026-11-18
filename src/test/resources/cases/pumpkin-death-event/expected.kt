package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.*
import pumpkin.Event
import pumpkin.Logging
import pumpkin.Player.Player

open class DeathPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("d", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        events.listen(context, Event.EventType.PLAYER_DEATH_EVENT, Event.EventPriority.NORMAL, true, j2ktLambda1@ { server, event ->
            if (event is Event.Event.PlayerDeathEvent) {
                val player: Player = event.value.player
                Logging.log(Logging.Level.INFO, player.getName())
            }
            return@j2ktLambda1 event
        })
        return@runCatching Unit
    }

}
