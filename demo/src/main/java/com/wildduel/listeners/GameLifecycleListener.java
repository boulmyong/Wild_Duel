package com.wildduel.listeners;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class GameLifecycleListener implements Listener {

    private final GameManager gameManager;

    public GameLifecycleListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.BATTLE) {
            // 전투 중에 죽으면 즉시 관전 모드로 설정합니다.
            player.setGameMode(GameMode.SPECTATOR);
            // 잠시 후 승리 조건을 확인합니다.
            Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), gameManager::checkWinCondition, 20L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameState gameState = gameManager.getGameState();

        if (gameState == GameState.BATTLE || gameState == GameState.ENDED) {
            // 전투 중이거나 게임이 끝났으면 로비에서 리스폰합니다.
            event.setRespawnLocation(gameManager.getLobbyWorld().getSpawnLocation());
            Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR); // 관전 모드 재확인
                }
            }, 1L);
        } else if (gameState == GameState.FARMING) {
            // 파밍 중에는 게임 월드에서 리스폰합니다.
            event.setRespawnLocation(gameManager.getGameWorld().getSpawnLocation());
        } else {
            // 그 외 상태에서는 로비에서 리스폰합니다.
            event.setRespawnLocation(gameManager.getLobbyWorld().getSpawnLocation());
            Bukkit.getScheduler().runTaskLater(WildDuel.getInstance(), () -> {
                if (player.isOnline()) {
                    gameManager.preparePlayerForLobby(player);
                }
            }, 1L);
        }
    }
}
