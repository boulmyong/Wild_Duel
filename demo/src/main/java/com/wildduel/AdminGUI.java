package com.wildduel;

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
        Inventory gui = Bukkit.createInventory(null, 27, "§4와일드 듀얼 - 관리자 패널");

        // Game Control Buttons
        gui.setItem(0, createButton(Material.BEACON, "§a월드 스폰 설정", "메인 스폰 지점을 현재 위치로 설정합니다."));
        gui.setItem(1, createButton(Material.DIAMOND_SWORD, "§b듀얼 시작 지점 설정", "듀얼 시작 위치를 현재 위치로 설정합니다."));
        gui.setItem(2, createButton(Material.GREEN_WOOL, "§2게임 시작", "준비 단계를 끝내고 게임을 시작합니다."));

        // Team Control Buttons
        gui.setItem(9, createButton(Material.PLAYER_HEAD, "§6수동 팀 배정", "팀 관리 GUI를 엽니다."));
        gui.setItem(10, createButton(Material.TNT, "§c랜덤 팀 배정", "모든 플레이어를 무작위로 팀에 배정합니다."));

        // Settings Buttons
        ItemStack autoSmeltButton = createButton(
                Material.FURNACE,
                "§7자동 제련: " + (gameManager.isAutoSmeltEnabled() ? "§a활성화" : "§c비활성화"),
                "클릭하여 기능을 켜거나 끕니다."
        );
        gui.setItem(18, autoSmeltButton);

        ItemStack teamSelectButton = createButton(
                Material.WRITABLE_BOOK,
                "§7팀 직접 선택: " + (gameManager.isPlayerTeamSelectionEnabled() ? "§a허용" : "§c차단"),
                "플레이어가 /team 명령어로 직접 팀을 선택하는 기능입니다."
        );
        gui.setItem(11, teamSelectButton);

        ItemStack prepTimeButton = createButton(
                Material.CLOCK,
                "§e준비 시간: §f" + gameManager.getInitialPrepTimeSeconds() + "초",
                "아래 버튼으로 시간을 조절하세요."
        );
        gui.setItem(19, prepTimeButton);

        gui.setItem(20, createButton(Material.EMERALD_BLOCK, "§a+60초"));
        gui.setItem(21, createButton(Material.LIME_STAINED_GLASS_PANE, "§a+10초"));
        gui.setItem(22, createButton(Material.RED_STAINED_GLASS_PANE, "§c-10초"));
        gui.setItem(23, createButton(Material.REDSTONE_BLOCK, "§c-60초"));

        // TPA Control
        gui.setItem(17, createButton(Material.ENDER_PEARL, "§d모든 TPA 쿨타임 초기화", "모든 플레이어의 TPA 쿨타임을 초기화합니다."));

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