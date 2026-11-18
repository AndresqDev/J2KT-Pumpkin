package plugin

import pumpkin.JavaPackets
import pumpkin.Player

/**
 * Sends application-defined bytes through the Java custom-payload packet
 * (the portable "raw payload" mechanism exposed by Pumpkin's WIT API).
 */
fun Player.JavaPlayer.sendCustomPayload(channel: String, data: ByteArray) {
    sendPacket(
        JavaPackets.ClientboundPacket.CCustomPayload(
            JavaPackets.CCustomPayload(channel, data.map { it.toUByte() })
        )
    )
}
