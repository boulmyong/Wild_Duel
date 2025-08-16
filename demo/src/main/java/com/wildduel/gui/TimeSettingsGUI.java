package com.wildduel.gui;

import com.wildduel.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class TimeSettingsGUI {

    private final GameManager gameManager;

    public TimeSettingsGUI(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player admin) {
        Inventory gui = Bukkit.createInventory(null, 36, AdminGUI.TIME_SETTINGS_TITLE);

        // --- Initial Farming Time Settings ---
        gui.setItem(10, createButton(Material.REDSTONE_BLOCK, "§c-60초 (초기)"));
        gui.setItem(11, createButton(Material.RED_STAINED_GLASS_PANE, "§c-10초 (초기)"));
        gui.setItem(12, createButton(Material.CLOCK, "§e총 파밍 시간: §f" + gameManager.getInitialPrepTimeSeconds() + "초", "§7게임 시작 전 시간을 설정합니다."));
        gui.setItem(13, createButton(Material.LIME_STAINED_GLASS_PANE, "§a+10초 (초기)"));
        gui.setItem(14, createButton(Material.EMERALD_BLOCK, "§a+60초 (초기)"));

        // --- Live Time Adjustment ---
        gui.setItem(21, createButton(Material.REDSTONE, "§c-60초 (라이브)", "§7파밍 중에만 작동합니다."));
        gui.setItem(22, createButton(Material.COMPASS, "§e라이브 시간 조절", "§7현재 남은 시간을 조절합니다.", "§7파밍 중에만 작동합니다.")); // Changed from WATCH to COMPASS for variety
        gui.setItem(23, createButton(Material.GLOWSTONE_DUST, "§a+60초 (라이브)", "§7파밍 중에만 작동합니다."));


        // --- Back button ---
        gui.setItem(35, createButton(Material.BARRIER, "§c뒤로가기"));

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
