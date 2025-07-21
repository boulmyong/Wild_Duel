package com.wildduel.listeners;

import com.wildduel.game.TeamManager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class TeamGUIListener implements Listener {

    private final TeamManager teamManager;

    public TeamGUIListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals("§1팀 선택하기")) {
            return;
        }

        // 1. Immediately cancel the event.
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        if (clickedItem.getType() == Material.BARRIER) {
            teamManager.leaveTeam(player);
            player.sendMessage("§7팀에서 나왔습니다.");
            player.closeInventory();
            return;
        }

        if (clickedItem.getType().name().endsWith("_WOOL")) {
            String teamName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).replace(" 팀 참가", "");
            teamManager.joinTeam(player, teamName);
            player.closeInventory();
        }
    }
}
