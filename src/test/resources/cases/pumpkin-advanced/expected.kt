package example

import plugin.AiGoal
import plugin.PluginContext
import plugin.PluginMetadata
import plugin.PumpkinCompatPlugin
import pumpkin.*
import pumpkin.Common
import pumpkin.Datapack
import pumpkin.Display
import pumpkin.Enchantments
import pumpkin.Forms
import pumpkin.ItemStack.ItemStack
import pumpkin.JavaDialogs
import pumpkin.Player.Player
import pumpkin.Recipe
import pumpkin.Scoreboard
import pumpkin.Server.Server
import pumpkin.Text.TextComponent
import pumpkin.World.Entity
import pumpkin.World.World

open class AdvancedPlugin : PumpkinCompatPlugin() {
    override fun metadata(): PluginMetadata {
        return PluginMetadata("advanced", "1", listOf(), "", listOf(), listOf())
    }

    override fun onLoad(context: PluginContext): Result<Unit> = runCatching {
        val server: Server = context.getServer()
        val world: World? = server.getWorldByName("world")
        if (world == null) {
            return@runCatching Unit
        }
        val board: Scoreboard.Scoreboard = world.getScoreboard()
        board.addObjective("kills", TextComponent.text("Kills"), Scoreboard.RenderType.INTEGER, null)
        board.updateScore("Steve", "kills", 3, null)
        board.setDisplaySlot(Scoreboard.DisplaySlot.SIDEBAR, "kills")
        val settings: Scoreboard.TeamSettings = Scoreboard.TeamSettings(TextComponent.text("Red"), false, false, Scoreboard.NametagVisibility.ALWAYS, Scoreboard.CollisionRule.ALWAYS, Common.NamedColor.RED, TextComponent.text(""), TextComponent.text(""))
        board.createTeam("red", settings)
        val player: Player? = server.getPlayerByName("Steve")
        val form: Forms.Form = Forms.Form.Simple(Forms.SimpleForm(TextComponent.text("Title"), TextComponent.text("Body"), listOf(Forms.SimpleFormButton(TextComponent.text("OK"), null), Forms.SimpleFormButton(TextComponent.text("Cancel"), Forms.FormImage(Forms.ImageType.PATH, "textures/ui/cancel")))))
        if (player != null) {
            val bedrock: pumpkin.Player.BedrockPlayer? = player.asBedrock()
            if (bedrock != null) {
                bedrock.openForm(form)
            }
        }
        val custom: Forms.Form = Forms.Form.Custom(Forms.CustomForm(TextComponent.text("Custom"), listOf(Forms.CustomFormElement.Label(TextComponent.text("Choose")), Forms.CustomFormElement.Toggle(TextComponent.text("Enabled") to true), Forms.CustomFormElement.Input(Triple(TextComponent.text("Name"), "Steve", "")))))
        val recipes: Recipe.RecipeManager = server.getRecipeManager()
        recipes.registerShaped("demo:block", Recipe.ShapedRecipe(listOf("###", "###", "###"), mapOf("#" to Recipe.Ingredient.Item("minecraft:stone")).toList(), ItemStack("minecraft:stone", (1).toUByte()), null, Recipe.RecipeCategory.BUILDING, true))
        recipes.registerShapeless("demo:shapeless", Recipe.ShapelessRecipe(listOf(Recipe.Ingredient.Item("minecraft:stone"), Recipe.Ingredient.Tag("minecraft:planks")), ItemStack("minecraft:stone", (2).toUByte()), null, Recipe.RecipeCategory.MISC))
        recipes.registerCooking("demo:ingot", Recipe.CookingType.SMELTING, Recipe.CookingRecipe(Recipe.Ingredient.Item("minecraft:iron_ore"), ItemStack("minecraft:iron_ingot", (1).toUByte()), 0.7f, (200).toUInt(), null, null))
        val packs: Datapack.DatapackManager = server.getDatapackManager()
        packs.enablePack("demo", Datapack.EnablePosition.Before("vanilla"))
        val info: Datapack.DatapackInfo? = packs.getPack("demo")
        if (info != null) {
            val recipesInPack: Int? = info.recipeCount.toInt()
        }
        val enchantment: Enchantments.CustomEnchantment = Enchantments.CustomEnchantment("demo:lifesteal", TextComponent.text("Lifesteal"), (3).toUInt(), (1).toUInt(), "#minecraft:enchantable/weapon", (5).toUInt(), listOf(Enchantments.AttributeModifierSlot.MAIN_HAND), listOf())
        server.getEnchantmentManager().registerEnchantment(enchantment)
        val maxLevel: Int = enchantment.maxLevel.toInt()
        val javaPlayer: pumpkin.Player.JavaPlayer? = if (player != null) player.asJava() else null
        val dialog: JavaDialogs.Dialog = JavaDialogs.Dialog(TextComponent.text("Dialog"), JavaDialogs.DialogType.NOTICE, listOf(JavaDialogs.DialogBody.PlainMessage(TextComponent.text("Hello"))), listOf(JavaDialogs.DialogInput.Bool(JavaDialogs.DialogInputBool(TextComponent.text("Enabled"), true)), JavaDialogs.DialogInput.Text(JavaDialogs.DialogInputText(TextComponent.text("Name"), TextComponent.text("Steve"), ""))), listOf(JavaDialogs.ActionButton(TextComponent.text("OK"), null, null, JavaDialogs.Action.OpenUrl("https://example.com"))), listOf(JavaDialogs.Link(JavaDialogs.LinkLabel.BuiltIn(JavaDialogs.LinkType.WEBSITE), "https://example.com")), null, true, null)
        val optionInput: JavaDialogs.DialogInput = JavaDialogs.DialogInput.SingleOption(JavaDialogs.DialogInputSingleOption(TextComponent.text("Option"), listOf("A", "B").map { TextComponent.text(it) }, (1).toUInt()))
        val numberInput: JavaDialogs.DialogInput = JavaDialogs.DialogInput.NumberRange(JavaDialogs.DialogInputNumberRange(TextComponent.text("Amount"), 1.0f, 64.0f, 1.0f, 1.0f, null))
        val customAction: JavaDialogs.Action = JavaDialogs.Action.CustomClick(JavaDialogs.CustomClickAction("demo:action", null))
        val customButton: JavaDialogs.ActionButton = JavaDialogs.ActionButton(TextComponent.text("Custom"), null, null, customAction)
        val customLink: JavaDialogs.Link = JavaDialogs.Link(JavaDialogs.LinkLabel.Custom(TextComponent.text("Site")), "https://example.com")
        val itemBody: JavaDialogs.DialogBody = JavaDialogs.DialogBody.Item(ItemStack("minecraft:stone", (1).toUByte()))
        if (javaPlayer != null) {
            javaPlayer.showDialog(dialog)
        }
        val block: pumpkin.World.Block? = pumpkin.World.getBlockByName("minecraft:stone")
        if (block != null) {
            val state: Int? = block.defaultStateId.toInt()
            val mapColor: Int? = block.mapColor.toInt()
        }
        val entity: Entity? = null
        if (entity != null) {
            val mob: pumpkin.World.Mob? = entity.asMob()
            if (mob != null) {
                mob.addCustomAiGoal((1).toUByte(), (2).toUInt())
                val health: Float = mob.asLiving().getHealth()
                mob.asLiving().setHealth(10.0f)
                aiGoals.register(mob, 1, DemoGoal())
            }
            val display: Display.BlockDisplayEntity? = Display.BlockDisplayEntity.fromEntity(entity)
            if (display != null) {
                display.setBlockStateId((1).toUShort())
            }
        }
        return@runCatching Unit
    }

    internal open class DemoGoal : AiGoal {
        override fun canStart(server: Server, entity: Entity): Boolean {
            return true
        }

    }

}
