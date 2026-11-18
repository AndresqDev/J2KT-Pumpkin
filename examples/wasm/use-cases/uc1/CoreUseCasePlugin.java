package example.uc1;

import java.util.List;
import pumpkin.Command;
import pumpkin.Event;
import pumpkin.EventPriority;
import pumpkin.EventSubscription;
import pumpkin.EventType;
import pumpkin.Logging;
import pumpkin.Permission;
import pumpkin.Player;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import pumpkin.ScheduledTask;
import pumpkin.Server;
import pumpkin.TextComponent;
import pumpkin.EntityTypes;
import pumpkin.Position;
import pumpkin.World;

/**
 * Use case 1: plugin lifecycle, permissions, commands, suggestions, events,
 * delayed/repeating tasks and inter-plugin messaging.
 *
 * Everything here is safe to load on a server with no players online; player
 * callbacks only run when the host dispatches them.
 */
public class CoreUseCasePlugin extends PumpkinPlugin {
    private EventSubscription joinSubscription;
    private ScheduledTask delayed;

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "j2kt-core-use-case",
                "1.0.0",
                List.of("j2kt"),
                "Core API coverage without requiring a live player",
                List.of(),
                List.of("j2kt-core-use-case:core.use", "j2kt-core-use-case:core.admin"));
    }

    @Override
    public void onLoad(PluginContext context) {
        Logging.log(Logging.Level.INFO, "core use case loading in " + context.getDataFolder());

        context.registerPermission(new Permission(
                "j2kt-core-use-case:core.use",
                "Allows using the core command",
                Permission.PermissionDefault.ALLOW,
                List.of(new Permission.PermissionChild("j2kt-core-use-case:core.admin", true))));

        Server server = context.getServer();
        Logging.log(Logging.Level.DEBUG, "server players: " + server.getPlayerCount());

        Command core = new Command(List.of("j2ktcore"), "Runs the core use case");
        getCommands().register(context, core, "j2kt-core-use-case:core.use", (sender, srv, args) -> {
            sender.sendMessage(TextComponent.text("core command executed by " + sender.getName()));
            srv.broadcastMessage(TextComponent.text("core command completed"));
            return 1;
        });

        Command.CommandNode suggestions = Command.CommandNode.literal("j2ktcore");
        getCommands().onSuggestions(suggestions, (sender, srv, request) ->
                new Command.CommandSuggestions(0, 0,
                        List.of(new Command.CommandSuggestion("j2ktcore",
                                TextComponent.text("Run the core use case")))));

        delayed = getTasks().afterTicks(20,
                srv -> Logging.log(Logging.Level.INFO, "delayed task on " + srv.getTps() + " tps"));
        getTasks().everyTicks(20, 100, srv -> {
            if (srv.getMspt() > 40.0) {
                Logging.log(Logging.Level.WARN, "server is running behind");
            }
        });

        joinSubscription = getEvents().listen(
                context,
                EventType.PLAYER_JOIN_EVENT,
                EventPriority.NORMAL,
                true,
                (srv, event) -> {
                    if (event instanceof Event.PlayerJoinEvent join) {
                        Player player = join.getPlayer();
                        join.getValue().setJoinMessage(TextComponent.text("Welcome " + player.getName()));
                        player.sendMessage(TextComponent.text("Use /j2ktcore to test the plugin"));
                    }
                    return event;
                });

        getEvents().listen(context, EventType.PLAYER_DEATH_EVENT, EventPriority.HIGH, false,
                (srv, event) -> {
                    if (event instanceof Event.PlayerDeathEvent death) {
                        death.getValue().setDeathMessage(TextComponent.text("A player died; core use case observed it"));
                    }
                    return event;
                });

        getEvents().listen(context, EventType.PLAYER_CHAT_EVENT, EventPriority.LOW, false,
                (srv, event) -> {
                    if (event instanceof Event.PlayerChatEvent chat) {
                        String message = chat.getMessage();
                        if (message != null && message.startsWith("!core")) {
                            chat.setCancelled(true);
                            Player player = chat.getPlayer();
                            player.sendMessage(TextComponent.text("chat command handled"));
                        }
                    }
                    return event;
                });

        getIpc().sendIpcMessage("j2kt-other-plugin", new byte[]{1, 2, 3});
        getIpc().onMessage((sender, message) -> {
            Logging.log(Logging.Level.INFO, "ipc message from " + sender);
            return message;
        });

        WorldUseCase.install(context);
        InventoryUseCase.install(context);

        World mobWorld = server.getWorld("world");
        if (mobWorld != null) {
            World.Entity mobEntity = mobWorld.spawnEntity(
                    EntityTypes.EntityType.ZOMBIE, new Position(4.0, 64.0, 4.0));
            World.Mob mob = mobEntity.asMob();
            if (mob != null) {
                getAiGoals().register(mob, 1, new DemoAiGoal());
            }
        }

        ContentUseCase.install(context);
        ModerationUseCase.install(this, context);

        ScheduledTask cancelMe = getTasks().afterTicks(1, srv -> {
        });
        cancelMe.cancel();

        EventSubscription temporary = getEvents().listen(
                context, EventType.PLAYER_LEAVE_EVENT, EventPriority.LOWEST, false,
                (srv, event) -> event);
        temporary.unregister();

        // Keep references so the lifecycle of returned handles is exercised.
    }

    @Override
    public void onUnload(PluginContext context) {
        Logging.log(Logging.Level.INFO, "core use case unloading");
    }
}
