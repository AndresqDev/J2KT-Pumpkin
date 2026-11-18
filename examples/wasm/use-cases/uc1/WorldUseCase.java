package example.uc1;

import pumpkin.Common;
import pumpkin.Display;
import pumpkin.EntityTypes;
import pumpkin.ItemStack;
import pumpkin.Logging;
import pumpkin.PluginContext;
import pumpkin.Position;
import pumpkin.Scoreboard;
import pumpkin.Server;
import pumpkin.TextComponent;
import pumpkin.World;

/**
 * Use case 2: world state, scoreboard/teams, block registry and entities.
 * No player is required; it spawns and inspects entities owned by the world.
 */
public final class WorldUseCase {
    public static void install(PluginContext context) {
        Server server = context.getServer();
        World world = server.getWorld("world");
        if (world == null) {
            return;
        }

        long time = world.getTime();
        world.setTime(time + 6000L);
        world.setStorm(false);
        world.setThundering(false);
        Logging.log(Logging.Level.INFO, "world " + world.getName() + " time=" + time);

        Scoreboard board = world.getScoreboard();
        board.addObjective("j2kt:visits", TextComponent.text("Visits"),
                Scoreboard.RenderType.INTEGER, null);
        board.setDisplaySlot(Scoreboard.DisplaySlot.SIDEBAR, "j2kt:visits");
        board.updateScore("world", "j2kt:visits", 1, null);
        board.createTeam("j2kt:world", new Scoreboard.TeamSettings(
                TextComponent.text("World"),
                false,
                true,
                Scoreboard.NametagVisibility.ALWAYS,
                Scoreboard.CollisionRule.ALWAYS,
                Common.NamedColor.AQUA,
                TextComponent.text("[W] "),
                TextComponent.text("")));

        World.Block stone = world.getBlockByName("minecraft:stone");
        if (stone != null) {
            Logging.log(Logging.Level.INFO, "stone id=" + stone.id + " hardness=" + stone.hardness);
            if (!world.getStatesForBlock(stone).isEmpty()) {
                World.BlockState state = world.getStatesForBlock(stone).get(0);
                Logging.log(Logging.Level.DEBUG, "state " + state.id + " of " + state.blockName);
            }
        }

        World.Entity zombieEntity = world.spawnEntity(
                EntityTypes.EntityType.ZOMBIE, new Position(8.0, 64.0, 8.0));
        World.Mob zombie = zombieEntity.asMob();
        if (zombie != null) {
            zombie.setHealth(10.0f);
            zombie.navigateToPos(new Position(12.0, 64.0, 12.0), 1.0);
            zombie.addCustomAiGoal(1, 42);
        }

        World.Entity displayEntity = world.spawnEntity(
                EntityTypes.EntityType.BLOCK_DISPLAY, new Position(0.0, 65.0, 0.0));
        Display.BlockDisplayEntity display = Display.BlockDisplayEntity.fromEntity(displayEntity);
        if (display != null && stone != null) {
            display.setBlockStateId(stone.defaultStateId);
            display.getDisplay().setBillboard(Display.BillboardMode.CENTER);
            display.getDisplay().setViewRange(2.0f);
            display.getDisplay().setShadowRadius(0.5f);
        }

        World.Entity itemEntity = world.spawnEntity(
                EntityTypes.EntityType.ITEM_DISPLAY, new Position(1.0, 65.0, 1.0));
        Display.ItemDisplayEntity itemDisplay = Display.ItemDisplayEntity.fromEntity(itemEntity);
        if (itemDisplay != null) {
            itemDisplay.setItem(ItemStack.of("minecraft:stone", 1));
            itemDisplay.setItemDisplayMode(Display.ItemDisplayMode.GROUND);
        }

        World.Entity textEntity = world.spawnEntity(
                EntityTypes.EntityType.TEXT_DISPLAY, new Position(0.0, 66.0, 0.0));
        Display.TextDisplayEntity text = Display.TextDisplayEntity.fromEntity(textEntity);
        if (text != null) {
            text.setText(TextComponent.text("j2kt world use case"));
            text.setLineWidth(200);
            text.setAlignment(Display.TextAlignment.CENTER);
        }
    }
}
