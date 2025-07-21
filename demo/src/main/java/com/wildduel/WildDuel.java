package com.wildduel;

import com.wildduel.commands.*;
import com.wildduel.game.GameManager;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.listeners.AdminGUIListener;
import com.wildduel.listeners.PlayerEventListener;
import com.wildduel.listeners.TeamAdminGUIListener;
import com.wildduel.listeners.TeamGUIListener;
import com.wildduel.listeners.TpaListener;
import com.wildduel.util.EmptyWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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

        // Create lobby world if it doesn't exist
        createLobbyWorld();

        teamManager = new TeamManager();
        gameManager = new GameManager(teamManager);
        teamAdminManager = new TeamAdminManager();
        tpaManager = new TpaManager(teamManager);

        // Initialize managers with world context
        gameManager.initializeWorlds();

        // Ensure teams are set up on plugin enable/reload
        teamManager.initializeTeams();

        getCommand("wd").setExecutor(new WildDuelCommand(gameManager, teamManager, teamAdminManager, tpaManager));
        getCommand("wd").setTabCompleter(new WildDuelTabCompleter());
        getCommand("tpa").setExecutor(new TpaCommand(tpaManager));
        getCommand("tpacancel").setExecutor(new TpaCancelCommand(tpaManager));
        getCommand("tparesponse").setExecutor(new TpaResponseCommand(tpaManager));
        getCommand("팀선택").setExecutor(new TeamSelectCommand(gameManager, teamManager));

        getServer().getPluginManager().registerEvents(new PlayerEventListener(gameManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new TeamAdminGUIListener(teamAdminManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new TpaListener(tpaManager), this);
        getServer().getPluginManager().registerEvents(new AdminGUIListener(gameManager, teamManager, tpaManager, teamAdminManager), this);
        getServer().getPluginManager().registerEvents(new TeamGUIListener(teamManager), this);
        getLogger().info("WildDuel plugin enabled!");
    }

    private void createLobbyWorld() {
        World lobbyWorld = Bukkit.getWorld("wildduel_world");
        if (lobbyWorld == null) {
            getLogger().info("'wildduel_world' not found, creating it...");
            WorldCreator wc = new WorldCreator("wildduel_world");
            wc.generator(new EmptyWorldGenerator());
            lobbyWorld = wc.createWorld();
            if (lobbyWorld != null) {
                lobbyWorld.setSpawnLocation(0, 1, 0);
                getLogger().info("'wildduel_world' created successfully.");
            }
        }
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
