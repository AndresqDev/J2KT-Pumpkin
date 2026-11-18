package example;
import pumpkin.*;
import java.util.List;
public class HelperPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("h", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        Player player = context.getServer().getPlayer("x");
        ItemStack stack = ItemStack.of("minecraft:stone", 2);
        if (player != null) {
            player.send("Hello");
            player.getInventory().setItem(0, stack);
        }
    }
}
