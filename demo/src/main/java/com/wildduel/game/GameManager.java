package com.wildduel.game;

import com.wildduel.WildDuel;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.bukkit.ChatColor;
import java.util.concurrent.CompletableFuture;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import io.papermc.lib.PaperLib;

public class GameManager {

    private final TeamManager teamManager;
    private GameState gameState = GameState.LOBBY;
    private World lobbyWorld;
    private World gameWorld;

    private BossBar timerBar;
    private int initialPrepTimeSeconds = 900; // 15 minutes default
    private int prepTimeSeconds;
    private BukkitRunnable gameTask;
    private BukkitRunnable saturationTask;
    private BukkitRunnable distanceDisplayTask;

    private boolean autoSmeltEnabled = false;
    private boolean playerTeamSelectionEnabled = false;

    public GameManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void initializeWorlds() {
        this.lobbyWorld = Bukkit.getWorld("wildduel_world");
        this.gameWorld = Bukkit.getWorld("wildduel_game"); // Default world
        if (lobbyWorld != null) {
            lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            lobbyWorld.setTime(6000L);
            lobbyWorld.setStorm(false);
            lobbyWorld.setPVP(true);
        }
        transitionToLobby();
    }

    public void transitionToLobby() {
        gameState = GameState.LOBBY;
        applySaturationEffectPeriodically(lobbyWorld);
        for (Player player : Bukkit.getOnlinePlayers()) {
            preparePlayerForLobby(player);
        }
    }

