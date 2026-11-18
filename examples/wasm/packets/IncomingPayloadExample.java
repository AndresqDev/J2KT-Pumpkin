package example.packets;

import pumpkin.Event;
import pumpkin.EventPriority;
import pumpkin.EventType;
import pumpkin.JavaPackets;
import pumpkin.JavaPlayer;
import pumpkin.Logging;
import pumpkin.Player;
import pumpkin.PluginContext;
import pumpkin.PumpkinPlugin;
import pumpkin.TextComponent;

/** Packet example 4: consume a Java custom-payload event and reply. */
public final class IncomingPayloadExample {
    public static void install(PumpkinPlugin plugin, PluginContext context) {
        plugin.getEvents().listen(
                context,
                EventType.PLAYER_CUSTOM_PAYLOAD_EVENT,
                EventPriority.NORMAL,
                false,
                (server, event) -> {
                    if (event instanceof Event.PlayerCustomPayloadEvent payload) {
                        String channel = payload.getChannel();
                        Logging.log(Logging.Level.INFO, "received payload " + channel);
                        if (channel != null && channel.equals("j2kt:ping")) {
                            Player player = payload.getPlayer();
                            JavaPlayer javaPlayer = player.asJava();
                            if (javaPlayer != null) {
                                javaPlayer.sendPacket(new JavaPackets.CCustomPayload(
                                        "j2kt:pong", new byte[]{4, 3, 2, 1}));
                            }
                        }
                    }
                    return event;
                });
    }
}
