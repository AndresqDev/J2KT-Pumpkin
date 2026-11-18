package example.packets;

import pumpkin.BedrockPackets;
import pumpkin.BedrockPlayer;
import pumpkin.Player;
import pumpkin.Server;

/** Packet example 3: send typed Bedrock packets and the Unknown marker. */
public final class BedrockPacketExample {
    public static void send(Server server) {
        Player player = server.getPlayer("j2kt-tester");
        if (player == null) {
            return;
        }
        BedrockPlayer bedrock = player.asBedrock();
        if (bedrock == null) {
            return;
        }
        bedrock.sendPacket(new BedrockPackets.CDisconnect(
                0, false, "j2kt disconnect", "j2kt disconnect"));
        bedrock.sendPacket(new BedrockPackets.CSetTime(6000));
        bedrock.sendPacket(new BedrockPackets.CModalFormRequest(1, "{}"));
        bedrock.sendPacket(new BedrockPackets.Unknown());
    }
}
