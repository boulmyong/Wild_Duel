package com.wildduel.gui;

import com.wildduel.game.GameManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.game.TeamAdminManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AdminGUI {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TpaManager tpaManager;
    private final TeamAdminManager teamAdminManager;

    public AdminGUI(GameManager gameManager, TeamManager teamManager, TpaManager tpaManager, TeamAdminManager teamAdminManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.tpaManager = tpaManager;
        this.teamAdminManager = teamAdminManager;
    }

    public void open(Player admin) {
        Inventory gui = Bukkit.createInventory(null, 27, "§4[관리자 패널] 와일드 듀얼");

        // --- Separators for visual grouping ---
        ItemStack separator = createButton(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17) { // Fill top and bottom rows, leaving middle clear for now
                // gui.setItem(i, separator);
            }
        }

        // --- Row 1: Core Actions & Info ---
        gui.setItem(1, createButton(Material.LIME_WOOL, "§a게임 시작", "10초 카운트다운을 시작합니다."));
        gui.setItem(4, createButton(Material.TNT, "§c랜덤 팀 배정", "모든 플레이어를 무작위로 팀에 배정합니다."));
        gui.setItem(7, createButton(Material.BARRIER, "§c게임 초기화 (정보)", "월드를 초기화하려면", "§e/wd reset confirm §c을 입력하세요."));

        // --- Row 2: Settings & Management ---
        gui.setItem(10, createButton(Material.WRITABLE_BOOK, "§7팀 자율 선택: " + (gameManager.isPlayerTeamSelectionEnabled() ? "§a허용" : "§c차단"), "플레이어가 /팀선택 명령어로", "자유롭게 팀을 선택하거나 변경합니다.", "§e클릭하여 상태를 변경합니다."));
        gui.setItem(12, createButton(Material.FURNACE, "§7자동 제련: " + (gameManager.isAutoSmeltEnabled() ? "§a켜짐" : "§c꺼짐"), "철, 금, 구리 등 특정 광물을 캘 때", "자동으로 제련된 상태로 드롭됩니다.", "§e클릭하여 상태를 변경합니다."));
        gui.setItem(14, createButton(Material.PLAYER_HEAD, "§6수동 팀 배정", "GUI를 통해 특정 플레이어를", "원하는 팀으로 강제 이동시킵니다."));
        gui.setItem(16, createButton(Material.ENDER_PEARL, "§dTPA 쿨타임 초기화", "모든 플레이어의 TPA 관련", "명령어 쿨타임을 즉시 초기화합니다."));

        // --- Row 2: In-Game Time Adjustment ---
        gui.setItem(13, createButton(Material.DIAMOND_BLOCK, "§b남은 시간 실시간 조절", "§a좌클릭: +60초", "§c우클릭: -60초", "§e(파밍 중에만 작동)"));

        // --- Row 3: Initial Farming Time Settings ---
        gui.setItem(20, createButton(Material.REDSTONE_BLOCK, "§c-60초"));
        gui.setItem(21, createButton(Material.RED_STAINED_GLASS_PANE, "§c-10초"));
        gui.setItem(22, createButton(Material.CLOCK, "§e총 파밍 시간: §f" + gameManager.getInitialPrepTimeSeconds() + "초", "게임 시작 전 시간을 설정합니다."));
        gui.setItem(23, createButton(Material.LIME_STAINED_GLASS_PANE, "§a+10초"));
        gui.setItem(24, createButton(Material.EMERALD_BLOCK, "§a+60초"));

        admin.openInventory(gui);
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}