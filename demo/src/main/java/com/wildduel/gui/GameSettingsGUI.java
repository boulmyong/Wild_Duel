package com.wildduel.gui;

import com.wildduel.game.GameManager;
import com.wildduel.game.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GameSettingsGUI {

    private final GameManager gameManager;

    public GameSettingsGUI(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player admin) {
        Inventory gui = Bukkit.createInventory(null, 27, AdminGUI.GAME_SETTINGS_TITLE);

        // Row 2: Settings
        String gameModeName = gameManager.getGameMode() == GameMode.TEAM ? "§c팀전" : "§9개인전";
        gui.setItem(10, createButton(Material.RED_BANNER, "§b게임 모드 변경", "§7현재: " + gameModeName, "§e클릭하여 모드를 전환합니다."));
        
        gui.setItem(11, createButton(Material.FURNACE, "§b자동 제련", "§7현재: " + (gameManager.isAutoSmeltEnabled() ? "§a켜짐" : "§c꺼짐"), "§e클릭하여 상태를 변경합니다."));
        
        gui.setItem(12, createButton(Material.WRITABLE_BOOK, "§b팀 자율 선택", "§7현재: " + (gameManager.isPlayerTeamSelectionEnabled() ? "§a허용" : "§c차단"), "§e클릭하여 상태를 변경합니다."));
        
        gui.setItem(13, createButton(Material.CHEST, "§b시작 아이템 설정", "기본 지급 아이템을 설정하는", "GUI를 엽니다."));

        // Row 3: Back button
        gui.setItem(26, createButton(Material.BARRIER, "§c뒤로가기"));

        admin.openInventory(gui);
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
