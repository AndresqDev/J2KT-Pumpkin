package example;

import java.util.List;
import pumpkin.Command;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import pumpkin.Server;
import pumpkin.TextComponent;

public class MethodRefPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() {
        return new PluginMetadata("method-ref", "1", List.of(), "", List.of(), List.of());
    }

    public void onLoad(PluginContext context) {
        getCommands().register(
                context,
                new Command(List.of("method"), "Method reference command"),
                "example.method",
                MethodRefPlugin::handle);
    }

    public static int handle(Command.CommandSender sender, Server server, Command.ConsumedArgs args) {
        sender.sendMessage(TextComponent.text("method reference"));
        return 1;
    }
}
