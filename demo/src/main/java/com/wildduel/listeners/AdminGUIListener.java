package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameMode;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.gui.AdminGUI;
import com.wildduel.gui.StartItemGUI;
import com.wildduel.gui.TeamAdminGUI;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class AdminGUIListener implements Listener {

    private final WildDuel plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TpaManager tpaManager;
    private final TeamAdminManager teamAdminManager;

    public AdminGUIListener(WildDuel plugin, GameManager gameManager, TeamManager teamManager, TpaManager tpaManager, TeamAdminManager teamAdminManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.tpaManager = tpaManager;
        this.teamAdminManager = teamAdminManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (!view.getTitle().equals("§4[관리자 패널] 와일드 듀얼")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Player admin = (Player) event.getWhoClicked();
        Material clickedMaterial = clickedItem.getType();

        switch (clickedMaterial) {
            // Game Control
            case LIME_WOOL:
                gameManager.askAdminToStart(admin);
                admin.closeInventory();
                break;
            case TNT:
                gameManager.resetGame();
                admin.sendMessage("§a게임이 초기화되었습니다.");
                break;
            case COMMAND_BLOCK:
                admin.sendMessage("§c월드 재생성을 시작합니다. 모든 플레이어가 추방됩니다.");
                gameManager.executeWorldRegeneration();
                admin.closeInventory();
                break;
            case ENDER_PEARL:
                World lobbyWorld = gameManager.getLobbyWorld();
                if (lobbyWorld != null) {
                    admin.teleport(lobbyWorld.getSpawnLocation());
                    admin.sendMessage("§d로비 월드로 이동했습니다.");
                } else {
                    admin.sendMessage("§c로비 월드를 찾을 수 없습니다.");
                }
                admin.closeInventory();
                break;
            case COMPASS:
                World gameWorld = gameManager.getGameWorld();
                if (gameWorld != null) {
                    admin.teleport(gameWorld.getSpawnLocation());
                    admin.sendMessage("§d게임 월드로 이동했습니다.");
                } else {
                    admin.sendMessage("§c게임 월드가 아직 생성되지 않았습니다.");
                }
                admin.closeInventory();
                break;

            // Game Settings
            case RED_BANNER:
                if (gameManager.getGameMode() == GameMode.TEAM) {
                    gameManager.setGameMode(GameMode.SOLO);
                    admin.sendMessage("§b게임 모드가 §9개인전§b으로 변경되었습니다.");
                } else {
                    gameManager.setGameMode(GameMode.TEAM);
                    admin.sendMessage("§b게임 모드가 §c팀전§b으로 변경되었습니다.");
                }
                break;
            case FURNACE:
                gameManager.setAutoSmelt(!gameManager.isAutoSmeltEnabled());
                admin.sendMessage("§b자동 제련이 " + (gameManager.isAutoSmeltEnabled() ? "§a활성화" : "§c비활성화") + "되었습니다.");
                break;
            case WRITABLE_BOOK:
                gameManager.setPlayerTeamSelectionEnabled(!gameManager.isPlayerTeamSelectionEnabled());
                admin.sendMessage("§b팀 자율 선택이 " + (gameManager.isPlayerTeamSelectionEnabled() ? "§a허용" : "§c차단") + "되었습니다.");
                break;
            case CHEST:
                new StartItemGUI(plugin.getDefaultStartInventory()).open(admin);
                break;

            // Player & Team Management
            case ANVIL:
                new TeamAdminGUI(plugin, teamAdminManager, teamManager).open(admin);
                break;
            case NETHER_STAR:
                teamManager.assignRandomTeams();
                admin.sendMessage("§6모든 플레이어를 랜덤 팀에 배정했습니다.");
                break;
            case BUCKET:
                tpaManager.refreshAllCooldowns();
                admin.sendMessage("§6모든 TPA 쿨타임이 초기화되었습니다.");
                break;

            // Time Management
            case CLOCK:
                if (event.getClick() == ClickType.LEFT) {
                    gameManager.addTime(60);
                } else if (event.getClick() == ClickType.RIGHT) {
                    gameManager.addTime(-60);
                }
                break;
            case REDSTONE_BLOCK:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() - 60);
                break;
            case RED_STAINED_GLASS_PANE:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() - 10);
                break;
            case LIME_STAINED_GLASS_PANE:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() + 10);
                break;
            case EMERALD_BLOCK:
                gameManager.setPrepTime(gameManager.getInitialPrepTimeSeconds() + 60);
                break;

            default:
                // Unclickable items, do nothing
                return;
        }

        // Refresh the GUI to show updated state
        new AdminGUI(plugin, gameManager, teamManager, tpaManager, teamAdminManager).open(admin);
    }
}