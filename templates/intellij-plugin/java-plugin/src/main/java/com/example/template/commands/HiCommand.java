package com.example.template.commands;

import java.util.List;
import pumpkin.Command;
import pumpkin.Common;
import pumpkin.Server;
import pumpkin.TextComponent;

public final class HiCommand {
    public static Command command() {
        return new Command(List.of("hi"), "Prints the Pumpkin pipeline greeting");
    }

    public static int handle(Command.CommandSender sender, Server server, Command.ConsumedArgs args) {
        TextComponent message = TextComponent.text("Hello, World, from Java → Kotlin → Wasm!");
        message.gradientNamed(List.of(
                Common.NamedColor.GOLD,
                Common.NamedColor.YELLOW,
                Common.NamedColor.GOLD));
        message.bold(true);
        sender.sendMessage(message);
        return 1;
    }
}
