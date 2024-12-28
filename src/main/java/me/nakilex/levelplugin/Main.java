package me.nakilex.levelplugin;

import me.nakilex.levelplugin.blacksmith.commands.BlacksmithCommand;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.effects.commands.EffectCommand;
import me.nakilex.levelplugin.effects.listeners.EffectListener;
import me.nakilex.levelplugin.effects.listeners.StatsEffectListener;
import me.nakilex.levelplugin.items.commands.AddItemCommand;
import me.nakilex.levelplugin.items.listeners.*;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.mob.commands.AddMobCommand;
import me.nakilex.levelplugin.mob.listeners.MobDamageListener;
import me.nakilex.levelplugin.mob.managers.MobManager;
import me.nakilex.levelplugin.npc.listeners.NPCClickListener;
import me.nakilex.levelplugin.npc.listeners.NPCCommandListener;
import me.nakilex.levelplugin.party.PartyChatListener;
import me.nakilex.levelplugin.party.PartyCommands;
import me.nakilex.levelplugin.party.PartyInviteListener;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.attributes.commands.AddPointsCommand;
import me.nakilex.levelplugin.player.attributes.commands.StatsCommand;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.commands.ClassCommand;
import me.nakilex.levelplugin.economy.commands.AddCoinsCommand;
import me.nakilex.levelplugin.economy.commands.BalanceCommand;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.effects.managers.EffectManager;
import me.nakilex.levelplugin.horse.commands.HorseCommand;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.player.level.commands.AddXPCommand;
import me.nakilex.levelplugin.player.level.commands.SetLevelCommand;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.attributes.managers.ActionBarTask;
import me.nakilex.levelplugin.horse.utils.HorseSaverTask;
import me.nakilex.levelplugin.player.attributes.managers.ManaRegenTask;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import me.nakilex.levelplugin.player.listener.PlayerJoinListener;
import me.nakilex.levelplugin.player.listener.PlayerKillListener;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.trade.commands.TradeCommand;
import me.nakilex.levelplugin.trade.listeners.PlayerRightClicksPlayerListener;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.player.classes.listeners.ClassMenuListener;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.player.attributes.listeners.StatsMenuListener;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.TradingWindow;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.storage.commands.StorageCommand;
import me.nakilex.levelplugin.storage.listeners.StorageEvents;


