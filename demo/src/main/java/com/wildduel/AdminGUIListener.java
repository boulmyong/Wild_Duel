package com.wildduel;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class AdminGUIListener implements Listener {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TpaManager tpaManager;
    private final TeamAdminManager teamAdminManager;

    public AdminGUIListener(GameManager gameManager, TeamManager teamManager, TpaManager tpaManager, TeamAdminManager teamAdminManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.tpaManager = tpaManager;
        this.teamAdminManager = teamAdminManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals("§4와일드 듀얼 - 관리자 패널")) {
            return;
        }

        // 1. Immediately cancel the event to prevent item moving.
        event.setCancelled(true);

        Player admin = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // 2. Send a debug message to the admin.
        admin.sendMessage("§e[DEBUG] Admin Panel click detected. Event cancelled.");

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Material type = clickedItem.getType();
        boolean refreshGui = true;

        switch (type) {
            case BEACON:
                admin.performCommand("wd set");
                admin.closeInventory();
                refreshGui = false;
                break;
            case DIAMOND_SWORD:
                admin.performCommand("wd sp");
                admin.closeInventory();
                refreshGui = false;
                break;
            case GREEN_WOOL:
                admin.performCommand("wd start");
                admin.closeInventory();
                refreshGui = false;
                break;
            case PLAYER_HEAD:
                new TeamAdminGUI(teamAdminManager, teamManager).open(admin);
                refreshGui = false;
                break;
            case TNT:
                admin.performCommand("wd randomteam");
                admin.closeInventory();
                refreshGui = false;
                break;
            case FURNACE:
                if (gameManager.getGameState() == GameState.FARMING || gameManager.getGameState() == GameState.BATTLE) {
                    admin.sendMessage("§c게임 중에는 자동 제련 설정을 변경할 수 없습니다.");
                } else {
                    gameManager.setAutoSmelt(!gameManager.isAutoSmeltEnabled());
                }
                break;
            case WRITABLE_BOOK:
                 if (gameManager.getGameState() == GameState.FARMING || gameManager.getGameState() == GameState.BATTLE) {
                    admin.sendMessage("§c게임 중에는 팀 선택 허용 여부를 변경할 수 없습니다.");
                } else {
                    gameManager.setPlayerTeamSelectionEnabled(!gameManager.isPlayerTeamSelectionEnabled());
                }
                break;
            case EMERALD_BLOCK:
                updatePrepTime(admin, 60);
                break;
            case LIME_STAINED_GLASS_PANE:
                updatePrepTime(admin, 10);
                break;
            case RED_STAINED_GLASS_PANE:
                updatePrepTime(admin, -10);
                break;
            case REDSTONE_BLOCK:
                updatePrepTime(admin, -60);
                break;
            case ENDER_PEARL:
                tpaManager.refreshAllCooldowns();
                admin.sendMessage("§a모든 TPA 쿨타임이 초기화되었습니다.");
                admin.closeInventory();
                refreshGui = false;
                break;
            default:
                refreshGui = false;
                break;
        }

        if (refreshGui) {
            new AdminGUI(gameManager, teamManager, tpaManager, teamAdminManager).open(admin);
        }
    }

    private void updatePrepTime(Player admin, int change) {
        if (gameManager.getGameState() == GameState.FARMING || gameManager.getGameState() == GameState.BATTLE) {
            admin.sendMessage("§c게임 중에는 준비 시간을 변경할 수 없습니다.");
            return;
        }
        int newTime = gameManager.getInitialPrepTimeSeconds() + change;
        gameManager.setPrepTime(newTime);
    }
}
