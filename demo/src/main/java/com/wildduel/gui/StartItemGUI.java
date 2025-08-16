package com.wildduel.gui;

import com.wildduel.WildDuel;
import com.wildduel.util.PlayerInventorySnapshot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StartItemGUI {

    private final WildDuel plugin;
    private Inventory inventory;
    private PlayerInventorySnapshot currentSnapshot;

    public StartItemGUI(WildDuel plugin, PlayerInventorySnapshot snapshot) {
        this.plugin = plugin;
        // Use the message file for the title to match the listener
        this.inventory = Bukkit.createInventory(null, 45, plugin.getMessage("gui.startitem.title"));
        this.currentSnapshot = snapshot;
        initializeItems();
    }

    private void initializeItems() {
        // Main inventory slots (0-35)
        if (currentSnapshot != null && currentSnapshot.getContents() != null) {
            for (int i = 0; i < 36; i++) {
                if (currentSnapshot.getContents()[i] != null) {
                    inventory.setItem(i, currentSnapshot.getContents()[i]);
                }
            }
        }

        // Armor slots (36-39)
        if (currentSnapshot != null && currentSnapshot.getArmorContents() != null) {
            ItemStack[] armor = currentSnapshot.getArmorContents();
            if (armor.length > 0 && armor[0] != null) inventory.setItem(36, armor[0]); // Boots
            if (armor.length > 1 && armor[1] != null) inventory.setItem(37, armor[1]); // Leggings
            if (armor.length > 2 && armor[2] != null) inventory.setItem(38, armor[2]); // Chestplate
            if (armor.length > 3 && armor[3] != null) inventory.setItem(39, armor[3]); // Helmet
        }

        // Off-hand slot (40)
        if (currentSnapshot != null && currentSnapshot.getOffHandContent() != null) {
            inventory.setItem(40, currentSnapshot.getOffHandContent());
        }

        // Set the save button first
        inventory.setItem(44, createGuiItem(Material.LIME_WOOL, plugin.getMessage("gui.startitem.button.save")));

        // Fill non-interactive slots (41, 42, 43) with gray stained glass pane
        ItemStack grayGlassPane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 41; i <= 43; i++) {
            inventory.setItem(i, grayGlassPane);
        }
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }
}