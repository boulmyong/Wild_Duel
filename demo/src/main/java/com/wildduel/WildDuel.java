package com.wildduel;

import org.bukkit.plugin.java.JavaPlugin;

public class WildDuel extends JavaPlugin {

    private static WildDuel instance;
    private GameManager gameManager;
    private TeamManager teamManager;
    private TeamAdminManager teamAdminManager;

    @Override
    public void onEnable() {
        instance = this;
        teamManager = new TeamManager();
        gameManager = new GameManager(teamManager);
        teamAdminManager = new TeamAdminManager();

        getCommand("wd").setExecutor(new WildDuelCommand(gameManager, teamManager, teamAdminManager));
        getCommand("wd").setTabCompleter(new WildDuelTabCompleter());

        getServer().getPluginManager().registerEvents(new PlayerEventListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new TeamAdminGUIListener(teamAdminManager, teamManager), this);
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
