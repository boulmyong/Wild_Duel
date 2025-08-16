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

        // 일반 팀 아이템 추가
        for (TeamManager.TeamData team : teamManager.getTeams()) {
            if (team.getName().equals(TeamManager.SPECTATOR_TEAM_NAME)) {
                continue; // 관전자 팀은 아래에서 따로 처리
            }
            ItemStack teamItem = new ItemStack(getWoolMaterial(team.getColor()), 1);
            ItemMeta meta = teamItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getMessage("gui.team.button.join-team", "%color%", team.getColor().toString(), "%team%", team.getName()));
                meta.getPersistentDataContainer().set(TEAM_NAME_KEY, PersistentDataType.STRING, team.getName());
                meta.setLore(createLore(team));
                teamItem.setItemMeta(meta);
            }
            gui.addItem(teamItem);
        }

        // 관전자 팀 아이템 추가 (망원경)
        TeamManager.TeamData spectatorTeam = null;
        for (TeamManager.TeamData team : teamManager.getTeams()) {
            if (team.getName().equals(TeamManager.SPECTATOR_TEAM_NAME)) {
                spectatorTeam = team;
                break;
            }
        }

        if (spectatorTeam != null) {
            ItemStack spectatorItem = new ItemStack(Material.SPYGLASS);
            ItemMeta specMeta = spectatorItem.getItemMeta();
            if (specMeta != null) {
                specMeta.setDisplayName(plugin.getMessage("gui.team.button.join-team", "%color%", spectatorTeam.getColor().toString(), "%team%", spectatorTeam.getName()));
                specMeta.getPersistentDataContainer().set(TEAM_NAME_KEY, PersistentDataType.STRING, spectatorTeam.getName());
                specMeta.setLore(createLore(spectatorTeam));
                spectatorItem.setItemMeta(specMeta);
            }
            gui.setItem(7, spectatorItem); // 7번 슬롯에 배치
        }

        // 팀 나가기 아이템
        ItemStack leaveItem = new ItemStack(Material.BARRIER);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        if (leaveMeta != null) {
            leaveMeta.setDisplayName(plugin.getMessage("gui.team.button.leave-team"));
            leaveItem.setItemMeta(leaveMeta);
        }
        gui.setItem(8, leaveItem);

        player.openInventory(gui);
    }

    // 아이템 설명을 생성하는 메소드 (코드 중복 제거)
    private List<String> createLore(TeamManager.TeamData team) {
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
        return lore;
    }

    // 양털 색상을 반환하는 메소드
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
            case GRAY: // 회색 추가
                return Material.GRAY_WOOL;
            default:
                return Material.WHITE_WOOL;
        }
    }
}
