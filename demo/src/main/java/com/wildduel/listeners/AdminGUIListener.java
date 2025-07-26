package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.gui.AdminGUI;
import com.wildduel.gui.TeamAdminGUI;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.game.TeamAdminManager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class AdminGUIListener implements Listener {

    private final WildDuel plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TpaManager tpaManager;
    private final TeamAdminManager teamAdminManager;

    public AdminGUIListener(WildDuel plugin, GameManager gameManager, TeamManager teamManager, TpaManager tpaManager, TeamAdminManager teamAdminManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.tpaManager = tpaManager;
        this.teamAdminManager = teamAdminManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals("§4[관리자 패널] 와일드 듀얼")) {
            return;
        }

        event.setCancelled(true);

        Player admin = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        boolean refreshGui = true;

        switch (clickedItem.getType()) {
            // Core Actions
            case LIME_WOOL: // Start Game
                admin.performCommand("wd start");
                admin.closeInventory();
                refreshGui = false;
                break;
            case TNT: // Random Teams
                admin.performCommand("wd randomteam");
                admin.closeInventory();
                refreshGui = false;
                break;

            // Settings & Management
            case WRITABLE_BOOK: // Toggle Team Selection
                togglePlayerTeamSelection(admin);
                break;
            case FURNACE: // Toggle Auto Smelt
                toggleAutoSmelt(admin);
                break;
            case PLAYER_HEAD: // Manual Team Assign
                new TeamAdminGUI(teamAdminManager, teamManager).open(admin);
                refreshGui = false;
                break;
            case ENDER_PEARL: // Reset TPA Cooldowns
                tpaManager.refreshAllCooldowns();
                admin.sendMessage(plugin.getMessage("command.tparefresh.success-all"));
                admin.closeInventory();
                refreshGui = false;
                break;

            // Initial Farming Time
            case REDSTONE_BLOCK: // -60s
                updatePrepTime(admin, -60);
                break;
            case RED_STAINED_GLASS_PANE: // -10s
                updatePrepTime(admin, -10);
                break;
            case LIME_STAINED_GLASS_PANE: // +10s
                updatePrepTime(admin, 10);
                break;
            case EMERALD_BLOCK: // +60s
                updatePrepTime(admin, 60);
                break;

            case DIAMOND_BLOCK: // Live Time Adjust
                if (event.isLeftClick()) {
                    adjustRemainingTime(admin, 60);
                } else if (event.isRightClick()) {
                    adjustRemainingTime(admin, -60);
                }
                break;

            default:
                refreshGui = false;
                break;
        }

        if (refreshGui) {
            new AdminGUI(gameManager, teamManager, tpaManager, teamAdminManager).open(admin);
        }
    }

    private void toggleAutoSmelt(Player admin) {
        if (gameManager.getGameState() != GameState.LOBBY) {
            admin.sendMessage(plugin.getMessage("error.must-be-lobby"));
            return;
        }
        gameManager.setAutoSmelt(!gameManager.isAutoSmeltEnabled());
    }

    private void togglePlayerTeamSelection(Player admin) {
        if (gameManager.getGameState() != GameState.LOBBY) {
            admin.sendMessage(plugin.getMessage("error.must-be-lobby"));
            return;
        }
        gameManager.setPlayerTeamSelectionEnabled(!gameManager.isPlayerTeamSelectionEnabled());
    }

    private void updatePrepTime(Player admin, int change) {
        if (gameManager.getGameState() != GameState.LOBBY) {
            admin.sendMessage(plugin.getMessage("error.must-be-lobby"));
            return;
        }
        int newTime = gameManager.getInitialPrepTimeSeconds() + change;
        if (newTime < 0) newTime = 0;
        gameManager.setPrepTime(newTime);
    }

    private void adjustRemainingTime(Player admin, int change) {
        if (gameManager.getGameState() != GameState.FARMING) {
            admin.sendMessage(plugin.getMessage("error.must-be-farming"));
            return;
        }
        gameManager.addTime(change);
        admin.sendMessage(plugin.getMessage("command.addtime.success", "%seconds%", String.valueOf(change)));
    }
}
