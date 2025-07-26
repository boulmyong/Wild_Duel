package com.wildduel.listeners;

import com.wildduel.game.TpaManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TpaListener implements Listener {

    public TpaListener(TpaManager tpaManager) {
        // Constructor can be kept for future TPA-specific events if needed
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // The teleport countdown logic, which checks for movement, is self-contained
        // in the TpaManager's BukkitRunnable, so no code is needed here.
    }

    // PlayerQuitEvent is now handled centrally in PlayerEventListener
}
