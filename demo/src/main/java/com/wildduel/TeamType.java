package com.wildduel;

import org.bukkit.ChatColor;

public enum TeamType {
    RED("Red", ChatColor.RED),
    BLUE("Blue", ChatColor.BLUE),
    NONE("None", ChatColor.WHITE),
    SPECTATOR("Spectator", ChatColor.GRAY);

    private final String name;
    private final ChatColor color;

    TeamType(String name, ChatColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }
}
