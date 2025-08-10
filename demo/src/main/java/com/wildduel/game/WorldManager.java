package com.wildduel.game;

import com.wildduel.WildDuel;
import com.wildduel.util.EmptyWorldGenerator;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WorldManager {

    private final WildDuel plugin;
    private final String lobbyWorldName;
    private final String gameWorldName;
    private World lobbyWorld;
    private World gameWorld;

    public WorldManager(WildDuel plugin) {
        this.plugin = plugin;
        // config.yml에서 월드 이름을 불러오도록 수정 (추후 적용)
        this.lobbyWorldName = "wildduel_world";
        this.gameWorldName = "wildduel_game";
    }

    public void initializeWorlds() {
        this.lobbyWorld = Bukkit.getWorld(lobbyWorldName);
        if (this.lobbyWorld == null) {
            plugin.getLogger().info("Lobby world '" + lobbyWorldName + "' not found, creating it...");
            WorldCreator wc = new WorldCreator(lobbyWorldName);
            wc.generator(new EmptyWorldGenerator());
            this.lobbyWorld = wc.createWorld();
            if (this.lobbyWorld != null) {
                this.lobbyWorld.setSpawnLocation(0, 1, 0);
            }
        }
    }

    public CompletableFuture<World> recreateGameWorldAsync() {
        return unloadWorldAsync(gameWorldName)
                .thenCompose(this::deleteWorldFolderAsync)
                .thenCompose(v -> createNewWorldAsync(gameWorldName))
                .thenCompose(this::preGenerateSpawnChunksAsync)
                .whenComplete((world, ex) -> {
                    if (ex != null) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to create game world", ex);
                        this.gameWorld = null;
                    } else {
                        this.gameWorld = world;
                        plugin.getLogger().info("New game world is ready.");
                    }
                });
    }

    private CompletableFuture<File> unloadWorldAsync(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                plugin.getLogger().info("Unloading world: " + world.getName());
                if (!Bukkit.unloadWorld(world, false)) {
                    throw new RuntimeException("Failed to unload world: " + world.getName());
                }
                plugin.getLogger().info("World unloaded successfully.");
                return world.getWorldFolder();
            }
            return new File(Bukkit.getWorldContainer(), worldName);
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    private CompletableFuture<Void> deleteWorldFolderAsync(File worldFolder) {
        return CompletableFuture.runAsync(() -> {
            if (worldFolder.exists()) {
                plugin.getLogger().info("Deleting world folder: " + worldFolder.getName());
                if (!deleteWorldRecursively(worldFolder)) {
                    throw new RuntimeException("Failed to delete world folder: " + worldFolder.getName());
                }
                plugin.getLogger().info("World folder deleted successfully.");
            }
        }, runnable -> plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run()));
    }

    private CompletableFuture<World> createNewWorldAsync(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("Creating new world: " + worldName);
            WorldCreator wc = new WorldCreator(worldName);
            wc.seed(new Random().nextLong());
            World newWorld = wc.createWorld();
            if (newWorld == null) {
                throw new RuntimeException("Failed to create new world.");
            }
            plugin.getLogger().info("New world created. Pre-generating spawn chunks...");
            return newWorld;
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    private CompletableFuture<World> preGenerateSpawnChunksAsync(World world) {
        if (world == null) return CompletableFuture.completedFuture(null);

        Location spawn = world.getSpawnLocation();
        int radius = 8; // 17x17 chunks
        List<CompletableFuture<Chunk>> chunkFutures = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                chunkFutures.add(PaperLib.getChunkAtAsync(world, spawn.getChunk().getX() + x, spawn.getChunk().getZ() + z, true));
            }
        }

        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    plugin.getLogger().info("Spawn chunk generation complete for: " + world.getName());
                    return world;
                });
    }

    private boolean deleteWorldRecursively(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (!deleteWorldRecursively(file)) return false;
                    } else {
                        if (!file.delete()) return false;
                    }
                }
            }
        }
        return path.delete();
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public void setGameWorld(World world) {
        this.gameWorld = world;
    }
}
