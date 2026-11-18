package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.Event
import pumpkin.Logging
import pumpkin.Player.Player
import pumpkin.Text.TextComponent

/**
 * A Java plugin written against the Pumpkin stub API.
 *
 * Compile it in IntelliJ against the `pumpkin-stub` artifact, then run
 * j2kt with `--target pumpkin` to obtain Kotlin that follows the real
 * Pumpkin Kotlin API shape.
 */
open class ExamplePlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("example-plugin", "0.1.0", listOf("Andres"), "Example plugin written in Java", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        Logging.log(Logging.Level.INFO, "Loading example plugin")
        events.listen(context, Event.EventType.PLAYER_JOIN_EVENT, Event.EventPriority.NORMAL, true, j2ktLambda1@ { server, event ->
            if (event is Event.Event.PlayerJoinEvent) {
                val player: Player = event.value.player
                player.sendSystemMessage(TextComponent.text("Welcome, " + player.getName()), false)
            }
            return@j2ktLambda1 event
        })
        tasks.afterTicks((20).toULong(), j2ktLambda2@ { server ->
            server.broadcast(TextComponent.text("20 ticks later").getText())
        })
        return@runCatching Unit
    }

    override fun onUnload(context: PluginContext): Result<Unit> = runCatching {
        Logging.log(Logging.Level.INFO, "Unloading example plugin")
        return@runCatching Unit
    }

}
