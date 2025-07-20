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

    private final TeamManager teamManager;
    private GameState gameState = GameState.LOBBY;
    private Location duelStartLocation;
    private BossBar timerBar;
    private int initialPrepTimeSeconds = 900; // 15 minutes default
    private int prepTimeSeconds;
    private BukkitRunnable gameTask;

    public GameManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void setWorldSpawn(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.setSpawnLocation(location);
        }
        if (gameState == GameState.LOBBY) {
            gameState = GameState.PREPARING;
            applySaturationEffect();
        }
    }

    private void applySaturationEffect() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false));
        }
    }

    public void setDuelStartLocation(Location location) {
        this.duelStartLocation = location;
        if (gameState == GameState.LOBBY) {
            gameState = GameState.PREPARING;
        }
    }

    public void startGame() {
        if (gameState != GameState.PREPARING || duelStartLocation == null) {
            // Handle error: Game not ready or duel start location not set
            return;
        }

        this.prepTimeSeconds = this.initialPrepTimeSeconds;
        World world = duelStartLocation.getWorld();
        world.setSpawnLocation(duelStartLocation);
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

        applySaturationEffect();

        setupTeam();
        startTimer();
        gameState = GameState.FARMING;
    }

    public void setPrepTime(int seconds) {
        if (gameState != GameState.PREPARING) {
            // Handle error: Can only set prep time before the game starts
            return;
        }
        this.initialPrepTimeSeconds = seconds;
    }

    public void setTime(int seconds) {
        if (gameState != GameState.FARMING) {
            // Handle error: Game not in farming phase
            return;
        }
        if (seconds > this.initialPrepTimeSeconds) {
            seconds = this.initialPrepTimeSeconds;
        }
        this.prepTimeSeconds = seconds;
    }

    private void setupTeam() {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        for (TeamManager.TeamData teamData : teamManager.getTeams().values()) {
            Team team = scoreboard.getTeam(teamData.getName());
            if (team == null) {
                team = scoreboard.registerNewTeam(teamData.getName());
            }
            team.setAllowFriendlyFire(false);
            team.setColor(teamData.getColor());
            for (Player player : teamData.getPlayers()) {
                team.addEntry(player.getName());
            }
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
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setPVP(true);

        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        for (TeamManager.TeamData teamData : teamManager.getTeams().values()) {
            Team team = scoreboard.getTeam(teamData.getName());
            if (team != null) {
                team.unregister();
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("Battle has begun!");
            player.removePotionEffect(PotionEffectType.SATURATION);
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
