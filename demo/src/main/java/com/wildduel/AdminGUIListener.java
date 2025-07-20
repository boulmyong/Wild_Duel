package com.wildduel;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AdminGUIListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TpaManager tpaManager;
    private final TeamAdminManager teamAdminManager;

    public AdminGUIListener(GameManager gameManager, TeamManager teamManager, TpaManager tpaManager, TeamAdminManager teamAdminManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.tpaManager = tpaManager;
        this.teamAdminManager = teamAdminManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§4Wild Duel - Admin Panel")) {
            return;
        }

        event.setCancelled(true);
        Player admin = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Material type = clickedItem.getType();

        switch (type) {
            case BEACON:
                admin.performCommand("wd set");
                break;
            case DIAMOND_SWORD:
                admin.performCommand("wd sp");
                break;
            case GREEN_WOOL:
                admin.performCommand("wd start");
                admin.closeInventory();
                break;
            case PLAYER_HEAD:
                new TeamAdminGUI(teamAdminManager, teamManager).open(admin);
                break;
            case TNT:
                admin.performCommand("wd randomteam");
                admin.closeInventory();
                break;
            case FURNACE:
                gameManager.setAutoSmelt(!gameManager.isAutoSmeltEnabled());
                break;
            case EMERALD_BLOCK:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() + 60);
                break;
            case LIME_STAINED_GLASS_PANE:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() + 10);
                break;
            case RED_STAINED_GLASS_PANE:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() - 10);
                break;
            case REDSTONE_BLOCK:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() - 60);
                break;
            case ENDER_PEARL:
                tpaManager.refreshAllCooldowns();
                admin.sendMessage("§aAll TPA cooldowns have been refreshed.");
                break;
            default:
                return; // Unhandled item
        }

        // Refresh the GUI to show updated state
        new AdminGUI(gameManager, teamManager, tpaManager, teamAdminManager).open(admin);
    }
}
