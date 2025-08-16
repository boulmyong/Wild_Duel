package com.wildduel.gui;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameMode;
import com.wildduel.game.GameState;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.game.TeamAdminManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AdminGUI {

    private final WildDuel plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TpaManager tpaManager;
    private final TeamAdminManager teamAdminManager;

    public AdminGUI(WildDuel plugin, GameManager gameManager, TeamManager teamManager, TpaManager tpaManager, TeamAdminManager teamAdminManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.tpaManager = tpaManager;
        this.teamAdminManager = teamAdminManager;
    }

    public void open(Player admin) {
        Inventory gui = Bukkit.createInventory(null, 54, "§4[관리자 패널] 와일드 듀얼");

        // --- Info Panel ---
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
        createDisplay(gui, 4, Material.GRASS_BLOCK, "§b게임 월드 상태", (gameWorld != null ? "§a로딩됨" : "§c준비 안됨"));
        createDisplay(gui, 8, Material.BOOK, "§b플러그인 정보", "§fWildDuel v" + plugin.getDescription().getVersion());

        // --- Game Control ---
        gui.setItem(10, createButton(Material.LIME_WOOL, "§a게임 시작", "10초 카운트다운을 시작합니다."));
        gui.setItem(11, createButton(Material.TNT, "§c게임 초기화", "§e주의: §f게임 진행상황을", "§f모두 초기화하고 로비로 돌아갑니다.", "§7(월드는 삭제되지 않습니다)"));
        gui.setItem(13, createButton(Material.COMMAND_BLOCK, "§c월드 재생성", "§e주의: §f게임 월드를 삭제하고", "§f새로 생성합니다. 모든 플레이어가", "§f추방되니 미리 공지해주세요."));
        gui.setItem(15, createButton(Material.ENDER_PEARL, "§d로비로 텔레포트", "로비 월드로 이동합니다."));
        gui.setItem(16, createButton(Material.COMPASS, "§d게임 월드로 텔레포트", "게임 월드로 이동합니다."));

        // --- Game Settings ---
        String gameModeName = gameManager.getGameMode() == GameMode.TEAM ? "§c팀전" : "§9개인전";
        gui.setItem(20, createButton(Material.RED_BANNER, "§b게임 모드 변경", "§7현재: " + gameModeName, "§e클릭하여 모드를 전환합니다."));
        gui.setItem(21, createButton(Material.FURNACE, "§b자동 제련", "§7현재: " + (gameManager.isAutoSmeltEnabled() ? "§a켜짐" : "§c꺼짐"), "§e클릭하여 상태를 변경합니다."));
        gui.setItem(22, createButton(Material.WRITABLE_BOOK, "§b팀 자율 선택", "§7현재: " + (gameManager.isPlayerTeamSelectionEnabled() ? "§a허용" : "§c차단"), "§e클릭하여 상태를 변경합니다."));
        gui.setItem(24, createButton(Material.CHEST, "§b시작 아이템 설정", "기본 지급 아이템을 설정하는", "GUI를 엽니다."));

        // --- Player & Team Management ---
        gui.setItem(30, createButton(Material.ANVIL, "§6수동 팀 배정", "GUI를 통해 특정 플레이어를", "원하는 팀으로 강제 이동시킵니다."));
        gui.setItem(31, createButton(Material.NETHER_STAR, "§6랜덤 팀 배정", "모든 플레이어를 무작위로", "팀에 배정합니다."));
        gui.setItem(32, createButton(Material.BUCKET, "§6TPA 쿨타임 초기화", "모든 플레이어의 TPA 관련", "명령어 쿨타임을 즉시 초기화합니다."));

        // --- Time Management ---
        gui.setItem(38, createButton(Material.REDSTONE_BLOCK, "§c-60초"));
        gui.setItem(39, createButton(Material.RED_STAINED_GLASS_PANE, "§c-10초"));
        gui.setItem(40, createButton(Material.CLOCK, "§e총 파밍 시간: §f" + gameManager.getInitialPrepTimeSeconds() + "초", "§7게임 시작 전 시간을 설정합니다.", "§b(라이브 시간 조절: §a좌클릭 +60초§7, §c우클릭 -60초§b)"));
        gui.setItem(41, createButton(Material.LIME_STAINED_GLASS_PANE, "§a+10초"));
        gui.setItem(42, createButton(Material.EMERALD_BLOCK, "§a+60초"));

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
