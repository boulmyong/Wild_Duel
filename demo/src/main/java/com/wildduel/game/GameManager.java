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
    private boolean isTeamGameMode = false;

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

    public void askAdminToStart(Player admin) {
        TextComponent message = new TextComponent(ChatColor.YELLOW + "어떤 작업을 수행하시겠습니까? ");

        TextComponent regenLink = new TextComponent(ChatColor.RED + "[월드 재생성]");
        regenLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wd regen_world_confirm"));
        regenLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§c모든 플레이어를 내보내고 맵을 초기화합니다.\n§c(서버 랙 유발)")));

        TextComponent startLink = new TextComponent(ChatColor.GREEN + "[즉시 시작]");
        startLink.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/wd start_game_final"));
        startLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§a현재 준비된 월드에서 즉시 게임을 시작합니다.")));

        message.addExtra(regenLink);
        message.addExtra(new TextComponent(ChatColor.GRAY + " 또는 "));
        message.addExtra(startLink);

        admin.spigot().sendMessage(message);
    }

    public void executeWorldRegeneration() {
        if (isWorldRegenerating) {
            Bukkit.broadcastMessage(ChatColor.RED + "게임 월드가 이미 생성중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }
        isWorldRegenerating = true;
        Bukkit.setWhitelist(true);
        WildDuel.getInstance().getLogger().info("월드 재생성을 위해 화이트리스트를 활성화합니다.");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(ChatColor.GREEN + "서버 관리자가 게임 월드를 재생성하고 있습니다. 잠시 후 다시 접속해주세요.");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                WildDuel.getInstance().getLogger().info("플레이어 처리 후 월드 재생성을 시작합니다...");
                recreateGameWorldAsync().whenComplete((newWorld, ex) -> {
                    isWorldRegenerating = false;
                    Bukkit.setWhitelist(false);
                    WildDuel.getInstance().getLogger().info("월드 재생성이 완료되어 화이트리스트를 비활성화합니다.");
                    if (ex != null) {
                        WildDuel.getInstance().getLogger().log(Level.SEVERE, "게임 월드 생성에 실패했습니다. 플러그인을 확인해주세요.", ex);
                        GameManager.this.gameWorld = null;
                    } else {
                        GameManager.this.gameWorld = newWorld;
                        WildDuel.getInstance().getLogger().info("다음 게임을 위한 월드 준비가 완료되었습니다.");
                    }
                });
            }
        }.runTaskLater(WildDuel.getInstance(), 60L); // 3-second delay
    }

    public void executeGameStart() {
        if (getGameState() != GameState.LOBBY) {
            Bukkit.broadcastMessage(ChatColor.RED + "게임이 로비 상태일 때만 시작할 수 있습니다.");
            return;
        }
        if (isWorldRegenerating) {
            Bukkit.broadcastMessage(ChatColor.RED + "게임 월드가 생성중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }
        if (gameWorld == null) {
            Bukkit.broadcastMessage(ChatColor.RED + "게임 월드가 아직 준비되지 않았습니다. 먼저 [월드 재생성]을 실행해주세요.");
            return;
        }

        List<Player> participants = new ArrayList<>(lobbyWorld.getPlayers());
        if (participants.size() < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "게임 시작을 위한 최소 인원은 2명입니다.");
            return;
        }

        // Determine game mode and validate teams
        this.isTeamGameMode = participants.stream().anyMatch(p -> teamManager.getPlayerTeam(p) != null);
        if (this.isTeamGameMode) {
            for (Player player : participants) {
                if (teamManager.getPlayerTeam(player) == null) {
                    Bukkit.broadcastMessage(ChatColor.RED + "팀전에 참여하지 않은 플레이어가 있습니다. 모든 플레이어를 팀에 배정해주세요.");
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
                    player.sendTitle(ChatColor.GREEN + "게임이 곧 시작됩니다...", ChatColor.YELLOW + String.valueOf(countdown), 0, 25, 5);
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
    }

    

    private CompletableFuture<World> recreateGameWorldAsync() {
        // Phase 1: Unload world (on main thread)
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
        // Phase 2: Delete world folder (async)
        .thenCompose(worldFolder -> CompletableFuture.runAsync(() -> {
            if (worldFolder.exists()) {
                WildDuel.getInstance().getLogger().info("기존 월드 폴더를 삭제합니다: " + worldFolder.getName());
                if (!deleteWorldRecursively(worldFolder)) {
                    throw new RuntimeException("월드 폴더 삭제에 실패했습니다: " + worldFolder.getName());
                }
                WildDuel.getInstance().getLogger().info("월드 폴더 삭제 완료.");
            }
        }, runnable -> WildDuel.getInstance().getServer().getAsyncScheduler().runNow(WildDuel.getInstance(), scheduledTask -> runnable.run())))
        // Phase 3: Create new world (on main thread)
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

        List<Player> alivePlayers = gameWorld.getPlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL && p.isOnline())
                .toList();

        if (isTeamGameMode) {
            // Team Game Logic
            List<TeamManager.TeamData> activeTeams = new ArrayList<>();
            for (TeamManager.TeamData teamData : teamManager.getTeams()) {
                if (teamData.getPlayers().stream().anyMatch(alivePlayers::contains)) {
                    activeTeams.add(teamData);
                }
            }

            if (activeTeams.size() <= 1) {
                endGame(activeTeams.isEmpty() ? null : activeTeams.get(0));
            }
        } else {
            // FFA Logic
            if (alivePlayers.size() <= 1) {
                endGame(alivePlayers.isEmpty() ? null : alivePlayers.get(0));
            }
        }
    }

    private void endGame(Player winner) {
        if (getGameState() == GameState.ENDED) return;
        setGameState(GameState.ENDED);

        String winnerMessage = "§a" + winner.getName() + "님 승리!";
        String broadcastWinner = "§f승리: §a" + winner.getName() + "님!";

        broadcastEndMessage(winnerMessage, broadcastWinner);
    }

    private void endGame(TeamManager.TeamData winner) {
        if (getGameState() == GameState.ENDED) return;
        setGameState(GameState.ENDED);

        String winnerMessage;
        String broadcastWinner;

        if (winner != null && !winner.getPlayers().isEmpty()) {
            if (winner.getPlayers().size() == 1) {
                Player soloWinner = new ArrayList<>(winner.getPlayers()).get(0);
                winnerMessage = "§a" + soloWinner.getName() + "님 승리!";
                broadcastWinner = "§f승리: §a" + soloWinner.getName() + "님!";
            } else {
                winnerMessage = winner.getColor() + winner.getName() + " 팀 승리!";
                broadcastWinner = "§f승리: " + winner.getColor() + winner.getName() + " 팀!";
            }
        } else {
            winnerMessage = "게임이 무승부로 종료되었습니다!";
            broadcastWinner = "§f승리: 무승부!";
        }

        broadcastEndMessage(winnerMessage, broadcastWinner);
    }

    private void broadcastEndMessage(String winnerMessage, String broadcastWinner) {
        Bukkit.broadcastMessage("§a========================================");
        Bukkit.broadcastMessage("§e           게임 종료");
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage(broadcastWinner);
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

        if (this.gameWorld != null) {
            WildDuel.getInstance().getLogger().info("게임 월드 정리 중...");
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
                WildDuel.getInstance().getLogger().info("게임 월드 정리 완료.");
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

    public synchronized void shutdown() {
        if (gameTask != null) gameTask.cancel();
        if (saturationTask != null) saturationTask.cancel();
        if (timerBar != null) timerBar.removeAll();
        if (distanceDisplayTask != null) distanceDisplayTask.cancel();
        WildDuel.getInstance().getLogger().info("All game tasks cancelled for shutdown.");
    }
}