import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private static Main instance;

    // Managers
    private LevelManager levelManager;
    private MobManager mobManager;
    private EconomyManager economyManager;
    private ItemManager itemManager;
    private ItemUpgradeManager itemUpgradeManager;
    private SpellManager spellmanager;
    private HorseManager horseManager;
    private EffectManager effectManager;
    private PartyManager partyManager;
    public final static String PREFIX = "";
    private static Main plugin;
    private DealMaker dealMaker;
    private File customConfigFile;
    private FileConfiguration customConfig;
    private ConfigValues configValues;
    private MessageStrings messageStrings;

    // Configurations
    private FileConfiguration mobConfig;
    private HorseConfigManager horseConfigManager;

    // NamespacedKey for PersistentDataContainer
    private NamespacedKey upgradeKey;

    @Override
    public void onEnable() {
        instance = this;

        // Load custommobs.yml configuration
        saveResource("custommobs.yml", true); // Copy file from JAR if it doesn't exist
        mobConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "custommobs.yml"));
        horseConfigManager = new HorseConfigManager(getDataFolder());
        StorageManager.getInstance().loadAll(); // Load existing storage data

        //Trade Plugin
        this.plugin = this;
        this.createCustomConfig();
        this.configValues = new ConfigValues(this.customConfigFile);
        this.dealMaker = new DealMaker();
        this.messageStrings = new MessageStrings();
        this.getCommand("trade").setExecutor(new TradeCommand());


        // Initialize NamespacedKey for item upgrades
        upgradeKey = new NamespacedKey(this, "upgrade_level");

        // Initialize Managers
        levelManager = new LevelManager(this);
        economyManager = new EconomyManager(this);
        itemManager = new ItemManager(this); // Load items dynamically from items.yml
        itemUpgradeManager = new ItemUpgradeManager(this);
        mobManager = new MobManager(this);
        spellmanager = new SpellManager(this); // Happens after registering listeners
        partyManager = new PartyManager();

        // Initialize EffectManager
        effectManager = new EffectManager();

        StatsManager.getInstance().setLevelManager(levelManager);

        // Start repeating tasks
        startTasks();

        // Initialize Blacksmith GUI and Horse GUI
        BlacksmithGUI blacksmithGUI = new BlacksmithGUI(economyManager, itemUpgradeManager, itemManager);
        horseManager = new HorseManager(horseConfigManager);
        HorseGUI horseGUI = new HorseGUI(horseManager, economyManager);

        // Register Listeners
        registerListeners(blacksmithGUI, horseGUI);

        // Register Commands
        registerCommands(blacksmithGUI, horseGUI);

        if (!getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().severe("Citizens is installed but disabled! Check for errors.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

    }

    @Override
    public void onDisable() {
        // Save balances if economy manager is initialized
        if (economyManager != null) {
            economyManager.saveBalances();
        }

        this.dealMaker.closeAllTrades();

        getLogger().info("LevelPlugin has been disabled!");
    }

    // Singleton Instance Getter
    public static Main getInstance() {
        return instance;
    }

    public FileConfiguration getCustomConfig() {
        return this.customConfig;
    }

    public static Main getPlugin() {
        return plugin;
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public ConfigValues getConfigValues() {
        return this.configValues;
    }

    public DealMaker getDealMaker() {
        return this.dealMaker;
    }

    public MessageStrings getMessageStrings() {
        return this.messageStrings;
    }

    public void reloadConfigValues() {
        this.configValues = new ConfigValues(this.customConfigFile);
    }

    // Getters for Managers
    public LevelManager getLevelManager() {
        return levelManager;
    }

    private void createCustomConfig() {
        customConfigFile = new File(getDataFolder(), "config.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            try {
                customConfigFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            saveResource("config.yml", false);
        }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    // Start periodic tasks
    private void startTasks() {
        new ActionBarTask().runTaskTimer(this, 1L, 1L);
        new ManaRegenTask().runTaskTimer(this, 20L, 20L);
        new HorseSaverTask(horseConfigManager).runTaskTimer(this, 6000L, 6000L); // Saves every 5 minutes
    }

    // Register plugin event listeners
    private void registerListeners(BlacksmithGUI blacksmithGUI, HorseGUI horseGUI) {
        getServer().getPluginManager().registerEvents(new MobDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerKillListener(levelManager, mobConfig, partyManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(levelManager), this);
        getServer().getPluginManager().registerEvents(new StatsMenuListener(), this);
        getServer().getPluginManager().registerEvents(new StatsEffectListener(), this);
        getServer().getPluginManager().registerEvents(new ArmorListener(), this);
        getServer().getPluginManager().registerEvents(new ArmorStatsListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponListener(), this);
        getServer().getPluginManager().registerEvents(new WeaponStatsListener(), this);
        getServer().getPluginManager().registerEvents(new ClickComboListener(), this);
        getServer().getPluginManager().registerEvents(new ItemNameDisplayListener(), this);
        getServer().getPluginManager().registerEvents(new StaticItemListener(), this);
        getServer().getPluginManager().registerEvents(new ClassMenuListener(), this);
        getServer().getPluginManager().registerEvents(blacksmithGUI, this);
        getServer().getPluginManager().registerEvents(horseGUI, this);
        getServer().getPluginManager().registerEvents(new EffectListener(effectManager), this);
        getServer().getPluginManager().registerEvents(new NPCClickListener(economyManager), this);
        getServer().getPluginManager().registerEvents(new NPCCommandListener(), this);
        getServer().getPluginManager().registerEvents(new StorageEvents(), this);
        getServer().getPluginManager().registerEvents(new PlayerRightClicksPlayerListener(), this);
        getServer().getPluginManager().registerEvents(new TradingWindow(), this);
        getServer().getPluginManager().registerEvents(new PartyChatListener(partyManager), this);
        getServer().getPluginManager().registerEvents(new PartyInviteListener(partyManager), this);



    }

    // Register plugin commands
    private void registerCommands(BlacksmithGUI blacksmithGUI, HorseGUI horseGUI) {
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
        getCommand("horse").setExecutor(new HorseCommand(horseManager, horseGUI));
        getCommand("effect").setExecutor(new EffectCommand(effectManager));
        getCommand("ps").setExecutor(new StorageCommand());
        getCommand("party").setExecutor(new PartyCommands(partyManager));


    }
}
