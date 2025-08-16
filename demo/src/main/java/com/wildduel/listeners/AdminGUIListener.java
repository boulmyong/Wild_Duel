package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.*;
import com.wildduel.gui.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminGUIListener implements Listener {

    private final WildDuel plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TeamAdminManager teamAdminManager;
    private final TpaManager tpaManager;

    // Set to track players confirming a reset
    private final Set<UUID> playersConfirmingReset = new HashSet<>();

    public AdminGUIListener(WildDuel plugin, GameManager gameManager, TeamManager teamManager, TeamAdminManager teamAdminManager, TpaManager tpaManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.teamAdminManager = teamAdminManager;
        this.tpaManager = tpaManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        String title = view.getTitle();
        Player admin = (Player) event.getWhoClicked();

        // Delegate to the correct handler based on GUI title
        if (title.equals(AdminGUI.MAIN_TITLE)) {
            handleMainMenuClick(event, admin);
        } else if (title.equals(AdminGUI.GAME_SETTINGS_TITLE)) {
            handleGameSettingsClick(event, admin);
        } else if (title.equals(AdminGUI.TIME_SETTINGS_TITLE)) {
            handleTimeSettingsClick(event, admin);
        } else if (title.equals(AdminGUI.WORLD_SETTINGS_TITLE)) {
            handleWorldSettingsClick(event, admin);
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event, Player admin) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material clickedMaterial = clickedItem.getType();

        // If player clicks anything other than the confirm button, cancel the confirmation state.
        if (clickedMaterial != Material.REDSTONE_BLOCK && clickedMaterial != Material.TNT) {
            playersConfirmingReset.remove(admin.getUniqueId());
        }

        switch (clickedMaterial) {
            case LIME_WOOL:
                gameManager.askAdminToStart(admin);
                admin.closeInventory();
                break;
            case ANVIL:
                admin.closeInventory();
                new TeamAdminGUI(plugin, teamAdminManager, teamManager).open(admin);
                break;
            case TNT: // First click for reset
                playersConfirmingReset.add(admin.getUniqueId());
                admin.sendMessage("§c정말로 게임을 초기화하시겠습니까? 확인 버튼을 다시 눌러주세요.");
                new AdminGUI(plugin, gameManager, playersConfirmingReset).open(admin); // Refresh to show confirm button
                return;
            case REDSTONE_BLOCK: // This is now the confirm button
                if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().contains("초기화 확인")) {
                    if (playersConfirmingReset.remove(admin.getUniqueId())) { // remove and check if it was present
                        gameManager.resetGame();
                        teamManager.resetTeams();
                        tpaManager.resetTpa();
                        admin.sendMessage("§a게임이 초기화되었습니다. (팀, TPA 정보 포함)");
                        admin.closeInventory();
                    }
                }
                break;
            case CRAFTING_TABLE:
                new GameSettingsGUI(gameManager).open(admin);
                break;
            case CLOCK:
                new TimeSettingsGUI(gameManager).open(admin);
                break;
            case COMMAND_BLOCK:
                new WorldSettingsGUI(gameManager).open(admin);
                break;
            case BARRIER:
                admin.closeInventory();
                break;
        }
    }

    private void handleGameSettingsClick(InventoryClickEvent event, Player admin) {
        playersConfirmingReset.remove(admin.getUniqueId()); // Cancel confirmation on sub-menu navigation
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        switch (clickedItem.getType()) {
            case RED_BANNER:
                if (gameManager.getGameMode() == GameMode.TEAM) {
                    gameManager.setGameMode(GameMode.SOLO);
                } else {
                    gameManager.setGameMode(GameMode.TEAM);
                }
                break;
            case FURNACE:
                gameManager.setAutoSmelt(!gameManager.isAutoSmeltEnabled());
                break;
            case WRITABLE_BOOK:
                gameManager.setPlayerTeamSelectionEnabled(!gameManager.isPlayerTeamSelectionEnabled());
                break;
            case CHEST:
                admin.closeInventory();
                new StartItemGUI(plugin, plugin.getDefaultStartInventory()).open(admin);
                return; 
            case BARRIER:
                new AdminGUI(plugin, gameManager, playersConfirmingReset).open(admin);
                return;
        }
        
        new GameSettingsGUI(gameManager).open(admin);
    }

    private void handleTimeSettingsClick(InventoryClickEvent event, Player admin) {
        playersConfirmingReset.remove(admin.getUniqueId());
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        switch (clickedItem.getType()) {
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
            case REDSTONE:
                gameManager.addTime(-60);
                break;
            case GLOWSTONE_DUST:
                gameManager.addTime(60);
                break;
            case BARRIER:
                new AdminGUI(plugin, gameManager, playersConfirmingReset).open(admin);
                return;
        }

        new TimeSettingsGUI(gameManager).open(admin);
    }

    private void handleWorldSettingsClick(InventoryClickEvent event, Player admin) {
        playersConfirmingReset.remove(admin.getUniqueId());
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        switch (clickedItem.getType()) {
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
            case BARRIER:
                new AdminGUI(plugin, gameManager, playersConfirmingReset).open(admin);
                return;
        }

        new WorldSettingsGUI(gameManager).open(admin);
    }
}
