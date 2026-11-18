package example;

import pumpkin.Event;
import pumpkin.EventPriority;
import pumpkin.EventType;
import pumpkin.Logging;
import pumpkin.Player;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import pumpkin.TextComponent;

import java.util.List;

/**
 * A Java plugin written against the Pumpkin stub API.
 *
 * <p>Compile it in IntelliJ against the {@code pumpkin-stub} artifact, then run
 * j2kt with {@code --target pumpkin} to obtain Kotlin that follows the real
 * Pumpkin Kotlin API shape.</p>
 */
public class ExamplePlugin extends PumpkinPlugin {
    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "example-plugin",
                "0.1.0",
                List.of("Andres"),
                "Example plugin written in Java",
                List.of(),
                List.of()
        );
    }

    @Override
    public void onLoad(PluginContext context) {
        Logging.log(Logging.Level.INFO, "Loading example plugin");

        getEvents().listen(context, EventType.PLAYER_JOIN_EVENT, EventPriority.NORMAL, true, (server, event) -> {
            if (event instanceof Event.PlayerJoinEvent join) {
                Player player = join.getPlayer();
                player.sendMessage(TextComponent.text("Welcome, " + player.getName()));
            }
            return event;
        });

        getTasks().afterTicks(20, server -> {
            server.broadcastMessage(TextComponent.text("20 ticks later"));
        });
    }

    @Override
    public void onUnload(PluginContext context) {
        Logging.log(Logging.Level.INFO, "Unloading example plugin");
    }
}
