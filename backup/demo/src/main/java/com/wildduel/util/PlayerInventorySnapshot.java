package com.wildduel.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerInventorySnapshot {

    private ItemStack[] contents;
    private ItemStack[] armorContents;
    private ItemStack offHandContent;

    public PlayerInventorySnapshot() {
        this.contents = new ItemStack[36]; // Main inventory slots
        this.armorContents = new ItemStack[4]; // Helmet, Chestplate, Leggings, Boots
        this.offHandContent = null;
    }

    public PlayerInventorySnapshot(ItemStack[] contents, ItemStack[] armorContents, ItemStack offHandContent) {
        this.contents = contents;
        this.armorContents = armorContents;
        this.offHandContent = offHandContent;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public ItemStack[] getArmorContents() {
        return armorContents;
    }

    public ItemStack getOffHandContent() {
        return offHandContent;
    }

    public void apply(Player player) {
        player.getInventory().clear();
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armorContents);
        player.getInventory().setItemInOffHand(offHandContent);
    }

    public void serialize(ConfigurationSection section) {
        section.set("contents", serializeItemStackList(contents));
        section.set("armorContents", serializeItemStackList(armorContents));
        section.set("offHandContent", offHandContent != null ? offHandContent.serialize() : null);
    }

    public static PlayerInventorySnapshot deserialize(ConfigurationSection section) {
        ItemStack[] contents = deserializeItemStackList(section.getList("contents"), 36);
        ItemStack[] armorContents = deserializeItemStackList(section.getList("armorContents"), 4);
        ItemStack offHandContent = null;
        if (section.isConfigurationSection("offHandContent")) {
            offHandContent = ItemStack.deserialize(section.getConfigurationSection("offHandContent").getValues(true));
        } else if (section.get("offHandContent") instanceof ItemStack) { // For direct ItemStack serialization
            offHandContent = (ItemStack) section.get("offHandContent");
        }

        return new PlayerInventorySnapshot(contents, armorContents, offHandContent);
    }

    private static List<java.util.Map<String, Object>> serializeItemStackList(ItemStack[] items) {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                list.add(item != null ? item.serialize() : null);
            }
        }
        return list;
    }

    private static ItemStack[] deserializeItemStackList(List<?> list, int size) {
        ItemStack[] items = new ItemStack[size];
        if (list != null) {
            for (int i = 0; i < list.size() && i < size; i++) {
                Object obj = list.get(i);
                if (obj instanceof java.util.Map) {
                    items[i] = ItemStack.deserialize((java.util.Map<String, Object>) obj);
                } else {
                    items[i] = null;
                }
            }
        }
        return items;
    }
}
