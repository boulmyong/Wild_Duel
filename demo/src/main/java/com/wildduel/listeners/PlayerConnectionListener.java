package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import com.wildduel.game.TeamManager;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final WildDuel plugin = WildDuel.getInstance();

    public PlayerConnectionListener(GameManager gameManager, TeamManager teamManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        teamManager.applyTeamVisualsOnJoin(player);
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.FARMING || gameState == GameState.BATTLE) {
            player.setGameMode(GameMode.SPECTATOR);
            World gameWorld = gameManager.getGameWorld();
            if (gameWorld != null) {
                player.teleport(gameWorld.getSpawnLocation());
            } else {
                player.teleport(gameManager.getLobbyWorld().getSpawnLocation());
            }
            player.sendMessage(plugin.getMessage("info.spectator-mode"));
        } else { 
            gameManager.preparePlayerForLobby(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        gameManager.handlePlayerQuit(player);
        teamManager.handlePlayerQuit(player);
        plugin.getTpaManager().handlePlayerQuit(player);
    }
}
