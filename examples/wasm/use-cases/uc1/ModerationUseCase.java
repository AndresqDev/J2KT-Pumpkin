package example.uc1;

import java.util.List;
import pumpkin.Event;
import pumpkin.EventPriority;
import pumpkin.EventSubscription;
import pumpkin.EventType;
import pumpkin.Logging;
import pumpkin.Player;
import pumpkin.PluginContext;
import pumpkin.PumpkinPlugin;
import pumpkin.ScheduledTask;
import pumpkin.Server;
import pumpkin.TextComponent;

/**
 * Use case 5: moderation/communication flows. It registers events, schedules
 * work, handles IPC and guards every player-specific action, so it is safe to
 * load on an empty server and test without a live client.
 */
public final class ModerationUseCase {
    private static EventSubscription chatSubscription;
    private static ScheduledTask cleanupTask;

    public static void install(PumpkinPlugin plugin, PluginContext context) {
        Server server = context.getServer();

        chatSubscription = plugin.getEvents().listen(
                context,
                EventType.PLAYER_CHAT_EVENT,
                EventPriority.HIGH,
                true,
                (srv, event) -> {
                    if (event instanceof Event.PlayerChatEvent chat) {
                        String message = chat.getMessage();
                        if (message != null && message.startsWith("/j2ktmute ")) {
                            chat.setCancelled(true);
                            Player player = chat.getPlayer();
                            player.sendMessage(TextComponent.text("muted by j2kt"));
                        }
                    }
                    return event;
                });

        cleanupTask = plugin.getTasks().afterTicks(200, srv -> {
            srv.broadcastMessage(TextComponent.text("j2kt moderation tick"));
            Player player = srv.getPlayer("j2kt-tester");
            if (player != null) {
                player.sendMessage(TextComponent.text("moderation tick"));
            }
        });

        plugin.getIpc().sendIpcMessage("j2kt-moderation", new byte[]{9, 8, 7});
        plugin.getIpc().onMessage((sender, message) -> {
            Logging.log(Logging.Level.INFO, "moderation ipc from " + sender);
            return message;
        });

        Player player = server.getPlayer("j2kt-tester");
        if (player != null) {
            player.teleport(new pumpkin.Position(0.0, 65.0, 0.0));
            player.sendMessage(TextComponent.text("welcome to the moderation use case"));
        }
    }
}
