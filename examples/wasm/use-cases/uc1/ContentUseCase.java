package example.uc1;

import java.util.List;
import java.util.Map;
import pumpkin.Datapack;
import pumpkin.Enchantments;
import pumpkin.ItemStack;
import pumpkin.Permission;
import pumpkin.PluginContext;
import pumpkin.Recipe;
import pumpkin.Server;
import pumpkin.TextComponent;

/**
 * Use case 4: data-driven content registration: shaped/shapeless/cooking
 * recipes, datapack inspection and control, permissions and custom
 * enchantments. None of these calls requires an online player.
 */
public final class ContentUseCase {
    public static void install(PluginContext context) {
        Server server = context.getServer();

        context.registerPermission(new Permission(
                "j2kt-core-use-case:content",
                "Allows content registration",
                Permission.PermissionDefault.OP,
                List.of()));

        Recipe.RecipeManager recipes = server.getRecipeManager();
        recipes.registerShaped("j2kt:stone_block", new Recipe.ShapedRecipe(
                List.of("###", "###", "###"),
                Map.of("#", Recipe.item("minecraft:stone")),
                ItemStack.of("minecraft:stone", 1),
                "j2kt",
                Recipe.RecipeCategory.BUILDING,
                true));
        recipes.registerShapeless("j2kt:shapeless", new Recipe.ShapelessRecipe(
                List.of(Recipe.item("minecraft:diamond"), Recipe.tag("minecraft:planks")),
                ItemStack.of("minecraft:diamond_block", 1),
                "j2kt",
                Recipe.RecipeCategory.MISC));
        recipes.registerCooking("j2kt:cooked", Recipe.CookingType.SMELTING,
                new Recipe.CookingRecipe(
                        Recipe.item("minecraft:iron_ore"),
                        ItemStack.of("minecraft:iron_ingot", 1),
                        0.7f,
                        200,
                        "j2kt",
                        Recipe.RecipeCategory.MISC));

        Datapack.DatapackManager datapacks = server.getDatapackManager();
        for (Datapack.DatapackInfo info : datapacks.listAllPacks()) {
            if (info.isEnabled) {
                int recipeCount = info.recipeCount;
                String name = info.name;
                if (recipeCount > 0 && name != null) {
                    // Read-only inspection; no filesystem access required.
                }
            }
        }
        datapacks.enablePack("j2kt-data", Datapack.before("vanilla"));
        datapacks.isEnabled("j2kt-data");
        datapacks.disablePack("j2kt-data");
        datapacks.reload();
        datapacks.executeFunction("j2kt:reload");

        Enchantments.CustomEnchantment enchantment = new Enchantments.CustomEnchantment(
                "j2kt:lifesteal",
                TextComponent.text("Lifesteal"),
                3,
                1,
                "#minecraft:enchantable/weapon",
                5,
                List.of(Enchantments.AttributeModifierSlot.MAIN_HAND),
                List.of());
        server.getEnchantmentManager().registerEnchantment(enchantment);
    }
}
