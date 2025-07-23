package com.wildduel;

import com.wildduel.commands.*;
import com.wildduel.game.GameManager;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.listeners.AdminGUIListener;
import com.wildduel.listeners.PlayerEventListener;
import com.wildduel.listeners.TeamAdminGUIListener;
import com.wildduel.listeners.TeamGUIListener;
import com.wildduel.listeners.TpaListener;
import com.wildduel.util.EmptyWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import com.wildduel.util.PlayerInventorySnapshot;
import com.wildduel.listeners.StartItemGUIListener;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.bukkit.ChatColor;

public class WildDuel extends JavaPlugin {

    private static WildDuel instance;
    private GameManager gameManager;
    private TeamManager teamManager;
    private TeamAdminManager teamAdminManager;
    private TpaManager tpaManager;
    private List<ItemStack> defaultStartItems = new ArrayList<>();
    private PlayerInventorySnapshot defaultStartInventory;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadMessages();
        loadDefaultStartItems();
        loadDefaultStartInventory();

        // Create lobby world if it doesn't exist
        createLobbyWorld();

        teamManager = new TeamManager();
        gameManager = new GameManager(teamManager);
        teamAdminManager = new TeamAdminManager();
        tpaManager = new TpaManager(teamManager);

        // Initialize managers with world context
        gameManager.initializeWorlds();

        // Ensure teams are set up on plugin enable/reload
        teamManager.initializeTeams();

        getCommand("wd").setExecutor(new WildDuelCommand(gameManager, teamManager, teamAdminManager, tpaManager));
        getCommand("wd").setTabCompleter(new WildDuelTabCompleter());
        getCommand("tpa").setExecutor(new TpaCommand(tpaManager));
        getCommand("tpacancel").setExecutor(new TpaCancelCommand(tpaManager));
        getCommand("tparesponse").setExecutor(new TpaResponseCommand(tpaManager));
        getCommand("팀선택").setExecutor(new TeamSelectCommand(gameManager, teamManager));

        getServer().getPluginManager().registerEvents(new PlayerEventListener(gameManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new TeamAdminGUIListener(teamAdminManager, teamManager), this);
        getServer().getPluginManager().registerEvents(new TpaListener(tpaManager), this);
        getServer().getPluginManager().registerEvents(new AdminGUIListener(gameManager, teamManager, tpaManager, teamAdminManager), this);
        getServer().getPluginManager().registerEvents(new TeamGUIListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new StartItemGUIListener(this), this);
        getLogger().info("WildDuel plugin enabled!");
    }

