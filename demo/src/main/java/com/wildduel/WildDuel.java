package com.wildduel;

import org.bukkit.plugin.java.JavaPlugin;

public class WildDuel extends JavaPlugin {

    private static WildDuel instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        gameManager = new GameManager();
        getCommand("wd").setExecutor(new WildDuelCommand(gameManager));
        getServer().getPluginManager().registerEvents(new PlayerEventListener(gameManager), this);
        getLogger().info("WildDuel plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("WildDuel plugin disabled!");
    }

    public static WildDuel getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
