package com.wildduel;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TeamGUIListener implements Listener {

    private final TeamManager teamManager;

    public TeamGUIListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("팀 선택")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (clickedItem.getType() == Material.BARRIER) {
            teamManager.leaveTeam(player);
            player.sendMessage("You have left the team.");
            player.closeInventory();
            return;
        }

        if (clickedItem.getType().name().endsWith("_WOOL")) {
            String teamName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).replace(" Team", "");
            teamManager.joinTeam(player, teamName);
            player.sendMessage("You have joined the " + teamName + " team.");
            player.closeInventory();
        }
    }
}