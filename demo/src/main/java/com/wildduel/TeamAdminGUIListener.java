package com.wildduel;

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

    private final TeamAdminManager adminManager;
    private final TeamManager teamManager;

    public TeamAdminGUIListener(TeamAdminManager adminManager, TeamManager teamManager) {
        this.adminManager = adminManager;
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals("§8팀 수동 배정")) {
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
            // 3. Prevent negative page index
            if (clickedItem.getItemMeta().getDisplayName().contains("이전")) {
                if (selection.getCurrentPage() > 0) {
                    selection.setCurrentPage(selection.getCurrentPage() - 1);
                }
            } else {
                selection.setCurrentPage(selection.getCurrentPage() + 1);
            }
        } else if (type.name().endsWith("_WOOL") || type == Material.GLASS) {
            selection.setSelectedTeam(getTeamTypeFromMaterial(type));
        } else if (type == Material.ANVIL) {
            applyTeamSelection(admin, selection);
            refreshGui = false;
            admin.closeInventory();
        } else {
            refreshGui = false;
        }

        if (refreshGui) {
            new TeamAdminGUI(adminManager, teamManager).open(admin);
        }
    }

    private void applyTeamSelection(Player admin, TeamAdminManager.PlayerSelection selection) {
        UUID selectedPlayerUUID = selection.getSelectedPlayer();
        TeamType selectedTeam = selection.getSelectedTeam();

        if (selectedPlayerUUID == null) {
            admin.sendMessage("§c먼저 플레이어를 선택해주세요.");
            return;
        }
        if (selectedTeam == null) {
            admin.sendMessage("§c변경할 팀을 선택해주세요.");
            return;
        }

        Player selectedPlayer = Bukkit.getPlayer(selectedPlayerUUID);
        if (selectedPlayer == null) {
            admin.sendMessage("§c해당 플레이어는 오프라인 상태입니다.");
            return;
        }

        if (selectedTeam == TeamType.RED || selectedTeam == TeamType.BLUE) {
            teamManager.joinTeam(selectedPlayer, selectedTeam.getName());
            admin.sendMessage(String.format("§a%s님을 %s 팀으로 설정했습니다.", selectedPlayer.getName(), selectedTeam.getName()));
            selectedPlayer.sendMessage("§e[관리자] " + admin.getName() + "님이 당신을 " + selectedTeam.getColor() + selectedTeam.getName() + "§e 팀으로 옮겼습니다.");
        } else {
            teamManager.leaveTeam(selectedPlayer);
            if (selectedTeam == TeamType.SPECTATOR) {
                selectedPlayer.setGameMode(GameMode.SPECTATOR);
                admin.sendMessage(String.format("§a%s님을 관전자로 설정했습니다.", selectedPlayer.getName()));
                selectedPlayer.sendMessage("§e[관리자] " + admin.getName() + "님이 당신을 관전자로 설정했습니다.");
            } else { // NONE
                admin.sendMessage(String.format("§a%s님을 소속 없는 상태로 설정했습니다.", selectedPlayer.getName()));
                selectedPlayer.sendMessage("§e[관리자] " + admin.getName() + "님이 당신을 팀에서 제외했습니다.");
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