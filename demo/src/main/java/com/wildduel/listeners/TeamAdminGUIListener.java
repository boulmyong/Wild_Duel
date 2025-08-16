package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TeamType;
import com.wildduel.gui.TeamAdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class TeamAdminGUIListener implements Listener {

    private final WildDuel plugin;
    private final TeamAdminManager adminManager;
    private final TeamManager teamManager;

    public TeamAdminGUIListener(WildDuel plugin, TeamAdminManager adminManager, TeamManager teamManager) {
        this.plugin = plugin;
        this.adminManager = adminManager;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals(plugin.getMessage("gui.teamadmin.title"))) {
            return;
        }

        if (event.getClickedInventory() != view.getTopInventory()) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Player admin = (Player) event.getWhoClicked();
        TeamAdminManager.PlayerSelection selection = adminManager.getPlayerSelection(admin);
        boolean refreshGui = true;

        Material type = clickedItem.getType();
        ItemMeta meta = clickedItem.getItemMeta();

        if (type == Material.PLAYER_HEAD && meta != null) {
            String uuidString = meta.getPersistentDataContainer().get(TeamAdminGUI.PLAYER_UUID_KEY, PersistentDataType.STRING);
            if (uuidString != null) {
                UUID playerUUID = UUID.fromString(uuidString);
                selection.setSelectedPlayer(playerUUID);

                // Get the player's current team and update the selection state
                Player clickedPlayer = Bukkit.getPlayer(playerUUID);
                if (clickedPlayer != null) {
                    String teamName = teamManager.getPlayerTeam(clickedPlayer);
                    TeamType teamType = getTeamTypeFromName(teamName);
                    selection.setSelectedTeam(teamType);
                }
            }
        } else if (type == Material.ARROW) {
            if (clickedItem.getItemMeta().getDisplayName().contains("이전")) {
                // 페이지 번호가 음수가 되지 않도록 방지합니다.
                if (selection.getCurrentPage() > 0) {
                    selection.setCurrentPage(selection.getCurrentPage() - 1);
                }
            } else { // "다음"
                selection.setCurrentPage(selection.getCurrentPage() + 1);
            }
        } else if (type.name().endsWith("_WOOL") || type == Material.SPYGLASS) {
            selection.setSelectedTeam(getTeamTypeFromMaterial(type));
        } else if (type == Material.ANVIL) {
            applyTeamSelection(admin, selection);
            refreshGui = true;
        } else {
            refreshGui = false;
        }

        if (refreshGui) {
            new TeamAdminGUI(plugin, adminManager, teamManager).open(admin);
        }
    }

    private void applyTeamSelection(Player admin, TeamAdminManager.PlayerSelection selection) {
        UUID selectedPlayerUUID = selection.getSelectedPlayer();
        TeamType selectedTeamType = selection.getSelectedTeam();

        if (selectedPlayerUUID == null) {
            admin.sendMessage(plugin.getMessage("gui.teamadmin.error.no-player-selected"));
            return;
        }
        if (selectedTeamType == null) {
            admin.sendMessage(plugin.getMessage("gui.teamadmin.error.no-team-selected"));
            return;
        }

        Player selectedPlayer = Bukkit.getPlayer(selectedPlayerUUID);
        if (selectedPlayer == null) {
            admin.sendMessage(plugin.getMessage("gui.teamadmin.error.player-offline"));
            return;
        }

        // "없음"을 선택한 경우, 팀에서만 내보냅니다.
        if (selectedTeamType == TeamType.NONE) {
            teamManager.leaveTeam(selectedPlayer);
            admin.sendMessage(plugin.getMessage("gui.teamadmin.success.set-no-team", "%player%", selectedPlayer.getName()));
            selectedPlayer.sendMessage(plugin.getMessage("gui.teamadmin.info.removed-from-team", "%admin%", admin.getName()));
        } else {
            // RED, BLUE, SPECTATOR 팀을 선택한 경우, joinTeam을 호출합니다.
            // joinTeam 메소드가 기존 팀 탈퇴, 게임모드 변경 등을 모두 알아서 처리합니다.
            boolean success = teamManager.joinTeam(selectedPlayer, selectedTeamType.getName());
            if (success) {
                if (selectedTeamType == TeamType.SPECTATOR) {
                    admin.sendMessage(plugin.getMessage("gui.teamadmin.success.set-spectator", "%player%", selectedPlayer.getName()));
                    selectedPlayer.sendMessage(plugin.getMessage("gui.teamadmin.info.spectator-set", "%admin%", admin.getName()));
                } else {
                    admin.sendMessage(plugin.getMessage("gui.teamadmin.success.set-team", "%player%", selectedPlayer.getName(), "%team%", selectedTeamType.getName()));
                    selectedPlayer.sendMessage(plugin.getMessage("gui.teamadmin.info.team-changed", "%admin%", admin.getName(), "%teamcolor%", selectedTeamType.getColor().toString(), "%team%", selectedTeamType.getName()));
                }
            }
        }
    }

    private TeamType getTeamTypeFromMaterial(Material material) {
        if (material == Material.RED_WOOL) return TeamType.RED;
        if (material == Material.BLUE_WOOL) return TeamType.BLUE;
        if (material == Material.SPYGLASS) return TeamType.SPECTATOR;
        return TeamType.NONE;
    }

    // Helper method to convert team name string to TeamType enum
    private TeamType getTeamTypeFromName(String name) {
        if (name == null) {
            return TeamType.NONE;
        }
        for (TeamType type : TeamType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return TeamType.NONE;
    }
}
