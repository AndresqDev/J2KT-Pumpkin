package example;
import pumpkin.Player;
import pumpkin.Position;
import pumpkin.PumpkinPlugin;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import java.util.List;
public class PositionPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("p", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        Player player = context.getServer().getPlayer("x");
        if (player != null) {
            Position pos = player.getPosition();
            player.teleport(new Position(pos.x() + 1, pos.y(), pos.z()));
        }
    }
}
