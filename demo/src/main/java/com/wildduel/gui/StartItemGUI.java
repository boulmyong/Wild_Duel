package com.wildduel.gui;

import com.wildduel.util.PlayerInventorySnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class StartItemGUI {

    private Inventory inventory;
    private PlayerInventorySnapshot currentSnapshot;

    public StartItemGUI(PlayerInventorySnapshot snapshot) {
        this.inventory = Bukkit.createInventory(null, 45, ChatColor.BLUE + "시작 아이템 설정");
        this.currentSnapshot = snapshot;
        initializeItems();
    }

    private void initializeItems() {
        // Main inventory slots (0-35)
        for (int i = 0; i < 36; i++) {
            if (currentSnapshot.getContents()[i] != null) {
                inventory.setItem(i, currentSnapshot.getContents()[i]);
            }
        }

        // Armor slots (36-39)
        // Helmet (39), Chestplate (38), Leggings (37), Boots (36) in Bukkit API
        // Mapping to GUI slots: 36 (Boots), 37 (Leggings), 38 (Chestplate), 39 (Helmet)
        ItemStack[] armor = currentSnapshot.getArmorContents();
        if (armor[0] != null) inventory.setItem(36, armor[0]); // Boots
        if (armor[1] != null) inventory.setItem(37, armor[1]); // Leggings
        if (armor[2] != null) inventory.setItem(38, armor[2]); // Chestplate
        if (armor[3] != null) inventory.setItem(39, armor[3]); // Helmet

        // Off-hand slot (40)
        if (currentSnapshot.getOffHandContent() != null) {
            inventory.setItem(40, currentSnapshot.getOffHandContent());
        }

        // Set the save button first
        inventory.setItem(44, createGuiItem(Material.LIME_WOOL, ChatColor.GREEN + "설정 저장"));

        // Fill non-interactive slots (41, 42, 43) with gray stained glass pane
        ItemStack grayGlassPane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 41; i <= 43; i++) {
            inventory.setItem(i, grayGlassPane);
        }
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public PlayerInventorySnapshot getCurrentSnapshot() {
        // Reconstruct snapshot from current GUI state
        ItemStack[] newContents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            newContents[i] = inventory.getItem(i);
        }

        ItemStack[] newArmorContents = new ItemStack[4];
        newArmorContents[0] = inventory.getItem(36); // Boots
        newArmorContents[1] = inventory.getItem(37); // Leggings
        newArmorContents[2] = inventory.getItem(38); // Chestplate
        newArmorContents[3] = inventory.getItem(39); // Helmet

        ItemStack newOffHandContent = inventory.getItem(40);

        return new PlayerInventorySnapshot(newContents, newArmorContents, newOffHandContent);
    }

    public Inventory getInventory() {
        return inventory;
    }
}
