package example.packets;

import pumpkin.JavaPackets;
import pumpkin.JavaPlayer;
import pumpkin.Player;
import pumpkin.Server;
import pumpkin.TextComponent;

/** Packet example 5: fan out a packet to every online Java player. */
public final class PacketBroadcastExample {
    public static int sendToAll(Server server) {
        int sent = 0;
        for (Player player : server.getAllPlayers()) {
            JavaPlayer javaPlayer = player.asJava();
            if (javaPlayer != null) {
                javaPlayer.sendPacket(new JavaPackets.CCustomPayload(
                        "j2kt:broadcast", new byte[]{(byte) sent, 1, 2}));
                player.sendMessage(TextComponent.text("packet " + sent));
                sent++;
            }
        }
        return sent;
    }
}
