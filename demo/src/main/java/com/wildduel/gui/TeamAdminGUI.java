package com.wildduel.gui;

import com.wildduel.WildDuel;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeamAdminGUI {

    private final WildDuel plugin;
    private final TeamAdminManager adminManager;
    private final TeamManager teamManager;
    public static final NamespacedKey PLAYER_UUID_KEY = new NamespacedKey(WildDuel.getInstance(), "player_uuid");

    public TeamAdminGUI(WildDuel plugin, TeamAdminManager adminManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.adminManager = adminManager;
        this.teamManager = teamManager;
    }

    public void open(Player admin) {
        TeamAdminManager.PlayerSelection selection = adminManager.getPlayerSelection(admin);
        Inventory gui = Bukkit.createInventory(null, 54, plugin.getMessage("gui.teamadmin.title"));

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int page = selection.getCurrentPage();
        int startIndex = page * 27;

        for (int i = 0; i < 27; i++) {
            int playerIndex = startIndex + i;
            if (playerIndex < players.size()) {
                Player player = players.get(playerIndex);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    // Store UUID safely in Persistent Data Container
                    meta.getPersistentDataContainer().set(PLAYER_UUID_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
                    meta.setOwningPlayer(player);

                    // --- Visual Feedback for Selection ---
                    if (player.getUniqueId().equals(selection.getSelectedPlayer())) {
                        meta.setDisplayName(plugin.getMessage("gui.teamadmin.player.selected", "%player%", player.getName()));
                        meta.addEnchant(Enchantment.PROTECTION, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    } else {
                        meta.setDisplayName(plugin.getMessage("gui.teamadmin.player.normal", "%player%", player.getName()));
                    }
                    head.setItemMeta(meta);
                }
                gui.setItem(i, head);
            }
        }

        // Page Buttons
        if (page > 0) {
            gui.setItem(45, createButton(Material.ARROW, plugin.getMessage("gui.teamadmin.button.previous-page")));
        }
        // 다음 페이지에 표시할 플레이어가 남아있는 경우에만 버튼을 표시합니다.
        if (startIndex + 27 < players.size()) {
            gui.setItem(53, createButton(Material.ARROW, plugin.getMessage("gui.teamadmin.button.next-page")));
        }

        // Team Selection & Apply Buttons
        gui.setItem(46, createTeamButton(TeamType.RED, selection.getSelectedTeam()));
        gui.setItem(47, createTeamButton(TeamType.BLUE, selection.getSelectedTeam()));
        gui.setItem(48, createTeamButton(TeamType.NONE, selection.getSelectedTeam()));
        gui.setItem(50, createTeamButton(TeamType.SPECTATOR, selection.getSelectedTeam()));
        gui.setItem(49, createButton(Material.ANVIL, plugin.getMessage("gui.teamadmin.button.apply")));

        admin.openInventory(gui);
    }

    private ItemStack createButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTeamButton(TeamType teamType, TeamType selectedTeam) {
        ItemStack item = new ItemStack(getMaterialForTeam(teamType));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessage("gui.teamadmin.team-button", "%color%", teamType.getColor().toString(), "%team%", teamType.getName()));
            if (teamType == selectedTeam) {
                meta.addEnchant(Enchantment.PROTECTION, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getMaterialForTeam(TeamType teamType) {
        switch (teamType) {
            case RED: return Material.RED_WOOL;
            case BLUE: return Material.BLUE_WOOL;
            case SPECTATOR: return Material.GLASS;
            default: return Material.WHITE_WOOL;
        }
    }
}
