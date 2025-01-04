package me.nakilex.levelplugin;

import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.effects.managers.EffectManager;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.managers.CooldownManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.managers.MobManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.CommandRegistry;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.utils.ListenerRegistry;
import me.nakilex.levelplugin.utils.TaskRegistry;
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

        CommandRegistry.registerCommands(
            this,
            blacksmithGUI,
            horseGUI,
            levelManager,
            economyManager,
            partyManager,
            effectManager,
            potionManager,
            lootChestManager,
            configManager,
            horseManager,
            mobManager
        );

        ListenerRegistry.registerListeners(
            this,
            blacksmithGUI,
            horseGUI,
            lootChestManager,
            potionManager,
            partyManager,
            economyManager,
            mobConfig
        );

        TaskRegistry.startTasks(this, horseConfigManager);
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

    public EconomyManager getEconomyManager() {
        return economyManager;
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
}
