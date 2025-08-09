package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.gui.StartItemGUI;
import com.wildduel.util.PlayerInventorySnapshot;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class StartItemGUIListener implements Listener {

    private final WildDuel plugin;

    public StartItemGUIListener(WildDuel plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.BLUE + "시작 아이템 설정")) {
            Player player = (Player) event.getWhoClicked();
            Inventory clickedInventory = event.getClickedInventory();
            ItemStack clickedItem = event.getCurrentItem();

            // GUI 내부 클릭
            if (event.getRawSlot() < 45) {
                // 저장 버튼 또는 회색 유리판 클릭
                if (event.getRawSlot() >= 41) {
                    event.setCancelled(true);
                    if (event.getRawSlot() == 44 && clickedItem != null && clickedItem.getType() == Material.LIME_WOOL) {
                        // 저장 로직
                        Inventory gui = event.getInventory();
                        ItemStack[] contents = new ItemStack[36];
                        ItemStack[] armor = new ItemStack[4];
                        ItemStack offHand = null;

                        for (int i = 0; i < 36; i++) {
                            contents[i] = gui.getItem(i);
                        }
                        armor[0] = gui.getItem(36); // Boots
                        armor[1] = gui.getItem(37); // Leggings
                        armor[2] = gui.getItem(38); // Chestplate
                        armor[3] = gui.getItem(39); // Helmet
                        offHand = gui.getItem(40);

                        PlayerInventorySnapshot newSnapshot = new PlayerInventorySnapshot(contents, armor, offHand);
                        plugin.setDefaultStartInventory(newSnapshot);
                        player.sendMessage(ChatColor.GREEN + "시작 아이템 설정이 저장되었습니다!");
                        player.closeInventory();
                    }
                }
                // 아이템 슬롯 (0-40)은 자유롭게 상호작용
            }
            // 플레이어 인벤토리 클릭은 기본 동작을 따름 (event.setCancelled(false))
        }
    }
}
