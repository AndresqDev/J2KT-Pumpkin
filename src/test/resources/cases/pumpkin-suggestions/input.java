package example;
import pumpkin.Command;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import java.util.List;
public class SuggestionPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("s", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        Command.CommandNode node = Command.CommandNode.literal("hello");
        getCommands().onSuggestions(node, (sender, server, request) -> new Command.CommandSuggestions(0, 0, List.of()));
    }
}
