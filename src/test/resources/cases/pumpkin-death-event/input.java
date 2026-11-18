package example;
import pumpkin.*;
import java.util.List;
public class DeathPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("d", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        getEvents().listen(context, EventType.PLAYER_DEATH_EVENT, EventPriority.NORMAL, true, (server, event) -> {
            if (event instanceof Event.PlayerDeathEvent death) {
                Player player = death.getPlayer();
                Logging.log(Logging.Level.INFO, player.getName());
            }
            return event;
        });
    }
}
