package me.nakilex.levelplugin;

import de.slikey.effectlib.EffectManager;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.duels.listeners.ProjectileFriendlyFireListener;
import me.nakilex.levelplugin.economy.gui.GemExchangeGUI;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.items.config.ItemConfig;
import me.nakilex.levelplugin.items.gui.ItemsBrowser;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.managers.CooldownManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import me.nakilex.levelplugin.mob.managers.MobManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.placeholders.MyCustomExpansion;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.MageSpell;
import me.nakilex.levelplugin.spells.RogueSpell;
import me.nakilex.levelplugin.spells.managers.ManaCostTracker;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.tips.BroadcastManager;
import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.*;
import me.nakilex.levelplugin.utils.registeries.CommandRegistry;
import me.nakilex.levelplugin.utils.registeries.ListenerRegistry;
import me.nakilex.levelplugin.utils.registeries.TaskRegistry;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private static Main instance;
    private BukkitAPIHelper mythicHelper;
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
    private MobRewardsConfig mobRewardsConfig;
    private StorageEvents storageEvents; // Single, shared instance
    private StorageManager storageManager;
    private ItemConfig itemConfig;
    private PlayerConfig playerConfig;
    private DmgNumberToggleManager dmgNumberToggleManager;
    private ManaCostTracker manaTracker;
    private RogueSpell rogueSpell;
    private ProjectileFriendlyFireListener projectileFriendlyFireListener;
    private FileConfiguration bossConfig;
    private File bossConfigFile;
    private GemsManager gemsManager;
    private GemExchangeGUI gemGui;
    private MageSpell mageSpell;
    private TipsConfigManager tipsCfg;
    private BroadcastManager broadcastMgr;


    @Override
    public void onEnable() {
        // Set the plugin instance
        instance = this;
        plugin = this;

        manaTracker = new ManaCostTracker(1.5, 5_000L);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MyCustomExpansion(this).register();
            getLogger().info("MyCustomExpansion registered with PlaceholderAPI!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Custom placeholders will not work.");
        }


        // Load configuration files
        loadConfigFiles();

        // Initialize managers and other components
        initializeManagers();

        // Initialize ItemConfig and load items
        itemConfig = new ItemConfig(this);
        itemConfig.loadItems();

        storageEvents = new StorageEvents();    // Create it here
        getServer().getPluginManager().registerEvents(storageEvents, this);


        playerConfig = new PlayerConfig(this);
        playerConfig.loadAllPlayers();


        CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(MetadataTrait.class).withName("MetadataTrait"));
        // Setup custom configurations like mob_rewards.yml
        setupCustomConfig();

        // Validate dependencies (e.g., MythicMobs)
        if (!validateDependencies()) {
            getLogger().severe("Missing required dependencies. Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize the MobRewardsConfig
        mobRewardsConfig = new MobRewardsConfig(this);

        // Register commands and event listeners
        registerCommandsAndListeners();
        new ItemsBrowser(this);

        // Log success message
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

        saveResource("field_bosses.yml", false);
        bossConfigFile = new File(getDataFolder(), "field_bosses.yml");
        bossConfig = YamlConfiguration.loadConfiguration(bossConfigFile);
    }

    private void initializeManagers() {

        itemManager = new ItemManager(this);

        configManager = new ConfigManager(this);
        cooldownManager = new CooldownManager(this, configManager, null);
        lootChestManager = new LootChestManager(this, configManager, cooldownManager, potionManager);
        cooldownManager.setLootChestManager(lootChestManager);
        dmgNumberToggleManager = new DmgNumberToggleManager();
        upgradeKey = new NamespacedKey(this, "upgrade_level");
        levelManager = new LevelManager(this);
        effectManager = new EffectManager(this);
        economyManager = new EconomyManager(this);
        itemUpgradeManager = new ItemUpgradeManager(this);
        mobManager = new MobManager(this);
        spellmanager = new SpellManager(this);
        partyManager = new PartyManager();
        gemsManager = new GemsManager();
        gemGui      = new GemExchangeGUI(this, gemsManager);
        this.tipsCfg     = new TipsConfigManager(this);
        this.broadcastMgr = new BroadcastManager(this, this.tipsCfg);
        this.broadcastMgr.start();

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
        SettingsManager settingsManager = new SettingsManager();
        SettingsGUI settingsGUI = new SettingsGUI(settingsManager); // Initialize the field properly


        // 1) Assign the field so it’s not null.
        this.storageManager = new StorageManager();

        CommandRegistry.registerCommands(
            this,
            blacksmithGUI,
            horseGUI,
            levelManager,
            economyManager,
            partyManager,
            potionManager,
            lootChestManager,
            configManager,
            horseManager,
            mobManager,
            storageManager,
            dmgNumberToggleManager,
            settingsGUI,
            gemsManager,
            gemGui,
            tipsCfg,
            broadcastMgr
        );


        ListenerRegistry.registerListeners(
            this,
            blacksmithGUI,
            horseGUI,
            lootChestManager,
            potionManager,
            partyManager,
            economyManager,
            mobConfig,
            mobRewardsConfig,
            dmgNumberToggleManager,
            settingsGUI,
            rogueSpell,
            projectileFriendlyFireListener,
            bossConfig,
            mageSpell,
            gemsManager
        );


        TaskRegistry.startTasks(this, horseConfigManager, horseManager);
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

        // Save items before shutting down
        if (itemConfig != null) {
            itemConfig.saveItems();
        }

        if (playerConfig != null) {
            playerConfig.saveAllPlayers();
        }

        if (storageManager != null) {
            storageManager.saveAllStorages();
        }

        if (dealMaker != null)
            dealMaker.closeAllTrades();

        getLogger().info("LevelPlugin has been disabled!");
    }


    public BukkitAPIHelper getMythicHelper() {
        return mythicHelper;
    }

    public static Main getInstance() {
        return instance;
    }

    public static Main getPlugin() {
        return plugin;
    }

    public PotionManager getPotionManager() {
        return potionManager;
    }

    @Override
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public MobManager getMobManager() {
        return mobManager;
    }

    public FileConfiguration getCustomConfig() {
        return customConfig;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfigValues getConfigValues() {
        return configValues;
    }

    public StorageEvents getStorageEvents() {
        return storageEvents;
    }

    public DealMaker getDealMaker() {
        return dealMaker;
    }

    public ItemConfig getItemConfig() {
        return itemConfig;
    }

    public MessageStrings getMessageStrings() {
        return messageStrings;
    }

    public ManaCostTracker getManaTracker() { return manaTracker; }

    public void reloadConfigValues() {
        this.configValues = new ConfigValues(this.customConfigFile);
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public GemsManager getGemsManager() {
        return gemsManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public FileConfiguration getBossConfig() {
        return bossConfig;
    }

    public EffectManager getEffectManager() { return effectManager; }


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
