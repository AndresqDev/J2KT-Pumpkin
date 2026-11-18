package example.packets;

import pumpkin.JavaPackets;
import pumpkin.JavaPlayer;
import pumpkin.Player;
import pumpkin.Server;

/** Packet example 2: send several typed Java clientbound packets. */
public final class JavaTypedPacketExample {
    public static void send(Server server) {
        Player player = server.getPlayer("j2kt-tester");
        if (player == null) {
            return;
        }
        JavaPlayer javaPlayer = player.asJava();
        if (javaPlayer == null) {
            return;
        }
        javaPlayer.sendPacket(new JavaPackets.CPlayDisconnect("j2kt says goodbye"));
        javaPlayer.sendPacket(new JavaPackets.CKeepAlive(123456789L));
        javaPlayer.sendPacket(new JavaPackets.CGameEvent(1, 0.0f));
        javaPlayer.sendPacket(new JavaPackets.CTransfer("localhost", 25565));
    }
}
