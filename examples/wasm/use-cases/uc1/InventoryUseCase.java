package example.uc1;

import java.util.List;
import pumpkin.Forms;
import pumpkin.Gui;
import pumpkin.Inventory;
import pumpkin.ItemStack;
import pumpkin.JavaDialogs;
import pumpkin.JavaPlayer;
import pumpkin.Player;
import pumpkin.PlayerInventory;
import pumpkin.PluginContext;
import pumpkin.Screens;
import pumpkin.Server;
import pumpkin.TextComponent;

/**
 * Use case 3: item stacks, GUI/inventory mutation, player inventory access,
 * Bedrock forms and Java dialogs. Player-only parts are null-guarded, so the
 * plugin can load and build without a connected player.
 */
public final class InventoryUseCase {
    public static void install(PluginContext context) {
        Server server = context.getServer();

        Gui gui = new Gui(Screens.Screen.GENERIC_9X3, TextComponent.text("j2kt inventory"));
        Inventory inventory = gui.getInventory();
        inventory.setItem(0, ItemStack.of("minecraft:stone", 16));
        inventory.setItem(1, ItemStack.of("minecraft:diamond", 3));
        gui.setItem(2, ItemStack.of("minecraft:emerald", 1));
        gui.setAllowGrabItems(false);
        gui.setAllowPutItems(false);

        inventory.countItem("minecraft:stone");
        inventory.containsItem("minecraft:stone");
        inventory.removeItem(1);
        inventory.setAllItems(List.of(
                ItemStack.of("minecraft:stone", 2),
                ItemStack.of("minecraft:diamond", 1)));
        inventory.getAllItems();

        ItemStack first = inventory.getItem(0);
        if (first != null) {
            int count = first.getCount();
            gui.setItem(3, ItemStack.of("minecraft:stone", count));
        }
        int size = gui.getSize();
        if (size > 0) {
            gui.clearItems();
        }

        Player player = server.getPlayer("j2kt-tester");
        if (player != null) {
            PlayerInventory playerInventory = player.getInventory();
            playerInventory.setHelmet(ItemStack.of("minecraft:diamond_helmet", 1));
            playerInventory.setChestplate(ItemStack.of("minecraft:diamond_chestplate", 1));
            playerInventory.setLeggings(ItemStack.of("minecraft:diamond_leggings", 1));
            playerInventory.setBoots(ItemStack.of("minecraft:diamond_boots", 1));
            playerInventory.setOffHand(ItemStack.of("minecraft:shield", 1));
            playerInventory.setSelectedSlot(2);
            int selected = playerInventory.getSelectedSlot();
            Inventory asInventory = playerInventory.asInventory();
            asInventory.getSize();
            player.openGui(gui);
            player.sendMessage(TextComponent.text("inventory use case ready"));
        }

        Forms.Form form = Forms.Form.simple(new Forms.SimpleForm(
                TextComponent.text("j2kt form"),
                TextComponent.text("No player is required to construct this form"),
                List.of(Forms.button("OK"), Forms.button("Cancel"))));

        Forms.Form custom = Forms.Form.custom(new Forms.CustomForm(
                TextComponent.text("j2kt custom form"),
                List.of(Forms.label("Choose an option"),
                        Forms.toggle("Enabled", true),
                        Forms.input("Name", "Player", ""),
                        Forms.dropdown("Mode", List.of("A", "B", "C"), 0))));

        JavaDialogs.Dialog dialog = new JavaDialogs.Dialog(
                TextComponent.text("j2kt dialog"),
                JavaDialogs.DialogType.CONFIRMATION,
                List.of(JavaDialogs.message(TextComponent.text("Confirm?"))),
                List.of(JavaDialogs.boolInput("Enabled", true)),
                List.of(JavaDialogs.button(TextComponent.text("OK"),
                        JavaDialogs.openUrl("https://example.com"))),
                List.of(JavaDialogs.builtInLink(JavaDialogs.LinkType.WEBSITE, "https://example.com")),
                JavaDialogs.AfterAction.PEEK,
                true,
                TextComponent.text("j2kt"));

        if (player != null) {
            JavaPlayer javaPlayer = player.asJava();
            if (javaPlayer != null) {
                javaPlayer.showDialog(dialog);
            }
        }
    }
}
