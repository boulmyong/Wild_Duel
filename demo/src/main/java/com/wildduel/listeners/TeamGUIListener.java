package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameMode;
import com.wildduel.game.TeamManager;
import com.wildduel.gui.TeamGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class TeamGUIListener implements Listener {

    private final WildDuel plugin;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    private final TeamGUI teamGUI;

    public TeamGUIListener(WildDuel plugin, TeamManager teamManager, GameManager gameManager, TeamGUI teamGUI) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
        this.teamGUI = teamGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals(plugin.getMessage("gui.team.title"))) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        // Handle Game Mode Change
        if (meta.getPersistentDataContainer().has(TeamGUI.GAME_MODE_KEY, PersistentDataType.STRING)) {
            String gameModeName = meta.getPersistentDataContainer().get(TeamGUI.GAME_MODE_KEY, PersistentDataType.STRING);
            try {
                GameMode selectedMode = GameMode.valueOf(gameModeName);
                gameManager.setGameMode(selectedMode);
                player.sendMessage(plugin.getMessage("gui.team.success.gamemode-set", "%mode%", selectedMode.getDisplayName()));
                teamGUI.open(player); // Refresh the GUI
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessage("gui.team.error.invalid-gamemode"));
            }
            return;
        }

        // Handle Team Leave
        if (clickedItem.getType() == Material.BARRIER) {
            teamManager.leaveTeam(player);
            player.sendMessage(plugin.getMessage("gui.team.info.left-team"));
            teamGUI.open(player); // Refresh the GUI
            return;
        }

        // Handle Team Join
        if (meta.getPersistentDataContainer().has(TeamGUI.TEAM_NAME_KEY, PersistentDataType.STRING)) {
            String teamName = meta.getPersistentDataContainer().get(TeamGUI.TEAM_NAME_KEY, PersistentDataType.STRING);
            if (teamName != null) {
                teamManager.joinTeam(player, teamName);
                teamGUI.open(player); // Refresh the GUI
            }
        }
    }
}
