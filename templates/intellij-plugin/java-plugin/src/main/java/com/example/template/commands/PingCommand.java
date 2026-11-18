package com.example.template.commands;

import java.util.List;
import pumpkin.Command;
import pumpkin.Server;
import pumpkin.TextComponent;

public final class PingCommand {
    public static Command command() {
        return new Command(List.of("ping"), "Responds with pong");
    }

    public static int handle(Command.CommandSender sender, Server server, Command.ConsumedArgs args) {
        sender.sendMessage(TextComponent.text("pong"));
        return 1;
    }
}
