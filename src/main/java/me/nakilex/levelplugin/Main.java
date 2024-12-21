package me.nakilex.levelplugin;

import me.nakilex.levelplugin.commands.*;
import me.nakilex.levelplugin.economy.BalanceCommand;
import me.nakilex.levelplugin.economy.EconomyManager;
import me.nakilex.levelplugin.items.ItemManager;
import me.nakilex.levelplugin.listeners.*;
import me.nakilex.levelplugin.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.managers.LevelManager;
import me.nakilex.levelplugin.managers.NPCManager;
import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.mob.MobManager;
import me.nakilex.levelplugin.spells.SpellManager;
import me.nakilex.levelplugin.tasks.ActionBarTask;
import me.nakilex.levelplugin.tasks.ManaRegenTask;
import me.nakilex.levelplugin.tasks.WeaponCheckTask;
import me.nakilex.levelplugin.ui.BlacksmithGUI;
import me.nakilex.levelplugin.ui.ClassMenuListener;
import me.nakilex.levelplugin.ui.StatsMenuListener;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {

    private static Main instance;

    // Managers
    private LevelManager levelManager;
    private MobManager mobManager;
    private EconomyManager economyManager;
    private ItemManager itemManager;
    private ItemUpgradeManager itemUpgradeManager;
    private NPCManager npcManager;
    private SpellManager spellmanager;

    // Configurations
    private FileConfiguration mobConfig;

    // NamespacedKey for PersistentDataContainer
    private NamespacedKey upgradeKey;

    @Override
    public void onEnable() {
        instance = this;

        // Load custommobs.yml configuration
        saveResource("custommobs.yml", false); // Copy file from JAR if it doesn't exist
        mobConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "custommobs.yml"));
        getLogger().info("Loaded custommobs.yml: " + mobConfig.saveToString()); // Debugging configuration loading

        // Initialize NPC Manager
        npcManager = new NPCManager(this);

        // Load and spawn NPCs
        npcManager.loadNPCs();
        npcManager.spawnAllNPCs();

        // Initialize NamespacedKey for item upgrades
        upgradeKey = new NamespacedKey(this, "upgrade_level");

        // Initialize Managers
        levelManager = new LevelManager(this);
        economyManager = new EconomyManager(this);
        itemManager = new ItemManager(this); // Load items dynamically from items.yml
        itemUpgradeManager = new ItemUpgradeManager(this);
        mobManager = new MobManager(this);
        spellmanager = new SpellManager(this); // Happens after registering listeners


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
        // Save balances if economy manager is initialized
        if (economyManager != null) {
            economyManager.saveBalances();
        }

        // Despawn and save NPCs
        if (npcManager != null) {
            npcManager.despawnAllNPCs();
            npcManager.saveNPCs();
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

    public NPCManager getNPCManager() {
        return npcManager;
    }

    // Getter for Upgrade Key
    public NamespacedKey getUpgradeKey() {
        return upgradeKey;
    }

    // Start periodic tasks
    private void startTasks() {
        new ActionBarTask().runTaskTimer(this, 1L, 1L);
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
        getServer().getPluginManager().registerEvents(new NPCClickListener(npcManager), this);
    }

    // Register plugin commands
    private void registerCommands(BlacksmithGUI blacksmithGUI) {
        getCommand("addpoints").setExecutor(new AddPointsCommand());
        getCommand("addxp").setExecutor(new AddXPCommand(levelManager));
        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("additem").setExecutor(new AddItemCommand());
        getCommand("setlevel").setExecutor(new SetLevelCommand(this));
        getCommand("class").setExecutor(new ClassCommand());
        getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        getCommand("addcoins").setExecutor(new AddCoinsCommand(economyManager));
        getCommand("addmob").setExecutor(new AddMobCommand(mobManager));
        getCommand("blacksmith").setExecutor(new BlacksmithCommand(blacksmithGUI));
        getCommand("npc").setExecutor(new NPCCommand(npcManager));
    }
}
