package com.wildduel;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class GameManager {

    private final TeamManager teamManager;
    private GameState gameState = GameState.LOBBY;
    private Location duelStartLocation;
    private BossBar timerBar;
    private int initialPrepTimeSeconds = 900; // 15 minutes default
    private int prepTimeSeconds;
    private BukkitRunnable gameTask;
    private BukkitRunnable saturationTask;
    private boolean autoSmeltEnabled = true; // Default to on
    private boolean playerTeamSelectionEnabled = false; // Default to off

    public GameManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void setWorldSpawn(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.setSpawnLocation(location);
        }
        transitionToPreparing();
    }

    private void applySaturationEffectPeriodically() {
        saturationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0, false, false));
                }
            }
        };
        saturationTask.runTaskTimer(WildDuel.getInstance(), 0, 100); // 5초마다 실행
    }

    private void stopSaturationEffect() {
        if (saturationTask != null) {
            saturationTask.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.SATURATION);
        }
    }

    public void setDuelStartLocation(Location location) {
        this.duelStartLocation = location;
        transitionToPreparing();
    }

    private void transitionToPreparing() {
        if (gameState == GameState.LOBBY) {
            gameState = GameState.PREPARING;
            applySaturationEffectPeriodically();
        }
    }

    public void startGame() {
        if (gameState != GameState.PREPARING || duelStartLocation == null) {
            return;
        }

        this.prepTimeSeconds = this.initialPrepTimeSeconds;
        World world = duelStartLocation.getWorld();
        Objects.requireNonNull(world).setSpawnLocation(duelStartLocation);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setPVP(false);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(duelStartLocation);
        border.setSize(1000); // 500 radius

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(duelStartLocation);
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            player.setGameMode(GameMode.SURVIVAL);
        }

        startTimer();
        gameState = GameState.FARMING;
    }

    public void setPrepTime(int seconds) {
        if (gameState == GameState.FARMING || gameState == GameState.BATTLE) {
            return;
        }
        if (seconds < 0) seconds = 0;
        this.initialPrepTimeSeconds = seconds;
    }

    public void setTime(int seconds) {
        if (gameState != GameState.FARMING) {
            return;
        }
        if (seconds > this.initialPrepTimeSeconds) {
            seconds = this.initialPrepTimeSeconds;
        }
        this.prepTimeSeconds = seconds;
    }

    private void startTimer() {
        timerBar = Bukkit.createBossBar("Time Left", BarColor.BLUE, BarStyle.SOLID);
        timerBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            timerBar.addPlayer(player);
        }

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (prepTimeSeconds <= 0) {
                    startBattle();
                    this.cancel();
                    return;
                }

                if (prepTimeSeconds == 300) {
                    Bukkit.broadcastMessage("§e[Notice] §fFarming time ends in 5 minutes!");
                } else if (prepTimeSeconds == 60) {
                    Bukkit.broadcastMessage("§e[Notice] §fFarming time ends in 1 minute!");
                }

                prepTimeSeconds--;
                timerBar.setProgress((double) prepTimeSeconds / initialPrepTimeSeconds);
                timerBar.setTitle("Farming Time: " + formatTime(prepTimeSeconds));
            }
        };
        gameTask.runTaskTimer(WildDuel.getInstance(), 0, 20);
    }

    private void startBattle() {
        gameState = GameState.BATTLE;
        if (duelStartLocation == null) return;
        World world = duelStartLocation.getWorld();
        Objects.requireNonNull(world).setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setPVP(true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("Battle has begun!");
        }

        stopSaturationEffect();

        WorldBorder border = world.getWorldBorder();
        border.setSize(100, 60 * 10); // Shrink to 50 radius over 10 minutes

        if (timerBar != null) {
            timerBar.removeAll();
            timerBar.setVisible(false);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public GameState getGameState() {
        return gameState;
    }

    public void handlePlayerDeath(Player player) {
        if (gameState == GameState.FARMING) {
            // Respawn logic during farming phase
        } else if (gameState == GameState.BATTLE) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void setAutoSmelt(boolean enabled) {
        if (gameState == GameState.FARMING || gameState == GameState.BATTLE) {
            return; // Can't change during game
        }
        this.autoSmeltEnabled = enabled;
    }

    public boolean isAutoSmeltEnabled() {
        return this.autoSmeltEnabled;
    }

    public int getInitialPrepTimeSeconds() {
        return initialPrepTimeSeconds;
    }

    public void setPlayerTeamSelectionEnabled(boolean enabled) {
        this.playerTeamSelectionEnabled = enabled;
    }

    public boolean isPlayerTeamSelectionEnabled() {
        return this.playerTeamSelectionEnabled;
    }

    public void checkWinCondition() {
        if (gameState != GameState.BATTLE) {
            return;
        }

        TeamManager.TeamData winner = null;
        int teamsWithPlayers = 0;

        for (TeamManager.TeamData teamData : teamManager.getTeams()) {
            long aliveCount = teamData.getPlayers().stream()
                    .filter(p -> p.isOnline() && p.getGameMode() == GameMode.SURVIVAL)
                    .count();

            if (aliveCount > 0) {
                teamsWithPlayers++;
                winner = teamData;
            }
        }

        if (teamsWithPlayers <= 1) {
            endGame(winner);
        }
    }

    private void endGame(TeamManager.TeamData winner) {
        if (winner != null) {
            Bukkit.broadcastMessage("§a========================================");
            Bukkit.broadcastMessage("§e           GAME OVER");
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§fWinner: " + winner.getColor() + winner.getName() + " Team!");
            Bukkit.broadcastMessage("§a========================================");

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(winner.getColor() + winner.getName() + " Team Wins!", "Congratulations!", 10, 70, 20);
            }
        } else {
            Bukkit.broadcastMessage("§a========================================");
            Bukkit.broadcastMessage("§e           GAME OVER");
            Bukkit.broadcastMessage(" ");
            Bukkit.broadcastMessage("§fThe game ended in a draw!");
            Bukkit.broadcastMessage("§a========================================");
        }

        // Reset game state and other variables
        gameState = GameState.LOBBY;
        if (gameTask != null) gameTask.cancel();
        if (timerBar != null) timerBar.removeAll();
        teamManager.leaveAllTeams();
        // Potentially teleport all players to spawn after a delay
    }
}
