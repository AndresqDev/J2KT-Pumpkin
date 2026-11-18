package example;
import pumpkin.Command;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import pumpkin.TextComponent;
import java.util.List;
public class CommandPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("c", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        Command command = new Command(List.of("hello"), "Greets");
        getCommands().register(context, command, "example.hello", (sender, server, args) -> {
            sender.sendMessage(TextComponent.text("Hello"));
            return 1;
        });
    }
}
