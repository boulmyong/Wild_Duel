package com.wildduel.gui;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class AdminGUI {

    private final WildDuel plugin;
    private final GameManager gameManager;
    private final Set<UUID> playersConfirmingReset;

    // Titles for different GUIs to be checked in the listener
    public static final String MAIN_TITLE = "§4[관리자 패널] 와일드 듀얼";
    public static final String GAME_SETTINGS_TITLE = "§4[설정] 게임 규칙";
    public static final String TIME_SETTINGS_TITLE = "§4[설정] 시간";
    public static final String WORLD_SETTINGS_TITLE = "§4[설정] 월드 & 서버";


    public AdminGUI(WildDuel plugin, GameManager gameManager, Set<UUID> playersConfirmingReset) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.playersConfirmingReset = playersConfirmingReset;
    }

    public void open(Player admin) {
        Inventory gui = Bukkit.createInventory(null, 27, MAIN_TITLE);

        // Row 1: Info
        createDisplay(gui, 0, Material.BEACON, "§b게임 상태", "§f" + gameManager.getGameState().toString());
        createDisplay(gui, 1, Material.DIAMOND_SWORD, "§b게임 모드", "§f" + gameManager.getGameMode().toString());
        int playerCount = 0;
        if (gameManager.getGameState() == GameState.LOBBY && gameManager.getLobbyWorld() != null) {
            playerCount = gameManager.getLobbyWorld().getPlayers().size();
        } else if (gameManager.getGameWorld() != null) {
            playerCount = gameManager.getGameWorld().getPlayers().size();
        } else {
            playerCount = Bukkit.getOnlinePlayers().size();
        }
        createDisplay(gui, 2, Material.PLAYER_HEAD, "§b플레이어 수", "§f" + playerCount + "명");
        World gameWorld = gameManager.getGameWorld();
        createDisplay(gui, 3, Material.GRASS_BLOCK, "§b게임 월드 상태", (gameWorld != null ? "§a로딩됨" : "§c준비 안됨"));
        createDisplay(gui, 4, Material.BOOK, "§b플러그인 정보", "§fWildDuel v" + plugin.getDescription().getVersion());

        // Row 2: Core Actions
        gui.setItem(9, createButton(Material.LIME_WOOL, "§a게임 시작"));
        gui.setItem(10, createButton(Material.ANVIL, "§6수동 팀 배정"));
        
        // Conditional Reset Button
        if (playersConfirmingReset != null && playersConfirmingReset.contains(admin.getUniqueId())) {
            gui.setItem(11, createButton(Material.REDSTONE_BLOCK, "§c§l초기화 확인", "§e클릭하면 모든 게임 정보가 초기화됩니다."));
        } else {
            gui.setItem(11, createButton(Material.TNT, "§c게임 초기화", "§e주의: §f두 번 클릭해야 실행됩니다."));
        }

        // Row 3: Sub-Menus
        gui.setItem(18, createButton(Material.CRAFTING_TABLE, "§e게임 규칙 설정"));
        gui.setItem(19, createButton(Material.CLOCK, "§e시간 설정"));
        gui.setItem(20, createButton(Material.COMMAND_BLOCK, "§e월드 & 서버 설정"));
        gui.setItem(26, createButton(Material.BARRIER, "§c닫기"));


        admin.openInventory(gui);
    }

    private void createDisplay(Inventory gui, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        gui.setItem(slot, item);
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
