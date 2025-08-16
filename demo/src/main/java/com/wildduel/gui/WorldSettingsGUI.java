package com.wildduel.gui;

import com.wildduel.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class WorldSettingsGUI {

    private final GameManager gameManager;

    public WorldSettingsGUI(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player admin) {
        Inventory gui = Bukkit.createInventory(null, 27, AdminGUI.WORLD_SETTINGS_TITLE);

        // Row 2: World Actions
        gui.setItem(10, createButton(Material.COMMAND_BLOCK, "§c월드 재생성", "§e주의: §f게임 월드를 삭제하고", "§f새로 생성합니다. 모든 플레이어가", "§f추방되니 미리 공지해주세요."));
        gui.setItem(12, createButton(Material.ENDER_PEARL, "§d로비로 텔레포트", "로비 월드로 이동합니다."));
        gui.setItem(14, createButton(Material.COMPASS, "§d게임 월드로 텔레포트", "게임 월드로 이동합니다."));

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