    private void createLobbyWorld() {
        World lobbyWorld = Bukkit.getWorld("wildduel_world");
        if (lobbyWorld == null) {
            getLogger().info("'wildduel_world' not found, creating it...");
            WorldCreator wc = new WorldCreator("wildduel_world");
            wc.generator(new EmptyWorldGenerator());
            lobbyWorld = wc.createWorld();
            if (lobbyWorld != null) {
                lobbyWorld.setSpawnLocation(0, 1, 0);
                getLogger().info("'wildduel_world' created successfully.");
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("WildDuel plugin disabled!");
    }

    public static WildDuel getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public List<ItemStack> getDefaultStartItems() {
        return defaultStartItems;
    }

    public void addDefaultStartItem(ItemStack item) {
        defaultStartItems.add(item);
        saveDefaultStartItems();
    }

    public void removeDefaultStartItem(ItemStack item) {
        // Remove by material type for simplicity, or could match exact item meta
        defaultStartItems.removeIf(i -> i.getType() == item.getType());
        saveDefaultStartItems();
    }

    private void saveDefaultStartItems() {
        List<java.util.Map<String, Object>> serializedItems = new ArrayList<>();
        for (ItemStack item : defaultStartItems) {
            java.util.Map<String, Object> itemMap = new java.util.HashMap<>();
            itemMap.put("material", item.getType().name());
            itemMap.put("amount", item.getAmount());
            // Add more item meta if needed (e.g., enchantments, display name)
            serializedItems.add(itemMap);
        }
        getConfig().set("default-start-items", serializedItems);
        saveConfig();
    }

    private void loadDefaultStartItems() {
        FileConfiguration config = getConfig();
        if (config.isList("default-start-items")) {
            List<?> items = config.getList("default-start-items");
            for (Object itemObj : items) {
                if (itemObj instanceof java.util.Map) {
                    java.util.Map<String, Object> itemMap = (java.util.Map<String, Object>) itemObj;
                    String materialName = (String) itemMap.get("material");
                    int amount = (Integer) itemMap.getOrDefault("amount", 1);
                    if (materialName != null) {
                        Material material = Material.getMaterial(materialName.toUpperCase());
                        if (material != null) {
                            defaultStartItems.add(new ItemStack(material, amount));
                        } else {
                            getLogger().warning("Unknown material: " + materialName);
                        }
                    }
                }
            }
        } else {
            getLogger().warning("default-start-items section not found or invalid in config.yml");
        }
    }

    public void recreateGameWorld(Runnable callback) {
        World gameWorld = Bukkit.getWorld("wildduel_game");
        File worldFolder = (gameWorld != null) ? gameWorld.getWorldFolder() : new File(Bukkit.getWorldContainer(), "wildduel_game");

        // Unload the world on the main thread
        if (gameWorld != null) {
            getLogger().info("Unloading world: " + gameWorld.getName());
            if (!Bukkit.unloadWorld(gameWorld, false)) {
                getLogger().warning("Failed to unload world: " + gameWorld.getName() + ". World deletion and creation will be aborted.");
                // Optionally, run callback even if unload fails, or handle error differently
                if (callback != null) {
                    Bukkit.getScheduler().runTask(this, callback);
                }
                return;
            }
            getLogger().info("World " + worldFolder.getName() + " unloaded successfully.");
        }

        // Delete the world folder asynchronously
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                getLogger().info("Attempting to delete world folder asynchronously: " + worldFolder.getName());
                boolean deleted = deleteWorld(worldFolder);
                if (deleted) {
                    getLogger().info("World folder " + worldFolder.getName() + " deleted successfully.");
                } else {
                    getLogger().warning("Failed to delete world folder: " + worldFolder.getName());
                }

                // Create the new world back on the main thread
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        getLogger().info("Creating new 'wildduel_game'...");
                        WorldCreator wc = new WorldCreator("wildduel_game");
                        wc.seed(new Random().nextLong());
                        World newWorld = wc.createWorld();
                        if (newWorld != null) {
                            getLogger().info("New 'wildduel_game' created successfully.");
                        } else {
                            getLogger().severe("Failed to create new 'wildduel_game'.");
                        }
                        
                        // Execute the callback if it exists
                        if (callback != null) {
                            callback.run();
                        }
                    }
                }.runTask(WildDuel.getInstance());
            }
        }.runTaskAsynchronously(WildDuel.getInstance());
    }

    private boolean deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorld(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return path.delete();
    }

    public PlayerInventorySnapshot getDefaultStartInventory() {
        return defaultStartInventory;
    }

    public void setDefaultStartInventory(PlayerInventorySnapshot snapshot) {
        this.defaultStartInventory = snapshot;
        saveDefaultStartInventory();
    }

    private void loadDefaultStartInventory() {
        ConfigurationSection section = getConfig().getConfigurationSection("default-start-inventory");
        if (section != null) {
            defaultStartInventory = PlayerInventorySnapshot.deserialize(section);
        } else {
            defaultStartInventory = new PlayerInventorySnapshot(); // Default empty snapshot
        }
    }

    private void saveDefaultStartInventory() {
        ConfigurationSection section = getConfig().createSection("default-start-inventory");
        defaultStartInventory.serialize(section);
        saveConfig();
    }

    public void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        // Load default messages from JAR
        Reader defConfigStream = new InputStreamReader(this.getResource("messages.yml"), StandardCharsets.UTF_8);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            messagesConfig.setDefaults(defConfig);
        }
    }

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(key, "&cMissing message: " + key));
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i+1]);
            }
        }
        return message;
    }
}
