package com.example.template.listeners;

import pumpkin.Event;
import pumpkin.Player;
import pumpkin.TextComponent;

public final class JoinListener {
    public static void handle(Event.PlayerJoinEvent join) {
        Player player = join.getPlayer();
        // Test Comment
        player.sendMessage(TextComponent.text("Welcome to the Pumpkin Java template!"));
    }
}
