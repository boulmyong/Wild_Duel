package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class GameMechanicsListener implements Listener {

    private final GameManager gameManager;
    private final Map<Material, Material> smeltMap = new HashMap<>();

    public GameMechanicsListener(GameManager gameManager) {
        this.gameManager = gameManager;
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
                    WildDuel.getInstance().getLogger().warning("[AutoSmelt] Invalid material name in config.yml: " + key + " or " + section.getString(key));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameManager.isAutoSmeltEnabled() && (gameManager.getGameState() == GameState.FARMING || gameManager.getGameState() == GameState.BATTLE)) {
            Material blockType = event.getBlock().getType();
            if (smeltMap.containsKey(blockType)) {
                event.setDropItems(false);
                Material smeltedItem = smeltMap.get(blockType);
                event.getBlock().getWorld().dropItem(event.getBlock().getLocation().add(0.5, 0.5, 0.5), new ItemStack(smeltedItem, 1));
            }
        }
    }
    
    // BlockPlaceEvent, PlayerDropItemEvent 등 로비에서 제한하던 규칙들을 제거했으므로
    // 해당 리스너 코드는 더 이상 필요하지 않습니다.
}
