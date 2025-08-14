package com.wildduel.gui;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;

import com.wildduel.game.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

import java.util.List;

public class TeamGUI {

    private final WildDuel plugin;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    public static final NamespacedKey TEAM_NAME_KEY = new NamespacedKey(WildDuel.getInstance(), "team_name");
    

    public TeamGUI(WildDuel plugin, TeamManager teamManager, GameManager gameManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, plugin.getMessage("gui.team.title"));

        // Team Selection Items
        for (TeamManager.TeamData team : teamManager.getTeams()) {
            ItemStack teamItem = new ItemStack(getWoolMaterial(team.getColor()), 1);
            ItemMeta meta = teamItem.getItemMeta();
            meta.setDisplayName(plugin.getMessage("gui.team.button.join-team", "%color%", team.getColor().toString(), "%team%", team.getName()));
            meta.getPersistentDataContainer().set(TEAM_NAME_KEY, PersistentDataType.STRING, team.getName());
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getMessage("gui.team.lore.click-to-join"));
            lore.add(plugin.getMessage("separator.blank"));
            lore.add(plugin.getMessage("gui.team.lore.current-members", "%count%", String.valueOf(team.getPlayers().size())));

            int count = 0;
            for (Player member : team.getPlayers()) {
                if (count < 10) {
                    lore.add(plugin.getMessage("gui.team.lore.member-name", "%player%", member.getName()));
                    count++;
                } else {
                    lore.add(plugin.getMessage("gui.team.lore.more-members", "%count%", String.valueOf(team.getPlayers().size() - count)));
                    break;
                }
            }
            meta.setLore(lore);
            teamItem.setItemMeta(meta);
            gui.addItem(teamItem);
        }

        


        ItemStack leaveItem = new ItemStack(Material.BARRIER);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        leaveMeta.setDisplayName(plugin.getMessage("gui.team.button.leave-team"));
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