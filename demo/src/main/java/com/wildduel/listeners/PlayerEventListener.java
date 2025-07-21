package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import com.wildduel.game.TeamManager;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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

public class PlayerEventListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final Map<Material, Material> smeltMap = new HashMap<>();

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
        if (gameState == GameState.FARMING || gameState == GameState.BATTLE) {
            // Set to spectator on death during any active game phase
            player.setGameMode(GameMode.SPECTATOR);
            // Check for win condition after a delay
            org.bukkit.Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), gameManager::checkWinCondition, 20L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        GameState gameState = gameManager.getGameState();
        if (gameState == GameState.FARMING || gameState == GameState.BATTLE) {
            // Respawn at lobby as spectator
            event.setRespawnLocation(gameManager.getLobbyWorld().getSpawnLocation());
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
        if (gameManager.getGameState() == GameState.LOBBY) {
            event.setCancelled(true);
        }
    }
}