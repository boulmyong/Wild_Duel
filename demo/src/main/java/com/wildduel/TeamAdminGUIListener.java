package com.wildduel;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamAdminGUIListener implements Listener {

    private final TeamAdminManager adminManager;
    private final TeamManager teamManager;

    public TeamAdminGUIListener(TeamAdminManager adminManager, TeamManager teamManager) {
        this.adminManager = adminManager;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("팀 관리")) {
            return;
        }

        event.setCancelled(true);
        Player admin = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        TeamAdminManager.PlayerSelection selection = adminManager.getPlayerSelection(admin);

        // Handle clicks
        Material type = clickedItem.getType();
        if (type == Material.PLAYER_HEAD) {
            Player clickedPlayer = Bukkit.getPlayer(clickedItem.getItemMeta().getDisplayName());
            if (clickedPlayer != null) {
                selection.setSelectedPlayer(clickedPlayer.getUniqueId());
            }
        } else if (type == Material.ARROW) {
            if (clickedItem.getItemMeta().getDisplayName().contains("이전")) {
                selection.setCurrentPage(selection.getCurrentPage() - 1);
            } else {
                selection.setCurrentPage(selection.getCurrentPage() + 1);
            }
        } else if (type.name().endsWith("_WOOL") || type == Material.GLASS) {
            selection.setSelectedTeam(getTeamTypeFromMaterial(type));
        } else if (type == Material.ANVIL) {
            applyTeamSelection(admin, selection);
        }

        // Refresh GUI
        new TeamAdminGUI(adminManager, teamManager).open(admin);
    }

    private void applyTeamSelection(Player admin, TeamAdminManager.PlayerSelection selection) {
        UUID selectedPlayerUUID = selection.getSelectedPlayer();
        TeamType selectedTeam = selection.getSelectedTeam();

        if (selectedPlayerUUID == null || selectedTeam == null) {
            admin.sendMessage("§c플레이어와 팀을 모두 선택하세요.");
            return;
        }

        Player selectedPlayer = Bukkit.getPlayer(selectedPlayerUUID);
        if (selectedPlayer == null) {
            admin.sendMessage("§c선택한 플레이어가 오프라인입니다.");
            return;
        }

        teamManager.leaveTeam(selectedPlayer); // Clear previous team

        if (selectedTeam == TeamType.RED || selectedTeam == TeamType.BLUE) {
            teamManager.joinTeam(selectedPlayer, selectedTeam.getName());
        } else if (selectedTeam == TeamType.SPECTATOR) {
            selectedPlayer.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }

        admin.sendMessage(String.format("§a%s님을 %s 팀으로 설정했습니다.", selectedPlayer.getName(), selectedTeam.getName()));
    }

    private TeamType getTeamTypeFromMaterial(Material material) {
        if (material == Material.RED_WOOL) return TeamType.RED;
        if (material == Material.BLUE_WOOL) return TeamType.BLUE;
        if (material == Material.GLASS) return TeamType.SPECTATOR;
        return TeamType.NONE;
    }
}
