package com.wildduel.gui;

import com.wildduel.game.TeamManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TeamGUI {

    private final TeamManager teamManager;

    public TeamGUI(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "§1팀 선택하기");

        for (TeamManager.TeamData team : teamManager.getTeams()) {
            ItemStack teamItem = new ItemStack(getWoolMaterial(team.getColor()), 1);
            ItemMeta meta = teamItem.getItemMeta();
            meta.setDisplayName(team.getColor() + team.getName() + " 팀 참가");
            List<String> lore = new ArrayList<>();
            lore.add("§7클릭하여 이 팀에 합류합니다.");
            lore.add(" ");
            lore.add("§f현재 팀원:");
            for (Player member : team.getPlayers()) {
                lore.add("§7- " + member.getName());
            }
            meta.setLore(lore);
            teamItem.setItemMeta(meta);
            gui.addItem(teamItem);
        }

        ItemStack leaveItem = new ItemStack(Material.BARRIER);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        leaveMeta.setDisplayName("§c팀 나가기");
        leaveItem.setItemMeta(leaveMeta);
        gui.setItem(8, leaveItem);

        player.openInventory(gui);
    }

    private Material getWoolMaterial(ChatColor color) {
        switch (color) {
            case RED:
                return Material.RED_WOOL;
            case BLUE:
                return Material.BLUE_WOOL;
            case GREEN:
                return Material.GREEN_WOOL;
            case YELLOW:
                return Material.YELLOW_WOOL;
            // Add other colors as needed
            default:
                return Material.WHITE_WOOL;
        }
    }
}