    public void preparePlayerForLobby(Player player) {
        Location spawnPoint = lobbyWorld.getSpawnLocation().clone().add(0.5, 0, 0.5);
        player.teleport(spawnPoint);
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void applySaturationEffectPeriodically(World world) {
        if (saturationTask != null) saturationTask.cancel();
        saturationTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : world.getPlayers()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 0, false, false));
                }
            }
        };
        saturationTask.runTaskTimer(WildDuel.getInstance(), 0, 100); // 5 seconds interval
    }

    private void stopSaturationEffect() {
        if (saturationTask != null) {
            saturationTask.cancel();
        }
    }

    public void startGame() {
        if (gameState != GameState.LOBBY) {
            return;
        }
        startCountdown();
    }

    private void startCountdown() {
        gameState = GameState.COUNTDOWN;
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (countdown <= 0) {
                    this.cancel();
                    transitionToFarming();
                    return;
                }
                for (Player player : lobbyWorld.getPlayers()) {
                    player.sendTitle(ChatColor.GREEN + "게임이 곧 시작됩니다...", ChatColor.YELLOW + String.valueOf(countdown), 0, 25, 5);
                }
                countdown--;
            }
        }.runTaskTimer(WildDuel.getInstance(), 0, 20);
    }

    private void transitionToFarming() {
        WildDuel.getInstance().recreateGameWorld(() -> {
            // This code runs after the world has been recreated.
            this.gameWorld = Bukkit.getWorld("wildduel_game");
            if (this.gameWorld == null) {
                Bukkit.broadcastMessage(ChatColor.RED + "Failed to create or load the game world. Aborting game start.");
                resetGame();
                return;
            }

            gameState = GameState.FARMING;
            this.prepTimeSeconds = this.initialPrepTimeSeconds;

            gameWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
            gameWorld.setPVP(false);
            WorldBorder border = gameWorld.getWorldBorder();
            border.setCenter(gameWorld.getSpawnLocation());
            border.setSize(1000);

            applySaturationEffectPeriodically(gameWorld);

            // Use a list of players to teleport to avoid ConcurrentModificationException
            List<Player> playersToTeleport = new ArrayList<>(lobbyWorld.getPlayers());
            List<CompletableFuture<Boolean>> teleportFutures = new ArrayList<>();

            for (Player player : playersToTeleport) {
                teleportFutures.add(teleportToCenter(player, gameWorld));
                player.setGameMode(GameMode.SURVIVAL);
                player.setExp(0F);
                player.setLevel(0);
                player.getInventory().clear();
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                WildDuel.getInstance().getDefaultStartInventory().apply(player);
            }

            // Wait for all teleports to complete, then start the timer.
            CompletableFuture.allOf(teleportFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                // This code runs on the main server thread after all teleports are done.
                startTimer();
            });
        });
    }

    private CompletableFuture<Boolean> teleportToCenter(Player player, World world) {
        Location spawnLocation = world.getSpawnLocation();
        return io.papermc.lib.PaperLib.teleportAsync(player, spawnLocation);
    }

    private void startTimer() {
        timerBar = Bukkit.createBossBar("파밍 시간", BarColor.BLUE, BarStyle.SOLID);
        timerBar.setVisible(true);
        for (Player player : gameWorld.getPlayers()) {
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
                if (prepTimeSeconds == 300 || prepTimeSeconds == 60) {
                    Bukkit.broadcastMessage("§e[안내] §f파밍 시간이 " + (prepTimeSeconds / 60) + "분 남았습니다!");
                }
                prepTimeSeconds--;
                timerBar.setProgress((double) prepTimeSeconds / initialPrepTimeSeconds);
                timerBar.setTitle("남은 파밍 시간: " + formatTime(prepTimeSeconds));
            }
        };
        gameTask.runTaskTimer(WildDuel.getInstance(), 0, 20);
    }

    private void startBattle() {
        gameState = GameState.BATTLE;
        gameWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
        gameWorld.setPVP(true);

        for (Player player : gameWorld.getPlayers()) {
            player.sendMessage("§c전투가 시작되었습니다!");
        }

        stopSaturationEffect();

        WorldBorder border = gameWorld.getWorldBorder();
        border.setSize(100, 60 * 10); // Shrink over 10 minutes

        startDistanceDisplay();

        if (timerBar != null) {
            timerBar.removeAll();
            timerBar.setVisible(false);
        }
    }

    public void checkWinCondition() {
        if (gameState != GameState.BATTLE) return;

        List<TeamManager.TeamData> activeTeams = new ArrayList<>();
        for (TeamManager.TeamData teamData : teamManager.getTeams()) {
            boolean hasAliveMembers = teamData.getPlayers().stream()
                    .anyMatch(p -> p.isOnline() && p.getGameMode() == GameMode.SURVIVAL && p.getWorld().equals(gameWorld));
            if (hasAliveMembers) {
                activeTeams.add(teamData);
            }
        }

        if (activeTeams.size() <= 1) {
            endGame(activeTeams.isEmpty() ? null : activeTeams.get(0));
        }
    }

    private void endGame(TeamManager.TeamData winner) {
        gameState = GameState.ENDED;
        String winnerMessage = winner != null ? winner.getColor() + winner.getName() + " 팀 승리!" : "게임이 무승부로 종료되었습니다!";
        Bukkit.broadcastMessage("§a========================================");
        Bukkit.broadcastMessage("§e           게임 종료");
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§f승리: " + (winner != null ? winner.getColor() + winner.getName() + " 팀!" : "무승부!"));
        Bukkit.broadcastMessage("§a========================================");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(winnerMessage, "10초 후 로비로 이동합니다.", 10, 80, 20);
        }

        if (gameTask != null) gameTask.cancel();
        if (timerBar != null) timerBar.removeAll();
        if (distanceDisplayTask != null) distanceDisplayTask.cancel();

        // After a 10-second delay, teleport everyone back to the lobby and prepare them
        new BukkitRunnable() {
            @Override
            public void run() {
                transitionToLobby();
                // No need to recreate world here anymore as it's handled before farming.
            }
        }.runTaskLater(WildDuel.getInstance(), 10 * 20);
    }

    public void resetGame() {
        if (gameTask != null) gameTask.cancel();
        if (saturationTask != null) saturationTask.cancel();
        if (timerBar != null) timerBar.removeAll();
        if (distanceDisplayTask != null) distanceDisplayTask.cancel();

        gameState = GameState.LOBBY;
        autoSmeltEnabled = false;
        playerTeamSelectionEnabled = false;
        initialPrepTimeSeconds = 900;

        if (gameWorld != null) {
            gameWorld.getWorldBorder().reset();
            gameWorld.setPVP(false);
        }

        transitionToLobby();
    }

    // Getters and Setters
    public GameState getGameState() { return gameState; }
    public World getLobbyWorld() { return lobbyWorld; }
    public World getGameWorld() { return gameWorld; }
    public boolean isAutoSmeltEnabled() { return autoSmeltEnabled; }
    public void setAutoSmelt(boolean autoSmeltEnabled) { this.autoSmeltEnabled = autoSmeltEnabled; }
    public boolean isPlayerTeamSelectionEnabled() { return playerTeamSelectionEnabled; }
    public void setPlayerTeamSelectionEnabled(boolean playerTeamSelectionEnabled) { this.playerTeamSelectionEnabled = playerTeamSelectionEnabled; }
    public int getInitialPrepTimeSeconds() { return initialPrepTimeSeconds; }
    public void setPrepTime(int seconds) { this.initialPrepTimeSeconds = seconds; }

    public void addTime(int seconds) {
        if (gameState != GameState.FARMING) {
            return; // Only works during farming phase
        }
        this.prepTimeSeconds += seconds;
        if (this.prepTimeSeconds < 0) {
            this.prepTimeSeconds = 0;
        }
        if (this.prepTimeSeconds > this.initialPrepTimeSeconds) {
            this.prepTimeSeconds = this.initialPrepTimeSeconds;
        }
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void startDistanceDisplay() {
        distanceDisplayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.BATTLE) {
                    this.cancel();
                    return;
                }
                WorldBorder border = gameWorld.getWorldBorder();
                Location center = border.getCenter();
                for (Player player : gameWorld.getPlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "중앙 좌표: " + ChatColor.WHITE + String.format("X: %.0f, Z: %.0f", center.getX(), center.getZ())));
                }
            }
        };
        distanceDisplayTask.runTaskTimer(WildDuel.getInstance(), 0, 20); // Every second
    }

    public void handlePlayerQuit(Player player) {
        // The TeamManager handles removing the player from their team.
        // We just need to check if the game should end.
        if (gameState == GameState.BATTLE || gameState == GameState.FARMING) {
            // Run win condition check on the next tick to ensure player is fully gone
            Bukkit.getScheduler().runTask(WildDuel.getInstance(), this::checkWinCondition);
        }
    }
}