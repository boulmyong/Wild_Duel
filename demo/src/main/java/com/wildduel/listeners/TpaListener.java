package com.wildduel.listeners;

import com.wildduel.game.TpaManager;
import com.wildduel.game.TpaRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TpaListener implements Listener {

    private final TpaManager tpaManager;

    public TpaListener(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // This logic is handled within the TpaManager's BukkitRunnable
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TpaRequest request = tpaManager.getPendingRequest(player.getUniqueId());
        if (request != null) {
            Player target = Bukkit.getPlayer(request.getTarget());
            tpaManager.cancelTpa(player, target, false);
        }
    }
}
