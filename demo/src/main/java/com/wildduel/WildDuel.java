package com.wildduel;

import org.bukkit.plugin.java.JavaPlugin;

public class WildDuel extends JavaPlugin {

    private static WildDuel instance;
    private GameManager gameManager;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        instance = this;
        gameManager = new GameManager(teamManager);
        teamManager = new TeamManager();
        getCommand("wd").setExecutor(new WildDuelCommand(gameManager, teamManager));
        getCommand("wd").setTabCompleter(new WildDuelTabCompleter());
        getServer().getPluginManager().registerEvents(new PlayerEventListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new TeamGUIListener(teamManager), this);
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
