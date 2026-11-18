package com.example.template;

import java.util.List;

import com.example.template.commands.HiCommand;
import com.example.template.commands.PingCommand;
import com.example.template.listeners.JoinListener;
import pumpkin.Event;
import pumpkin.EventPriority;
import pumpkin.EventType;
import pumpkin.Logging;
import pumpkin.Permission;
import pumpkin.PluginContext;
import pumpkin.PluginMetadata;
import pumpkin.PumpkinPlugin;
import pumpkin.TextComponent;

public class TemplatePlugin extends PumpkinPlugin {
    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "pumpkin-java-template",
                "1.0.0",
                List.of("Example"),
                "A Java to Kotlin to Wasm Pumpkin plugin template",
                List.of(),
                List.of("pumpkin-java-template:template.use"));
    }

    @Override
    public void onLoad(PluginContext context) {
        Logging.log(Logging.Level.INFO, "Loading Pumpkin Java template");

        context.registerPermission(new Permission(
                "pumpkin-java-template:template.use",
                "Allows using the template commands",
                Permission.PermissionDefault.ALLOW,
                List.of()));

        getEvents().listen(
                context,
                EventType.PLAYER_JOIN_EVENT,
                EventPriority.NORMAL,
                true,
                (server, event) -> {
                    if (event instanceof Event.PlayerJoinEvent join) {
                        JoinListener.handle(join);
                    }
                    return event;
                });

        getCommands().register(
                context,
                PingCommand.command(),
                "pumpkin-java-template:template.use",
                PingCommand::handle);

        getCommands().register(
                context,
                HiCommand.command(),
                "pumpkin-java-template:template.use",
                HiCommand::handle);
    }

    @Override
    public void onUnload(PluginContext context) {
        Logging.log(Logging.Level.INFO, "Unloading Pumpkin Java template");
    }
}
