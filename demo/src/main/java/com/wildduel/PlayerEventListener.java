package com.wildduel;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerEventListener implements Listener {

    private final GameManager gameManager;

    public PlayerEventListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        gameManager.handlePlayerDeath(event.getEntity());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.PREPARING || gameState == GameState.FARMING) {
<<<<<<< HEAD
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0, false, false));
=======
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false));
>>>>>>> 430f02a7c8cba737d27e2010c7875510514866e6
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.PREPARING || gameState == GameState.FARMING) {
<<<<<<< HEAD
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0, false, false));
=======
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false));
>>>>>>> 430f02a7c8cba737d27e2010c7875510514866e6
        }
    }
}
