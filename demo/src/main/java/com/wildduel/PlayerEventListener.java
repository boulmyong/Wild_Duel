package com.wildduel;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class PlayerEventListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager; // Add TeamManager
    private final Map<Material, Material> smeltMap = new HashMap<>();

    public PlayerEventListener(GameManager gameManager, TeamManager teamManager) { // Modify constructor
        this.gameManager = gameManager;
        this.teamManager = teamManager; // Initialize TeamManager
        // Initialize smelt map
        smeltMap.put(Material.IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        smeltMap.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        // For deepslate variants
        smeltMap.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        smeltMap.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        smeltMap.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        gameManager.handlePlayerDeath(event.getEntity());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Force set the main scoreboard and apply team visuals
        teamManager.applyTeamVisualsOnJoin(player);

        GameState gameState = gameManager.getGameState();
        if (gameState == GameState.PREPARING || gameState == GameState.FARMING) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0, false, false));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.PREPARING || gameState == GameState.FARMING) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0, false, false));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!gameManager.isAutoSmeltEnabled()) {
            return;
        }
        GameState gameState = gameManager.getGameState();
        if (gameState != GameState.FARMING && gameState != GameState.BATTLE) {
            return;
        }

        Material blockType = event.getBlock().getType();
        if (smeltMap.containsKey(blockType)) {
            event.setDropItems(false); // Prevent original drops
            Material smeltedItem = smeltMap.get(blockType);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(smeltedItem, 1));
        }
    }
}
