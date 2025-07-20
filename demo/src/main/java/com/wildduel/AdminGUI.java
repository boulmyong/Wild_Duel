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
        Inventory gui = Bukkit.createInventory(null, 27, "§4Wild Duel - Admin Panel");

        // Game Control Buttons
        gui.setItem(0, createButton(Material.BEACON, "§aSet World Spawn", "Sets the main spawn point."));
        gui.setItem(1, createButton(Material.DIAMOND_SWORD, "§bSet Duel Start", "Sets the duel location."));
        gui.setItem(2, createButton(Material.GREEN_WOOL, "§2Start Game", "Begins the match."));

        // Team Control Buttons
        gui.setItem(9, createButton(Material.PLAYER_HEAD, "§6Manual Team Assign", "Opens the team GUI."));
        gui.setItem(10, createButton(Material.TNT, "§cAssign Random Teams", "Assigns all players randomly."));

        // Settings Buttons
        ItemStack autoSmeltButton = createButton(
                Material.FURNACE,
                "§7Auto Smelt: " + (gameManager.isAutoSmeltEnabled() ? "§aEnabled" : "§cDisabled"),
                "Click to toggle."
        );
        gui.setItem(18, autoSmeltButton);

        ItemStack prepTimeButton = createButton(
                Material.CLOCK,
                "§ePrep Time: §f" + gameManager.getInitialPrepTimeSeconds() + "s",
                "Adjust with the buttons below."
        );
        gui.setItem(19, prepTimeButton);

        gui.setItem(20, createButton(Material.EMERALD_BLOCK, "§a+60s"));
        gui.setItem(21, createButton(Material.LIME_STAINED_GLASS_PANE, "§a+10s"));
        gui.setItem(22, createButton(Material.RED_STAINED_GLASS_PANE, "§c-10s"));
        gui.setItem(23, createButton(Material.REDSTONE_BLOCK, "§c-60s"));

        // TPA Control
        gui.setItem(17, createButton(Material.ENDER_PEARL, "§dRefresh All TPA Cooldowns", "Resets TPA cooldowns for everyone."));


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
