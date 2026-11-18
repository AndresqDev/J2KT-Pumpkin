package example

import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.*
import pumpkin.BedrockPackets
import pumpkin.JavaPackets
import pumpkin.Player.Player

open class RawPacketPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("raw", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        val player: Player? = context.getServer().getPlayerByName("x")
        if (player != null) {
            val java: pumpkin.Player.JavaPlayer? = player.asJava()
            if (java != null) {
                java.sendPacket(JavaPackets.ClientboundPacket.CCustomPayload(JavaPackets.CCustomPayload("my:channel", byteArrayOf(1, 2, 3).map { it.toUByte() })))
            }
            val bedrock: pumpkin.Player.BedrockPlayer? = player.asBedrock()
            if (bedrock != null) {
                bedrock.sendPacket(BedrockPackets.ClientboundPacket.Unknown)
            }
        }
        return@runCatching Unit
    }

}
