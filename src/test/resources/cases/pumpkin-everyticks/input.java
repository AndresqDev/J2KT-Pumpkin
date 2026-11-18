package example;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import pumpkin.TextComponent;
import java.util.List;
public class EveryTaskPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("t", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        getTasks().afterTicks(20, server -> server.broadcast("once"));
        getTasks().everyTicks(0, 20, server -> server.broadcast("tick"));
    }
}
