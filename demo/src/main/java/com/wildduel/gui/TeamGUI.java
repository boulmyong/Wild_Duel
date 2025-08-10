package com.wildduel.gui;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameMode;
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
import java.util.Arrays;
import java.util.List;

public class TeamGUI {

    private final WildDuel plugin;
    private final TeamManager teamManager;
    private final GameManager gameManager;
    public static final NamespacedKey TEAM_NAME_KEY = new NamespacedKey(WildDuel.getInstance(), "team_name");
    public static final NamespacedKey GAME_MODE_KEY = new NamespacedKey(WildDuel.getInstance(), "game_mode");

    public TeamGUI(WildDuel plugin, TeamManager teamManager, GameManager gameManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 18, plugin.getMessage("gui.team.title"));

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

        // Game Mode Selection Items
        ItemStack teamModeItem = new ItemStack(Material.RED_BANNER);
        ItemMeta teamModeMeta = teamModeItem.getItemMeta();
        teamModeMeta.setDisplayName(plugin.getMessage("gui.team.button.team-mode"));
        teamModeMeta.setLore(Arrays.asList(plugin.getMessage("gui.team.lore.click-to-set-team-mode"), plugin.getMessage("gui.team.lore.current-mode", "%mode%", (gameManager.getGameMode() == GameMode.TEAM ? "&a팀전" : "&c개인전"))));
        teamModeMeta.getPersistentDataContainer().set(GAME_MODE_KEY, PersistentDataType.STRING, GameMode.TEAM.name());
        teamModeItem.setItemMeta(teamModeMeta);
        gui.setItem(6, teamModeItem);

        ItemStack soloModeItem = new ItemStack(Material.BLUE_BANNER);
        ItemMeta soloModeMeta = soloModeItem.getItemMeta();
        soloModeMeta.setDisplayName(plugin.getMessage("gui.team.button.solo-mode"));
        soloModeMeta.setLore(Arrays.asList(plugin.getMessage("gui.team.lore.click-to-set-solo-mode"), plugin.getMessage("gui.team.lore.current-mode", "%mode%", (gameManager.getGameMode() == GameMode.SOLO ? "&a개인전" : "&c팀전"))));
        soloModeMeta.getPersistentDataContainer().set(GAME_MODE_KEY, PersistentDataType.STRING, GameMode.SOLO.name());
        soloModeItem.setItemMeta(soloModeMeta);
        gui.setItem(7, soloModeItem);


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