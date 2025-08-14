package com.wildduel.game;

import com.wildduel.WildDuel;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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

    private final WildDuel plugin;
    private final TeamManager teamManager;
    private volatile GameState gameState = GameState.LOBBY;
    private volatile boolean isWorldRegenerating = false;
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
    private GameMode gameMode = GameMode.SOLO;

    public GameManager(WildDuel plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public synchronized void setGameState(GameState newState) {
        this.gameState = newState;
    }

    public void initializeWorlds() {
        this.lobbyWorld = Bukkit.getWorld("wildduel_world");
        if (this.lobbyWorld == null) {
            WildDuel.getInstance().getLogger().severe(plugin.getMessage("error.lobby-world-not-found"));
            return;
        }
        
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            preparePlayerForLobby(player);
        }
        
        applySaturationEffectPeriodically(lobbyWorld);
    }

    public void preparePlayerForLobby(Player player) {
        if (lobbyWorld == null) return;
        Location spawnPoint = lobbyWorld.getSpawnLocation().clone().add(0.5, 0, 0.5);
        io.papermc.lib.PaperLib.teleportAsync(player, spawnPoint);
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
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

    public void askAdminToStart(Player admin) {
        TextComponent message = new TextComponent(plugin.getMessage("gui.admin.start.title"));

        TextComponent regenLink = new TextComponent(plugin.getMessage("gui.admin.start.regen-button"));
        regenLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wd regen_world_confirm"));
        regenLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getMessage("gui.admin.start.regen-button-lore"))));

        TextComponent startLink = new TextComponent(plugin.getMessage("gui.admin.start.start-button"));
        startLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wd start_game_final"));
        startLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getMessage("gui.admin.start.start-button-lore"))));

        message.addExtra(regenLink);
        message.addExtra(new TextComponent(plugin.getMessage("gui.admin.start.separator")));
        message.addExtra(startLink);

        admin.spigot().sendMessage(message);
    }

    public void executeWorldRegeneration() {
        if (isWorldRegenerating) {
            Bukkit.broadcastMessage(plugin.getMessage("error.world-already-generating"));
            return;
        }
        isWorldRegenerating = true;
        Bukkit.setWhitelist(true);
        WildDuel.getInstance().getLogger().info(plugin.getMessage("info.whitelist-enabled-for-regen"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(plugin.getMessage("kick.reason.world-regen"));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                WildDuel.getInstance().getLogger().info(plugin.getMessage("info.starting-world-regen-after-kick"));
                recreateGameWorldAsync().whenComplete((newWorld, ex) -> {
                    isWorldRegenerating = false;
                    Bukkit.setWhitelist(false);
                    WildDuel.getInstance().getLogger().info(plugin.getMessage("info.whitelist-disabled-after-regen"));
                    if (ex != null) {
                        WildDuel.getInstance().getLogger().log(Level.SEVERE, plugin.getMessage("error.world-gen-failed"), ex);
                        GameManager.this.gameWorld = null;
                    } else {
                        GameManager.this.gameWorld = newWorld;
                        WildDuel.getInstance().getLogger().info(plugin.getMessage("info.world-gen-complete"));
                    }
                });
            }
        }.runTaskLater(WildDuel.getInstance(), 60L); // 3-second delay
    }

    public void executeGameStart() {
        if (getGameState() != GameState.LOBBY) {
            Bukkit.broadcastMessage(plugin.getMessage("error.must-be-lobby"));
            return;
        }
        if (isWorldRegenerating) {
            Bukkit.broadcastMessage(plugin.getMessage("error.world-already-generating"));
            return;
        }
        if (gameWorld == null) {
            Bukkit.broadcastMessage(plugin.getMessage("error.world-not-ready"));
            return;
        }

        List<Player> participants = new ArrayList<>(lobbyWorld.getPlayers());
        if (participants.size() < 2) {
            Bukkit.broadcastMessage(plugin.getMessage("error.not-enough-players"));
            return;
        }

        // Determine game mode and validate teams
        if (this.gameMode == GameMode.TEAM) {
            for (Player player : participants) {
                if (teamManager.getPlayerTeam(player) == null) {
                    Bukkit.broadcastMessage(plugin.getMessage("error.player-not-in-team"));
                    return;
                }
            }
        }

        startCountdown();
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
                    player.sendTitle(plugin.getMessage("title.game-starting"), ChatColor.YELLOW + String.valueOf(countdown), 0, 25, 5);
                }
                countdown--;
            }
        }.runTaskTimer(WildDuel.getInstance(), 0, 20);
    }

    private void transitionToFarming() {
        setGameState(GameState.FARMING);
        this.prepTimeSeconds = this.initialPrepTimeSeconds;

        gameWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
        gameWorld.setPVP(false);
        WorldBorder border = gameWorld.getWorldBorder();
        border.setCenter(gameWorld.getSpawnLocation());
        border.setSize(1000);

        applySaturationEffectPeriodically(gameWorld);

        List<Player> playersToTeleport = new ArrayList<>(lobbyWorld.getPlayers());
        List<CompletableFuture<Boolean>> teleportFutures = new ArrayList<>();

        for (Player player : playersToTeleport) {
            teleportFutures.add(teleportToCenter(player, gameWorld));
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.setExp(0F);
            player.setLevel(0);
            player.getInventory().clear();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            WildDuel.getInstance().getDefaultStartInventory().apply(player);
        }

        CompletableFuture.allOf(teleportFutures.toArray(new CompletableFuture[0])).thenRun(this::startTimer);
    }

    

    private CompletableFuture<World> recreateGameWorldAsync() {
        // Phase 1: Unload world (on main thread)
        return CompletableFuture.supplyAsync(() -> {
            World world = Bukkit.getWorld("wildduel_game");
            if (world != null) {
                WildDuel.getInstance().getLogger().info(plugin.getMessage("info.unloading-world", "%world%", world.getName()));
                if (!Bukkit.unloadWorld(world, false)) {
                    throw new RuntimeException(plugin.getMessage("error.world-unload-failed", "%world%", world.getName()));
                }
                WildDuel.getInstance().getLogger().info(plugin.getMessage("info.world-unload-complete"));
                return world.getWorldFolder();
            }
            return new File(Bukkit.getWorldContainer(), "wildduel_game");
        }, runnable -> Bukkit.getScheduler().runTask(WildDuel.getInstance(), runnable))
        // Phase 2: Delete world folder (async)
        .thenCompose(worldFolder -> CompletableFuture.runAsync(() -> {
            if (worldFolder.exists()) {
                WildDuel.getInstance().getLogger().info(plugin.getMessage("info.deleting-world-folder", "%folder%", worldFolder.getName()));
                if (!deleteWorldRecursively(worldFolder)) {
                    throw new RuntimeException(plugin.getMessage("error.world-folder-delete-failed", "%folder%", worldFolder.getName()));
                }
                WildDuel.getInstance().getLogger().info(plugin.getMessage("info.world-folder-delete-complete"));
            }
        }, runnable -> WildDuel.getInstance().getServer().getAsyncScheduler().runNow(WildDuel.getInstance(), scheduledTask -> runnable.run())))
        // Phase 3: Create new world (on main thread)
        .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
            WildDuel.getInstance().getLogger().info(plugin.getMessage("info.creating-new-world"));
            WorldCreator wc = new WorldCreator("wildduel_game");
            wc.seed(new Random().nextLong());
            World newWorld = wc.createWorld();
            if (newWorld == null) {
                throw new RuntimeException(plugin.getMessage("error.new-world-create-failed"));
            }
            WildDuel.getInstance().getLogger().info(plugin.getMessage("info.new-world-create-complete"));
            return newWorld;
        }, runnable -> Bukkit.getScheduler().runTask(WildDuel.getInstance(), runnable)));
    }

    private boolean deleteWorldRecursively(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (!deleteWorldRecursively(file)) {
                            WildDuel.getInstance().getLogger().warning("Failed to delete sub-directory: " + file.getAbsolutePath());
                            return false;
                        }
                    } else {
                        if (!file.delete()) {
                            WildDuel.getInstance().getLogger().warning("Failed to delete file: " + file.getAbsolutePath());
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
        timerBar = Bukkit.createBossBar(plugin.getMessage("bossbar.farming-time"), BarColor.BLUE, BarStyle.SOLID);
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
                    Bukkit.broadcastMessage(plugin.getMessage("info.farming-time-remaining", "%minutes%", String.valueOf(prepTimeSeconds / 60)));
                }
                prepTimeSeconds--;
                timerBar.setProgress((double) prepTimeSeconds / initialPrepTimeSeconds);
                timerBar.setTitle(plugin.getMessage("bossbar.farming-time-remaining", "%time%", formatTime(prepTimeSeconds)));
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
            player.sendMessage(plugin.getMessage("info.battle-started"));
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
        if (getGameState() != GameState.BATTLE || gameWorld == null) {
            return;
        }

        List<Player> alivePlayers = gameWorld.getPlayers().stream()
                .filter(p -> p.getGameMode() == org.bukkit.GameMode.SURVIVAL && p.isOnline())
                .toList();

        if (gameMode == GameMode.TEAM) {
            java.util.Set<TeamManager.TeamData> aliveTeams = new java.util.HashSet<>();
            for (Player player : alivePlayers) {
                String teamName = teamManager.getPlayerTeam(player);
                if (teamName != null) {
                    for (TeamManager.TeamData team : teamManager.getTeams()) {
                        if (team.getName().equals(teamName)) {
                            aliveTeams.add(team);
                            break;
                        }
                    }
                }
            }

            if (aliveTeams.size() <= 1) {
                TeamManager.TeamData winner = aliveTeams.isEmpty() ? null : aliveTeams.iterator().next();
                endGame(winner);
            }
        } else { // Solo mode
            if (alivePlayers.size() <= 1) {
                Player winner = alivePlayers.isEmpty() ? null : alivePlayers.get(0);
                endGame(winner);
            }
        }
    }

    private void endGame(Object winner) {
        if (getGameState() == GameState.ENDED) {
            return;
        }
        setGameState(GameState.ENDED);

        String broadcastMessage;
        String titleMessage;

        if (winner instanceof Player) {
            Player winningPlayer = (Player) winner;
            broadcastMessage = plugin.getMessage("win.broadcast.player", "%player%", winningPlayer.getName());
            titleMessage = plugin.getMessage("win.title.player");
        } else if (winner instanceof TeamManager.TeamData) {
            TeamManager.TeamData winningTeam = (TeamManager.TeamData) winner;
            broadcastMessage = plugin.getMessage("win.broadcast.team", "%color%", winningTeam.getColor().toString(), "%team%", winningTeam.getName());
            titleMessage = plugin.getMessage("win.title.team", "%color%", winningTeam.getColor().toString(), "%team%", winningTeam.getName());
        } else {
            broadcastMessage = plugin.getMessage("win.broadcast.draw");
            titleMessage = plugin.getMessage("win.title.draw");
        }

        // Announce winner
        String separator = plugin.getMessage("separator.game-end");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(separator);
            player.sendMessage(" "); // Send a space for a blank line
            player.sendMessage(broadcastMessage);
            player.sendMessage(" ");
            player.sendMessage(separator);
            player.sendTitle(titleMessage, plugin.getMessage("title.subtitle.teleport-to-lobby"), 10, 80, 20);
        }

        // Cancel all game tasks
        if (gameTask != null) gameTask.cancel();
        if (timerBar != null) timerBar.removeAll();
        if (distanceDisplayTask != null) distanceDisplayTask.cancel();
        if (saturationTask != null) saturationTask.cancel();

        // Teleport everyone to lobby after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                transitionToLobby();
            }
        }.runTaskLater(WildDuel.getInstance(), 10 * 20); // 10 seconds
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

        if (this.gameWorld != null) {
            WildDuel.getInstance().getLogger().info(plugin.getMessage("info.cleaning-up-world"));
            World worldToClean = this.gameWorld;
            this.gameWorld = null; 
            CompletableFuture.runAsync(() -> {
                Bukkit.getScheduler().runTask(WildDuel.getInstance(), () -> {
                    if(worldToClean != null) {
                        Bukkit.unloadWorld(worldToClean, false);
                    }
                });
            }).thenRunAsync(() -> {
                deleteWorldRecursively(worldToClean.getWorldFolder());
                WildDuel.getInstance().getLogger().info(plugin.getMessage("info.world-cleanup-complete"));
            });
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
    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }
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
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.getMessage("actionbar.center-coords", "%x%", String.format("%.0f", center.getX()), "%z%", String.format("%.0f", center.getZ()))));
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

    public synchronized void shutdown() {
        if (gameTask != null) gameTask.cancel();
        if (saturationTask != null) saturationTask.cancel();
        if (timerBar != null) timerBar.removeAll();
        if (distanceDisplayTask != null) distanceDisplayTask.cancel();
        WildDuel.getInstance().getLogger().info("All game tasks cancelled for shutdown.");
    }
}