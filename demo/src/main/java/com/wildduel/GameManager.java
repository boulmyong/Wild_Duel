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
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

public class GameManager {

    private GameState gameState = GameState.LOBBY;
    private Location lobbySpawn;
    private Location worldSpawn;
    private BossBar timerBar;
    private int prepTimeSeconds = 900; // 15 minutes
    private BukkitRunnable gameTask;

    public void setLobby(Location location) {
        if (gameState != GameState.LOBBY) {
            // Handle error: Game already in progress
            return;
        }
        this.lobbySpawn = location;
        World world = location.getWorld();
        if (world != null) {
            world.setSpawnLocation(lobbySpawn);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTask(WildDuel.getInstance(), () -> {
                player.teleport(lobbySpawn);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false));
                WildDuel.getInstance().getLogger().info("Teleported " + player.getName() + " to lobby.");
            });
        }
        gameState = GameState.PREPARING;
    }

    public void setWorldSpawn(Location location) {
        this.worldSpawn = location;
    }

    public void startGame() {
        if (gameState != GameState.PREPARING || worldSpawn == null) {
            // Handle error: Game not ready or world spawn not set
            return;
        }

        World world = worldSpawn.getWorld();
        world.setSpawnLocation(worldSpawn);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setPVP(false);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(worldSpawn);
        border.setSize(2000); // 1000 radius

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(worldSpawn);
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.STONE_AXE));
            player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            player.setGameMode(GameMode.SURVIVAL);
        }

        setupTeam();
        startTimer();
        gameState = GameState.FARMING;
    }

    public void setTime(int seconds) {
        if (gameState != GameState.FARMING) {
            // Handle error: Game not in farming phase
            return;
        }
        this.prepTimeSeconds = seconds;
    }

    private void setupTeam() {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getTeam("WildDuel");
        if (team == null) {
            team = scoreboard.registerNewTeam("WildDuel");
        }
        team.setAllowFriendlyFire(false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            team.addEntry(player.getName());
        }
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
                prepTimeSeconds--;
                timerBar.setProgress((double) prepTimeSeconds / 900);
                timerBar.setTitle("Farming Time: " + formatTime(prepTimeSeconds));
            }
        };
        gameTask.runTaskTimer(WildDuel.getInstance(), 0, 20);
    }

    private void startBattle() {
        gameState = GameState.BATTLE;
        World world = worldSpawn.getWorld();
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setPVP(true);

        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getTeam("WildDuel");
        if (team != null) {
            team.unregister();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("Battle has begun!");
        }

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
}
