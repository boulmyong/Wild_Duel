package com.wildduel;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamAdminGUI {

    private final TeamAdminManager adminManager;
    private final TeamManager teamManager;

    public TeamAdminGUI(TeamAdminManager adminManager, TeamManager teamManager) {
        this.adminManager = adminManager;
        this.teamManager = teamManager;
    }

    public void open(Player admin) {
        TeamAdminManager.PlayerSelection selection = adminManager.getPlayerSelection(admin);
        Inventory gui = Bukkit.createInventory(null, 54, "팀 관리");

        // Player Heads
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int page = selection.getCurrentPage();
        int startIndex = page * 27;

        for (int i = 0; i < 27; i++) {
            int playerIndex = startIndex + i;
            if (playerIndex < players.size()) {
                Player player = players.get(playerIndex);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(player);
                meta.setDisplayName(player.getName());
                if (player.getUniqueId().equals(selection.getSelectedPlayer())) {
                    meta.addEnchant(Enchantment.PROTECTION, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                head.setItemMeta(meta);
                gui.setItem(i, head);
            }
        }

        // Control Buttons
        // Page Buttons
        if (page > 0) {
            gui.setItem(45, createButton(Material.ARROW, "◀ 이전 페이지"));
        }
        if (players.size() > (page + 1) * 27) {
            gui.setItem(53, createButton(Material.ARROW, "▶ 다음 페이지"));
        }

        // Team Selection Buttons
        gui.setItem(46, createTeamButton(TeamType.RED, selection.getSelectedTeam()));
        gui.setItem(47, createTeamButton(TeamType.BLUE, selection.getSelectedTeam()));
        gui.setItem(48, createTeamButton(TeamType.NONE, selection.getSelectedTeam()));
        gui.setItem(50, createTeamButton(TeamType.SPECTATOR, selection.getSelectedTeam()));

        // Apply Button
        gui.setItem(49, createButton(Material.ANVIL, "§a설정 적용"));

        admin.openInventory(gui);
    }

    private ItemStack createButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTeamButton(TeamType teamType, TeamType selectedTeam) {
        ItemStack item = new ItemStack(getMaterialForTeam(teamType));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(teamType.getColor() + teamType.getName() + " Team");
        if (teamType == selectedTeam) {
            meta.addEnchant(Enchantment.PROTECTION, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterialForTeam(TeamType teamType) {
        switch (teamType) {
            case RED:
                return Material.RED_WOOL;
            case BLUE:
                return Material.BLUE_WOOL;
            case SPECTATOR:
                return Material.GLASS;
            default:
                return Material.WHITE_WOOL;
        }
    }
}
