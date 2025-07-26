package com.wildduel.game;

import com.wildduel.WildDuel;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class GameManager {

    private final TeamManager teamManager;
    private volatile GameState gameState = GameState.LOBBY;
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

    public synchronized void setGameState(GameState newState) {
        this.gameState = newState;
    }

    public void initializeWorlds() {
        this.lobbyWorld = Bukkit.getWorld("wildduel_world");
        if (this.lobbyWorld == null) {
            WildDuel.getInstance().getLogger().severe("로비 월드(wildduel_world)를 찾을 수 없습니다! 플러그인이 정상적으로 동작하지 않을 수 있습니다.");
            return; // 초기화 중단
        }

        this.gameWorld = Bukkit.getWorld("wildduel_game"); // Default world
        
        lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        lobbyWorld.setTime(6000L);
        lobbyWorld.setStorm(false);
        lobbyWorld.setPVP(true);
        
        transitionToLobby();
    }

    public void transitionToLobby() {
        setGameState(GameState.LOBBY);
        if (lobbyWorld == null) return;
        applySaturationEffectPeriodically(lobbyWorld);
        for (Player player : Bukkit.getOnlinePlayers()) {
            preparePlayerForLobby(player);
        }
    }

    public void preparePlayerForLobby(Player player) {
        if (lobbyWorld == null) return;
        Location spawnPoint = lobbyWorld.getSpawnLocation().clone().add(0.5, 0, 0.5);
        io.papermc.lib.PaperLib.teleportAsync(player, spawnPoint);
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void applySaturationEffectPeriodically(World world) {
        if (world == null) return;
        if (saturationTask != null) saturationTask.cancel();
        saturationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (world.getPlayers().isEmpty()) return;
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

    public synchronized boolean startGame() {
        if (getGameState() != GameState.LOBBY) {
            return false; // Not in lobby state
        }
        if (lobbyWorld == null || lobbyWorld.getPlayers().size() < 2) {
            return false; // Not enough players
        }
        startCountdown();
        return true;
    }

    private void startCountdown() {
        setGameState(GameState.COUNTDOWN);
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (lobbyWorld == null) {
                    this.cancel();
                    return;
                }
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
        recreateGameWorldAsync().thenAccept(newGameWorld -> {
            // This code runs on the main server thread after the world is ready.
            this.gameWorld = newGameWorld;

            setGameState(GameState.FARMING);
            this.prepTimeSeconds = this.initialPrepTimeSeconds;

            gameWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
            gameWorld.setPVP(false);
            WorldBorder border = gameWorld.getWorldBorder();
            border.setCenter(gameWorld.getSpawnLocation());
            border.setSize(1000);

            applySaturationEffectPeriodically(newGameWorld); // 수정된 부분

            List<Player> playersToTeleport = new ArrayList<>(lobbyWorld.getPlayers());
            List<CompletableFuture<Boolean>> teleportFutures = new ArrayList<>();

            for (Player player : playersToTeleport) {
                teleportFutures.add(teleportToCenter(player, newGameWorld));
                player.setGameMode(GameMode.SURVIVAL);
                player.setExp(0F);
                player.setLevel(0);
                player.getInventory().clear();
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                WildDuel.getInstance().getDefaultStartInventory().apply(player);
            }

            CompletableFuture.allOf(teleportFutures.toArray(new CompletableFuture[0])).thenRun(this::startTimer);

        }).exceptionally(ex -> {
            Bukkit.broadcastMessage(ChatColor.RED + "게임 월드 생성에 실패했습니다. 게임 시작이 중단됩니다.");
            WildDuel.getInstance().getLogger().log(Level.SEVERE, "게임 월드 재생성 중 심각한 오류 발생", ex);
            resetGame();
            return null; // Handle the exception
        });
    }

    private CompletableFuture<World> recreateGameWorldAsync() {
        return CompletableFuture.supplyAsync(() -> {
            World world = Bukkit.getWorld("wildduel_game");
            if (world != null) {
                WildDuel.getInstance().getLogger().info("기존 게임 월드를 언로드합니다: " + world.getName());
                if (!Bukkit.unloadWorld(world, false)) {
                    throw new RuntimeException("게임 월드 언로드에 실패했습니다: " + world.getName());
                }
                WildDuel.getInstance().getLogger().info("게임 월드 언로드 완료.");
                return world.getWorldFolder();
            }
            return new File(Bukkit.getWorldContainer(), "wildduel_game");
        }, runnable -> Bukkit.getScheduler().runTask(WildDuel.getInstance(), runnable))
        .thenComposeAsync(worldFolder -> CompletableFuture.runAsync(() -> {
            if (worldFolder.exists()) {
                WildDuel.getInstance().getLogger().info("기존 월드 폴더를 삭제합니다: " + worldFolder.getName());
                if (!deleteWorld(worldFolder)) {
                    throw new RuntimeException("월드 폴더 삭제에 실패했습니다: " + worldFolder.getName());
                }
                WildDuel.getInstance().getLogger().info("월드 폴더 삭제 완료.");
            }
        }, runnable -> WildDuel.getInstance().getServer().getAsyncScheduler().runNow(WildDuel.getInstance(), task -> runnable.run())))
        .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
            WildDuel.getInstance().getLogger().info("새로운 게임 월드를 생성합니다...");
            WorldCreator wc = new WorldCreator("wildduel_game");
            wc.seed(new Random().nextLong());
            World newWorld = wc.createWorld();
            if (newWorld == null) {
                throw new RuntimeException("새로운 게임 월드 생성에 실패했습니다.");
            }
            WildDuel.getInstance().getLogger().info("새로운 게임 월드 생성 완료.");
            return newWorld;
        }, runnable -> Bukkit.getScheduler().runTask(WildDuel.getInstance(), runnable)));
    }

    private boolean deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorld(file);
                    } else {
                        if (!file.delete()) {
                            return false;
                        }
                    }
                }
            }
        }
        return path.delete();
    }

    private CompletableFuture<Boolean> teleportToCenter(Player player, World world) {
        Location spawnLocation = world.getSpawnLocation();
        return io.papermc.lib.PaperLib.teleportAsync(player, spawnLocation);
    }

    private void startTimer() {
        timerBar = Bukkit.createBossBar("파밍 시간", BarColor.BLUE, BarStyle.SOLID);
        timerBar.setVisible(true);
        if (gameWorld == null) return;
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
        setGameState(GameState.BATTLE);
        if (gameWorld == null) return;
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

    public synchronized void checkWinCondition() {
        if (getGameState() != GameState.BATTLE || gameWorld == null) return;

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

    private synchronized void endGame(TeamManager.TeamData winner) {
        if (getGameState() == GameState.ENDED) return;
        setGameState(GameState.ENDED);
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

        new BukkitRunnable() {
            @Override
            public void run() {
                transitionToLobby();
            }
        }.runTaskLater(WildDuel.getInstance(), 10 * 20);
    }

    public synchronized void resetGame() {
        if (gameTask != null) gameTask.cancel();
        if (saturationTask != null) saturationTask.cancel();
        if (timerBar != null) timerBar.removeAll();
        if (distanceDisplayTask != null) distanceDisplayTask.cancel();

        setGameState(GameState.LOBBY);
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
        if (getGameState() != GameState.FARMING) {
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
                if (getGameState() != GameState.BATTLE) {
                    this.cancel();
                    return;
                }
                if (gameWorld == null) return;
                WorldBorder border = gameWorld.getWorldBorder();
                Location center = border.getCenter();
                for (Player player : gameWorld.getPlayers()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "중앙 좌표: " + ChatColor.WHITE + String.format("X: %.0f, Z: %.0f", center.getX(), center.getZ())));
                }
            }
        };
        distanceDisplayTask.runTaskTimer(WildDuel.getInstance(), 0, 20); // Every second
    }

    public synchronized void handlePlayerQuit(Player player) {
        if (getGameState() == GameState.BATTLE || getGameState() == GameState.FARMING) {
            Bukkit.getScheduler().runTask(WildDuel.getInstance(), this::checkWinCondition);
        }
    }
}