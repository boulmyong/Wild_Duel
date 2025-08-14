package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TeamType;
import com.wildduel.gui.TeamAdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
                selection.setSelectedPlayer(UUID.fromString(uuidString));
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
        } else if (type.name().endsWith("_WOOL") || type == Material.GLASS) {
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
        TeamType selectedTeam = selection.getSelectedTeam();

        if (selectedPlayerUUID == null) {
            admin.sendMessage(plugin.getMessage("gui.teamadmin.error.no-player-selected"));
            return;
        }
        if (selectedTeam == null) {
            admin.sendMessage(plugin.getMessage("gui.teamadmin.error.no-team-selected"));
            return;
        }

        Player selectedPlayer = Bukkit.getPlayer(selectedPlayerUUID);
        if (selectedPlayer == null) {
            admin.sendMessage(plugin.getMessage("gui.teamadmin.error.player-offline"));
            return;
        }

        // 관전자였던 플레이어를 다른 팀으로 옮길 경우 서바이벌 모드로 변경
        if (selectedPlayer.getGameMode() == GameMode.SPECTATOR && selectedTeam != TeamType.SPECTATOR) {
            selectedPlayer.setGameMode(GameMode.SURVIVAL);
        }

        if (selectedTeam == TeamType.RED || selectedTeam == TeamType.BLUE) {
            teamManager.joinTeam(selectedPlayer, selectedTeam.getName());
            admin.sendMessage(plugin.getMessage("gui.teamadmin.success.set-team", "%player%", selectedPlayer.getName(), "%team%", selectedTeam.getName()));
            selectedPlayer.sendMessage(plugin.getMessage("gui.teamadmin.info.team-changed", "%admin%", admin.getName(), "%teamcolor%", selectedTeam.getColor().toString(), "%team%", selectedTeam.getName()));
        } else {
            teamManager.leaveTeam(selectedPlayer);
            if (selectedTeam == TeamType.SPECTATOR) {
                selectedPlayer.setGameMode(GameMode.SPECTATOR);
                admin.sendMessage(plugin.getMessage("gui.teamadmin.success.set-spectator", "%player%", selectedPlayer.getName()));
                selectedPlayer.sendMessage(plugin.getMessage("gui.teamadmin.info.spectator-set", "%admin%", admin.getName()));
            } else { // NONE
                admin.sendMessage(plugin.getMessage("gui.teamadmin.success.set-no-team", "%player%", selectedPlayer.getName()));
                selectedPlayer.sendMessage(plugin.getMessage("gui.teamadmin.info.removed-from-team", "%admin%", admin.getName()));
            }
        }
    }

    private TeamType getTeamTypeFromMaterial(Material material) {
        if (material == Material.RED_WOOL) return TeamType.RED;
        if (material == Material.BLUE_WOOL) return TeamType.BLUE;
        if (material == Material.GLASS) return TeamType.SPECTATOR;
        return TeamType.NONE;
    }
}