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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final Map<Material, Material> smeltMap = new HashMap<>();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public PlayerEventListener(GameManager gameManager, TeamManager teamManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        // Initialize smelt map
        smeltMap.put(Material.IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        smeltMap.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        smeltMap.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        teamManager.applyTeamVisualsOnJoin(player);
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.FARMING || gameState == GameState.BATTLE) {
            // Spectator for ongoing games
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(gameManager.getLobbyWorld().getSpawnLocation());
            player.sendMessage("§e게임이 이미 진행 중입니다. 관전 모드로 전환됩니다.");
        } else { // LOBBY, COUNTDOWN, ENDED
            // Go to lobby
            gameManager.preparePlayerForLobby(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameState gameState = gameManager.getGameState();
        Bukkit.getLogger().info("[DEBUG] PlayerDeathEvent for " + player.getName() + " in state " + gameState);

        if (gameState == GameState.BATTLE) {
            Location deathLoc = player.getLocation();
            deathLocations.put(player.getUniqueId(), deathLoc);
            Bukkit.getLogger().info("[DEBUG] Stored death location for " + player.getName() + ": " + deathLoc.toString());
            // Check for win condition after a delay
            org.bukkit.Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), gameManager::checkWinCondition, 20L);
        } else if (gameState == GameState.FARMING) {
            // In farming, they just respawn normally. The win condition check might be relevant.
            org.bukkit.Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), gameManager::checkWinCondition, 20L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameState gameState = gameManager.getGameState();
        Bukkit.getLogger().info("[DEBUG] PlayerRespawnEvent for " + player.getName() + " in state " + gameState);

        if (gameState == GameState.BATTLE || gameState == GameState.ENDED) {
            Location deathLoc = deathLocations.remove(player.getUniqueId());
            if (deathLoc != null) {
                Bukkit.getLogger().info("[DEBUG] Found death location for " + player.getName() + ". Setting respawn to: " + deathLoc.toString());
                event.setRespawnLocation(deathLoc);
            } else {
                // If no death location is found (e.g. player died before this fix, or joined mid-game)
                // teleport them to the lobby world instead of the main world spawn.
                Location lobbySpawn = gameManager.getLobbyWorld().getSpawnLocation();
                Bukkit.getLogger().warning("[DEBUG] Could not find death location for " + player.getName() + ". Using lobby fallback: " + lobbySpawn.toString());
                event.setRespawnLocation(lobbySpawn);
            }
            // Set to spectator AFTER respawning
            org.bukkit.Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), () -> {
                if(player.isOnline()) {
                    Bukkit.getLogger().info("[DEBUG] Setting gamemode to SPECTATOR for " + player.getName());
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }, 1L);

        } else if (gameState == GameState.FARMING) {
            Location respawnLoc = gameManager.getGameWorld().getSpawnLocation();
            Bukkit.getLogger().info("[DEBUG] Player " + player.getName() + " respawning in FARMING state to: " + respawnLoc.toString());
            event.setRespawnLocation(respawnLoc);
        } else if (gameState == GameState.LOBBY || gameState == GameState.COUNTDOWN) {
            Location lobbySpawn = gameManager.getLobbyWorld().getSpawnLocation();
            event.setRespawnLocation(lobbySpawn);
            // Teleport to lobby and prepare them again after respawn
            org.bukkit.Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), () -> {
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

