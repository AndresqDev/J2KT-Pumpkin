package example;

import java.util.List;
import java.util.Map;
import pumpkin.*;

public class AdvancedPlugin extends PumpkinPlugin {
    public PluginMetadata metadata() {
        return new PluginMetadata("advanced", "1", List.of(), "", List.of(), List.of());
    }

    public void onLoad(PluginContext context) {
        Server server = context.getServer();
        World world = server.getWorld("world");
        if (world == null) {
            return;
        }

        Scoreboard board = world.getScoreboard();
        board.addObjective("kills", TextComponent.text("Kills"), Scoreboard.RenderType.INTEGER, null);
        board.updateScore("Steve", "kills", 3, null);
        board.setDisplaySlot(Scoreboard.DisplaySlot.SIDEBAR, "kills");
        Scoreboard.TeamSettings settings = new Scoreboard.TeamSettings(
                TextComponent.text("Red"), false, false,
                Scoreboard.NametagVisibility.ALWAYS, Scoreboard.CollisionRule.ALWAYS,
                Common.NamedColor.RED, TextComponent.text(""), TextComponent.text(""));
        board.createTeam("red", settings);

        Player player = server.getPlayer("Steve");
        Forms.Form form = Forms.Form.simple(new Forms.SimpleForm(
                TextComponent.text("Title"), TextComponent.text("Body"),
                List.of(Forms.button("OK"), Forms.button("Cancel", Forms.image(Forms.ImageType.PATH, "textures/ui/cancel")))));
        if (player != null) {
            BedrockPlayer bedrock = player.asBedrock();
            if (bedrock != null) {
                bedrock.openForm(form);
            }
        }
        Forms.Form custom = Forms.Form.custom(new Forms.CustomForm(
                TextComponent.text("Custom"),
                List.of(Forms.label("Choose"), Forms.toggle("Enabled", true), Forms.input("Name", "Steve", ""))));

        Recipe.RecipeManager recipes = server.getRecipeManager();
        recipes.registerShaped("demo:block", new Recipe.ShapedRecipe(
                List.of("###", "###", "###"),
                Map.of("#", Recipe.item("minecraft:stone")),
                ItemStack.of("minecraft:stone", 1), null,
                Recipe.RecipeCategory.BUILDING, true));
        recipes.registerShapeless("demo:shapeless", new Recipe.ShapelessRecipe(
                List.of(Recipe.item("minecraft:stone"), Recipe.tag("minecraft:planks")),
                ItemStack.of("minecraft:stone", 2), null, Recipe.RecipeCategory.MISC));
        recipes.registerCooking("demo:ingot", Recipe.CookingType.SMELTING, new Recipe.CookingRecipe(
                Recipe.item("minecraft:iron_ore"), ItemStack.of("minecraft:iron_ingot", 1),
                0.7f, 200, null, null));

        Datapack.DatapackManager packs = server.getDatapackManager();
        packs.enablePack("demo", Datapack.before("vanilla"));
        Datapack.DatapackInfo info = packs.getPack("demo");
        if (info != null) {
            int recipesInPack = info.recipeCount;
        }

        Enchantments.CustomEnchantment enchantment = new Enchantments.CustomEnchantment(
                "demo:lifesteal", TextComponent.text("Lifesteal"), 3, 1,
                "#minecraft:enchantable/weapon", 5,
                List.of(Enchantments.AttributeModifierSlot.MAIN_HAND), List.of());
        server.getEnchantmentManager().registerEnchantment(enchantment);
        int maxLevel = enchantment.maxLevel;

        JavaPlayer javaPlayer = player != null ? player.asJava() : null;
        JavaDialogs.Dialog dialog = new JavaDialogs.Dialog(
                TextComponent.text("Dialog"),
                JavaDialogs.DialogType.NOTICE,
                List.of(JavaDialogs.message(TextComponent.text("Hello"))),
                List.of(JavaDialogs.boolInput("Enabled", true), JavaDialogs.textInput("Name", "Steve", "")),
                List.of(JavaDialogs.button(TextComponent.text("OK"),
                        JavaDialogs.openUrl("https://example.com"))),
                List.of(JavaDialogs.builtInLink(JavaDialogs.LinkType.WEBSITE, "https://example.com")),
                null, true, null);
        JavaDialogs.DialogInput optionInput = JavaDialogs.optionInput(
                "Option", List.of("A", "B"), 1);
        JavaDialogs.DialogInput numberInput = JavaDialogs.numberInput(
                "Amount", 1.0f, 64.0f, 1.0f, 1.0f, null);
        JavaDialogs.Action customAction = JavaDialogs.customClick("demo:action");
        JavaDialogs.ActionButton customButton = JavaDialogs.button(
                TextComponent.text("Custom"), customAction);
        JavaDialogs.Link customLink = JavaDialogs.customLink(
                TextComponent.text("Site"), "https://example.com");
        JavaDialogs.DialogBody itemBody = JavaDialogs.itemBody(
                ItemStack.of("minecraft:stone", 1));
        if (javaPlayer != null) {
            javaPlayer.showDialog(dialog);
        }

        World.Block block = world.getBlockByName("minecraft:stone");
        if (block != null) {
            int state = block.defaultStateId;
            int mapColor = block.mapColor;
        }

        World.Entity entity = null;
        if (entity != null) {
            World.Mob mob = entity.asMob();
            if (mob != null) {
                mob.addCustomAiGoal(1, 2);
                float health = mob.getHealth();
                mob.setHealth(10.0f);
                getAiGoals().register(mob, 1, new DemoGoal());
            }
            Display.BlockDisplayEntity display = Display.BlockDisplayEntity.fromEntity(entity);
            if (display != null) {
                display.setBlockStateId(1);
            }
        }
    }

    static class DemoGoal implements AiGoal {
        @Override
        public boolean canStart(Server server, World.Entity entity) {
            return true;
        }
    }
}
