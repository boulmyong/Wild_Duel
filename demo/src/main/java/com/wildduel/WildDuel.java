package com.wildduel;

import org.bukkit.plugin.java.JavaPlugin;

public class WildDuel extends JavaPlugin {

    private static WildDuel instance;
    private GameManager gameManager;
    private TeamManager teamManager;
    private TeamAdminManager teamAdminManager;
    private TpaManager tpaManager;

    @Override
    public void onEnable() {
        instance = this;
        teamManager = new TeamManager();
        gameManager = new GameManager(teamManager);
        teamAdminManager = new TeamAdminManager();
        tpaManager = new TpaManager(teamManager);

        getCommand("wd").setExecutor(new WildDuelCommand(gameManager, teamManager, teamAdminManager, tpaManager));
        getCommand("wd").setTabCompleter(new WildDuelTabCompleter());
        getCommand("tpa").setExecutor(new TpaCommand(tpaManager));
        getCommand("tpacancel").setExecutor(new TpaCancelCommand(tpaManager));
        getCommand("tparesponse").setExecutor(new TpaResponseCommand(tpaManager));

        getServer().getPluginManager().registerEvents(new PlayerEventListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new TeamAdminGUIListener(teamAdminManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new TpaListener(tpaManager), this);
        getServer().getPluginManager().registerEvents(new AdminGUIListener(gameManager, teamManager, tpaManager, teamAdminManager), this);
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
