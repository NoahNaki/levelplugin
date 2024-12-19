package me.nakilex.levelplugin;

import me.nakilex.levelplugin.commands.*;
import me.nakilex.levelplugin.economy.BalanceCommand;
import me.nakilex.levelplugin.economy.EconomyManager;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.listeners.*;
import me.nakilex.levelplugin.managers.*;
import me.nakilex.levelplugin.mob.MobManager;
import me.nakilex.levelplugin.tasks.*;
import me.nakilex.levelplugin.ui.BlacksmithGUI;
import me.nakilex.levelplugin.ui.ClassMenuListener;
import me.nakilex.levelplugin.ui.StatsMenuListener;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main extends JavaPlugin {

    private static Main instance;

    // Managers
    private LevelManager levelManager;
    private MobManager mobManager;
    private EconomyManager economyManager;
    private ItemManager itemManager;
    private ItemUpgradeManager itemUpgradeManager;

    // Configurations
    private FileConfiguration mobConfig;

    // NamespacedKey for PersistentDataContainer
    private NamespacedKey upgradeKey;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize NamespacedKey for item upgrades
        upgradeKey = new NamespacedKey(this, "upgrade_level");

        // Load configuration files
        createMobConfig();

        // Initialize Managers
        levelManager = new LevelManager(this);
        mobManager = new MobManager(this);
        economyManager = new EconomyManager(this);
        itemManager = new ItemManager(this); // Load items dynamically from items.yml
        itemUpgradeManager = new ItemUpgradeManager(this);

        StatsManager.getInstance().setLevelManager(levelManager);

        // Start repeating tasks
        startTasks();

        // Initialize Blacksmith GUI
        BlacksmithGUI blacksmithGUI = new BlacksmithGUI(economyManager, itemUpgradeManager, itemManager);

        // Register Listeners
        registerListeners(blacksmithGUI);

        // Register Commands
        registerCommands(blacksmithGUI);

        getLogger().info("LevelPlugin (with Blacksmith and dynamic items) has been enabled!");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) {
            economyManager.saveBalances();
        }
        getLogger().info("LevelPlugin has been disabled!");
    }

    // Singleton Instance Getter
    public static Main getInstance() {
        return instance;
    }

    // Getters for Managers
    public LevelManager getLevelManager() {
        return levelManager;
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public ItemUpgradeManager getItemUpgradeManager() {
        return itemUpgradeManager;
    }

    // Getter for Upgrade Key
    public NamespacedKey getUpgradeKey() {
        return upgradeKey;
    }

    // Create mob configuration
    private void createMobConfig() {
        File mobFile = new File(getDataFolder(), "mobs.yml");
        if (!mobFile.exists()) {
            saveResource("mobs.yml", false);
        }
        mobConfig = YamlConfiguration.loadConfiguration(mobFile);

        InputStream defaultStream = this.getResource("mobs.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            mobConfig.setDefaults(defaultConfig);
        }
    }

    // Start periodic tasks
    private void startTasks() {
        new ActionBarTask().runTaskTimer(this, 20L, 20L);
        new WeaponCheckTask().runTaskTimer(this, 20L, 20L);
        new ManaRegenTask().runTaskTimer(this, 20L, 20L);
    }

    // Register plugin event listeners
    private void registerListeners(BlacksmithGUI blacksmithGUI) {
        getServer().getPluginManager().registerEvents(new MobDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerKillListener(levelManager, mobConfig), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(levelManager), this);
        getServer().getPluginManager().registerEvents(new StatsMenuListener(), this);
        getServer().getPluginManager().registerEvents(new StatsEffectListener(), this);
        getServer().getPluginManager().registerEvents(new ArmorEquipListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponHeldListener(), this);
        getServer().getPluginManager().registerEvents(new ClickComboListener(), this);
        getServer().getPluginManager().registerEvents(new ItemNameDisplayListener(), this);
        getServer().getPluginManager().registerEvents(new StaticItemListener(), this);
        getServer().getPluginManager().registerEvents(new ClassMenuListener(), this);
        getServer().getPluginManager().registerEvents(blacksmithGUI, this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(mobManager, economyManager), this);
    }

    // Register plugin commands
    private void registerCommands(BlacksmithGUI blacksmithGUI) {
        getCommand("addpoints").setExecutor(new AddPointsCommand());
        getCommand("addxp").setExecutor(new AddXPCommand(levelManager));
        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("additem").setExecutor(new AddItemCommand()); // Pass item manager for dynamic item creation
        getCommand("setlevel").setExecutor(new SetLevelCommand(this));
        getCommand("class").setExecutor(new ClassCommand());
        getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        getCommand("addcoins").setExecutor(new AddCoinsCommand(economyManager));
        getCommand("addmob").setExecutor(new AddMobCommand(mobManager));
        getCommand("blacksmith").setExecutor(new BlacksmithCommand(blacksmithGUI));
    }
}
