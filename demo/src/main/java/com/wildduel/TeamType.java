package com.wildduel;

import org.bukkit.ChatColor;

public enum TeamType {
    RED("레드", ChatColor.RED),
    BLUE("블루", ChatColor.BLUE),
    NONE("없음", ChatColor.WHITE),
    SPECTATOR("관전자", ChatColor.GRAY);

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
