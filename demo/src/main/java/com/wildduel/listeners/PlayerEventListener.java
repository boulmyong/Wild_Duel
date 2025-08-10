package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import com.wildduel.game.TeamManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final Map<Material, Material> smeltMap = new HashMap<>();

    public PlayerEventListener(GameManager gameManager, TeamManager teamManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        loadSmeltMap();
    }

    public void loadSmeltMap() {
        smeltMap.clear();
        ConfigurationSection section = WildDuel.getInstance().getConfig().getConfigurationSection("auto-smelt-mapping");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material from = Material.valueOf(key.toUpperCase());
                    Material to = Material.valueOf(section.getString(key, "").toUpperCase());
                    smeltMap.put(from, to);
                } catch (IllegalArgumentException e) {
                    WildDuel.getInstance().getLogger().warning(WildDuel.getInstance().getMessage("log.autosmelt.invalid-material", "%key%", key, "%value%", section.getString(key)));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Ensure player is not in a team when they join
        teamManager.leaveTeam(player);

        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.FARMING || gameState == GameState.BATTLE) {
            // Spectator for ongoing games
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(gameManager.getLobbyWorld().getSpawnLocation());
            player.sendMessage(WildDuel.getInstance().getMessage("info.game-in-progress-spectator"));
        } else { // LOBBY, COUNTDOWN, ENDED
            // Go to lobby
            gameManager.preparePlayerForLobby(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.BATTLE) {
            // Set spectator mode immediately upon death in battle
            player.setGameMode(GameMode.SPECTATOR);
            // Check for win condition after a delay to allow for death processing
            Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), gameManager::checkWinCondition, 20L);
        } else if (gameState == GameState.FARMING) {
            // In farming, they just respawn normally. Let the onPlayerRespawn handle the location.
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.BATTLE || gameState == GameState.ENDED) {
            // If the game is in battle or has ended, players should always respawn at the lobby.
            event.setRespawnLocation(gameManager.getLobbyWorld().getSpawnLocation());
            // A short delay to ensure they are in the lobby, then prepare them.
            Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), () -> {
                if (player.isOnline()) {
                    gameManager.preparePlayerForLobby(player);
                    player.setGameMode(GameMode.SPECTATOR); // Re-affirm spectator mode
                }
            }, 1L);
        } else if (gameState == GameState.FARMING) {
            // Respawn at the game world's spawn during the farming phase.
            event.setRespawnLocation(gameManager.getGameWorld().getSpawnLocation());
        } else {
            // For any other state (Lobby, Countdown), respawn and prepare in the lobby.
            event.setRespawnLocation(gameManager.getLobbyWorld().getSpawnLocation());
            Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), () -> {
                if (player.isOnline()) {
                    gameManager.preparePlayerForLobby(player);
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameManager.isAutoSmeltEnabled() && (gameManager.getGameState() == GameState.FARMING || gameManager.getGameState() == GameState.BATTLE)) {
            Material blockType = event.getBlock().getType();
            if (smeltMap.containsKey(blockType)) {
                event.setDropItems(false);
                Material smeltedItem = smeltMap.get(blockType);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(smeltedItem, 1));
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // No restrictions in lobby anymore
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Allow dropping items in lobby
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        gameManager.handlePlayerQuit(player);
        teamManager.handlePlayerQuit(player);
        WildDuel.getInstance().getTpaManager().handlePlayerQuit(player);
    }
}

