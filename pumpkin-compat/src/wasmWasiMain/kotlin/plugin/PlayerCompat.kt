package plugin

import pumpkin.Player
import pumpkin.Text

/** Convenience operations over Player.Player. */
fun Player.Player.send(message: String) {
    sendSystemMessage(Text.TextComponent.text(message), false)
}

fun Player.Player.teleport(position: Triple<Double, Double, Double>) {
    teleport(position, null, null, getWorld())
}

fun Player.Player.position(): Triple<Double, Double, Double> = getPosition()
