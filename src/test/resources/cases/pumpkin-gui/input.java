package example;
import pumpkin.*;
import java.util.List;
public class GuiPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() { return new PluginMetadata("g", "1", List.of(), "", List.of(), List.of()); }
    public void onLoad(PluginContext context) {
        Player player = context.getServer().getPlayer("x");
        Gui gui = new Gui(Screens.Screen.GENERIC_9X3, TextComponent.text("Menu"));
        gui.setItem(0, ItemStack.of("minecraft:stone", 1));
        gui.setAllowGrabItems(false);
        if (player != null) {
            player.openGui(gui);
        }
    }
}
