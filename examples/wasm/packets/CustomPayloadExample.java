package example.packets;

import pumpkin.JavaPackets;
import pumpkin.JavaPlayer;
import pumpkin.Player;
import pumpkin.Server;

/** Packet example 1: send plugin-defined bytes over a Java custom payload. */
public final class CustomPayloadExample {
    public static void send(Server server) {
        Player player = server.getPlayer("j2kt-tester");
        if (player == null) {
            return;
        }
        JavaPlayer javaPlayer = player.asJava();
        if (javaPlayer != null) {
            javaPlayer.sendPacket(new JavaPackets.CCustomPayload(
                    "j2kt:custom-payload", new byte[]{1, 2, 3, 4}));
        }
    }
}
