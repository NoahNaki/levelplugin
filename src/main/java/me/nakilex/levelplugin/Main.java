package me.nakilex.levelplugin;

import me.nakilex.levelplugin.blacksmith.commands.BlacksmithCommand;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.economy.commands.AddCoinsCommand;
import me.nakilex.levelplugin.economy.commands.BalanceCommand;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.effects.commands.EffectCommand;
//import me.nakilex.levelplugin.effects.listeners.EffectListener;
import me.nakilex.levelplugin.effects.listeners.StatsEffectListener;
import me.nakilex.levelplugin.effects.managers.EffectManager;
import me.nakilex.levelplugin.horse.commands.HorseCommand;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.horse.utils.HorseSaverTask;
import me.nakilex.levelplugin.items.commands.AddItemCommand;
import me.nakilex.levelplugin.items.listeners.*;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.lootchests.commands.LootChestCommand;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.listeners.LootChestCloseListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestListener;
import me.nakilex.levelplugin.lootchests.managers.CooldownManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
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
import me.nakilex.levelplugin.player.attributes.listeners.StatsMenuListener;
import me.nakilex.levelplugin.player.attributes.managers.ActionBarTask;
import me.nakilex.levelplugin.player.attributes.managers.ManaRegenTask;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.commands.ClassCommand;
import me.nakilex.levelplugin.player.classes.listeners.ClassMenuListener;
import me.nakilex.levelplugin.player.level.commands.AddXPCommand;
import me.nakilex.levelplugin.player.level.commands.SetLevelCommand;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import me.nakilex.levelplugin.player.listener.PlayerJoinListener;
import me.nakilex.levelplugin.player.listener.PlayerKillListener;
import me.nakilex.levelplugin.potions.commands.AddPotionCommand;
import me.nakilex.levelplugin.potions.listeners.PotionUseListener;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.storage.commands.StorageCommand;
import me.nakilex.levelplugin.storage.listeners.StorageEvents;
import me.nakilex.levelplugin.trade.commands.TradeCommand;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.listeners.PlayerRightClicksPlayerListener;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.utils.TradingWindow;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private static Main instance;
    private LevelManager levelManager;
    private MobManager mobManager;
    private EconomyManager economyManager;
    private ItemManager itemManager;
    private ItemUpgradeManager itemUpgradeManager;
    private SpellManager spellmanager;
    private HorseManager horseManager;
    private EffectManager effectManager;
    private PartyManager partyManager;
    public static final String PREFIX = "";
    private static Main plugin;
    private DealMaker dealMaker;
    private File customConfigFile;
    private FileConfiguration customConfig;
    private ConfigValues configValues;
    private MessageStrings messageStrings;
    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private LootChestManager lootChestManager;
    private PotionManager potionManager;
    private FileConfiguration mobConfig;
    private HorseConfigManager horseConfigManager;
    private NamespacedKey upgradeKey;

    @Override
    public void onEnable() {
        instance = this;
        plugin = this;

        loadConfigFiles();
        initializeManagers();
        setupCustomConfig();
        registerCommandsAndListeners();
        if (!validateDependencies()) return;
        startTasks();
        getLogger().info("LevelPlugin has been enabled successfully!");
    }

    private void loadConfigFiles() {
        saveResource("potions.yml", false);
        File configFile = new File(getDataFolder(), "potions.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        potionManager = new PotionManager(config);

        saveResource("custommobs.yml", false);
        mobConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "custommobs.yml"));

        horseConfigManager = new HorseConfigManager(getDataFolder());
        StorageManager.getInstance().loadAll();
    }

    private void initializeManagers() {

        itemManager = new ItemManager(this);

        configManager = new ConfigManager(this);
        cooldownManager = new CooldownManager(this, configManager, null);
        lootChestManager = new LootChestManager(this, configManager, cooldownManager);
        cooldownManager.setLootChestManager(lootChestManager);

        upgradeKey = new NamespacedKey(this, "upgrade_level");
        levelManager = new LevelManager(this);
        economyManager = new EconomyManager(this);
        itemUpgradeManager = new ItemUpgradeManager(this);
        mobManager = new MobManager(this);
        spellmanager = new SpellManager(this);
        partyManager = new PartyManager();
        effectManager = new EffectManager();

        StatsManager.getInstance().setLevelManager(levelManager);
    }

    private void setupCustomConfig() {
        createCustomConfig();
        configValues = new ConfigValues(this.customConfigFile);
        dealMaker = new DealMaker();
        messageStrings = new MessageStrings();
    }

    private void registerCommandsAndListeners() {
        BlacksmithGUI blacksmithGUI = new BlacksmithGUI(economyManager, itemUpgradeManager, itemManager);
        horseManager = new HorseManager(horseConfigManager);
        HorseGUI horseGUI = new HorseGUI(horseManager, economyManager);


        getCommand("trade").setExecutor(new TradeCommand());
        registerCommands(blacksmithGUI, horseGUI);
        registerListeners(blacksmithGUI, horseGUI);
    }

    private boolean validateDependencies() {
        if (!getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().severe("Citizens is installed but disabled! Check for errors.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }



    @Override
    public void onDisable() {
        if (economyManager != null) {
            economyManager.saveBalances();
        }
        if (dealMaker != null) {
            dealMaker.closeAllTrades();
        }

        getLogger().info("LevelPlugin has been disabled!");
    }

    // Singleton
    public static Main getInstance() {
        return instance;
    }

    public static Main getPlugin() {
        return plugin;
    }

    @Override
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getCustomConfig() {
        return customConfig;
    }

    public ConfigValues getConfigValues() {
        return configValues;
    }

    public DealMaker getDealMaker() {
        return dealMaker;
    }

    public MessageStrings getMessageStrings() {
        return messageStrings;
    }

    public void reloadConfigValues() {
        this.configValues = new ConfigValues(this.customConfigFile);
    }

    // Manager Getters
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
        }
        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void startTasks() {
        new ActionBarTask().runTaskTimer(this, 1L, 1L);
        new ManaRegenTask().runTaskTimer(this, 20L, 20L);
        new HorseSaverTask(horseConfigManager).runTaskTimer(this, 6000L, 6000L);
    }

    private void registerListeners(BlacksmithGUI blacksmithGUI, HorseGUI horseGUI) {
        // Existing listeners
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
        getServer().getPluginManager().registerEvents(new NPCClickListener(economyManager), this);
        getServer().getPluginManager().registerEvents(new NPCCommandListener(), this);
        getServer().getPluginManager().registerEvents(new StorageEvents(), this);
        getServer().getPluginManager().registerEvents(new PlayerRightClicksPlayerListener(), this);
        getServer().getPluginManager().registerEvents(new TradingWindow(), this);
        getServer().getPluginManager().registerEvents(new PartyChatListener(partyManager), this);
        getServer().getPluginManager().registerEvents(new PartyInviteListener(partyManager), this);
        getServer().getPluginManager().registerEvents(new LootChestListener(lootChestManager), this);
        getServer().getPluginManager().registerEvents(new LootChestCloseListener(lootChestManager), this);
        getServer().getPluginManager().registerEvents(new PotionUseListener(potionManager, this), this);
    }


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
        getCommand("addpotion").setExecutor(new AddPotionCommand(potionManager, this));
        getCommand("lootchest").setExecutor(new LootChestCommand(configManager, lootChestManager));
    }
}
