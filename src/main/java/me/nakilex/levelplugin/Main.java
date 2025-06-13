package me.nakilex.levelplugin;

import de.slikey.effectlib.EffectManager;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.blacksmith.managers.ItemRepairManager;
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
import me.nakilex.levelplugin.blacksmith.gui.RerollBrowser;
import me.nakilex.levelplugin.potions.gui.PotionBrowser;
import me.nakilex.levelplugin.items.listeners.PickupCustomItemListener;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.listeners.ChestHologramListener;
import me.nakilex.levelplugin.lootchests.managers.CooldownManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.party.PartyGlowManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.runes.gui.EquipRunesGUI;
import me.nakilex.levelplugin.runes.gui.IdentifyRunesGUI;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.ArcherSpell;
import me.nakilex.levelplugin.spells.RogueSpell;
import me.nakilex.levelplugin.spells.listener.MeteorListener;
import me.nakilex.levelplugin.spells.managers.ManaCostTracker;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.tips.BroadcastManager;
import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.utils.MetadataTrait;
import me.nakilex.levelplugin.utils.registeries.CommandRegistry;
import me.nakilex.levelplugin.utils.registeries.ListenerRegistry;
import me.nakilex.levelplugin.utils.registeries.TaskRegistry;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static Main instance;
    private BukkitAPIHelper mythicHelper;
    private LevelManager levelManager;
    private EconomyManager economyManager;
    private ItemManager itemManager;
    private ItemUpgradeManager itemUpgradeManager;
    private ItemRepairManager itemRepairManager;
    private me.nakilex.levelplugin.items.tools.ToolManager toolManager;
    private me.nakilex.levelplugin.player.mining.managers.MiningManager miningManager;
    private me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig miningRewardsConfig;
    private SpellManager spellmanager;
    private HorseManager horseManager;
    private EffectManager effectManager;
    private PartyManager partyManager;
    private me.nakilex.levelplugin.guild.GuildManager guildManager;
    private me.nakilex.levelplugin.guild.GuildGUI guildGUI;
    private PartyGlowManager partyGlowManager;
    public static final String PREFIX = "";
    private static Main plugin;
    private DealMaker dealMaker;
    private File customConfigFile;
    private FileConfiguration customConfig;
    private ConfigValues configValues;
    private MessageStrings messageStrings;
    private ConfigManager configManager;
    private PickupCustomItemListener pickupCustomItemListener;
    private CooldownManager cooldownManager;
    private LootChestManager lootChestManager;
    private PotionManager potionManager;
    private HorseConfigManager horseConfigManager;
    private NamespacedKey upgradeKey;
    private MobRewardsConfig mobRewardsConfig;
    private StorageEvents storageEvents; // Single, shared instance
    private StorageManager storageManager;
    private ItemConfig itemConfig;
    private PlayerConfig playerConfig;
    private DmgNumberToggleManager dmgNumberToggleManager;
    private IdentifyRunesGUI identifyGui;
    private ManaCostTracker manaTracker;
    private RogueSpell rogueSpell;
    private ProjectileFriendlyFireListener projectileFriendlyFireListener;
    private FileConfiguration bossConfig;
    private File bossConfigFile;
    private RunesManager runesManager;
    private IdentifyRunesGUI identifyRunesGUI;
    private GemsManager gemsManager;
    private GemExchangeGUI gemGui;
    private me.nakilex.levelplugin.enchanting.managers.EnchantManager enchantManager;
    private me.nakilex.levelplugin.enchanting.gui.EnchantGUI enchantGUI;
    private ArcherSpell archerSpell;
    private TipsConfigManager tipsCfg;
    private BroadcastManager broadcastMgr;
    private me.nakilex.levelplugin.quests.managers.QuestManager questManager;
    private me.nakilex.levelplugin.npc.dialog.NPCDialogManager dialogManager;
    private me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager scoreboardManager;
    private me.nakilex.levelplugin.quests.managers.BeaconManager beaconManager;
    private me.nakilex.levelplugin.fasttravel.FastTravelManager fastTravelManager;
    private me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI fastTravelGUI;
    private me.nakilex.levelplugin.motd.MotdManager motdManager;
    private me.nakilex.levelplugin.fakeblock.FakeBlockManager fakeBlockManager;
    private me.nakilex.levelplugin.fakeblock.QuestGateManager questGateManager;
    private me.nakilex.levelplugin.fakeblock.ModelGateManager modelGateManager;
    private me.nakilex.levelplugin.environment.EnvironmentManager environmentManager;
    private me.nakilex.levelplugin.environment.UpgradeGUI upgradeGUI;
    private me.nakilex.levelplugin.environment.BuildingUpgradeGUI buildingUpgradeGUI;
    private me.nakilex.levelplugin.environment.stage.TownStageManager townStageManager;
    private me.nakilex.levelplugin.environment.stage.BuildingStageManager buildingStageManager;
    private me.nakilex.levelplugin.leaderboards.LeaderboardManager leaderboardManager;
    private me.nakilex.levelplugin.leaderboards.DuelStatsManager duelStatsManager;

    private final Map<UUID, List<NPC>> activeBowDrones = new HashMap<>();
    private ChestHologramListener chestHologramListener;
    private EquipRunesGUI equipGui;
    private me.nakilex.levelplugin.auctionhouse.AuctionHouseManager auctionHouseManager;
    private me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI auctionHouseGUI;
    private SettingsManager settingsManager;
    private SettingsGUI settingsGUI;

    public Map<UUID, List<NPC>> getActiveBowDrones() {
        return activeBowDrones;
    }

    // Mage Listeners

    private MeteorListener meteorListener;

    @Override
    public void onEnable() {
        // Set the plugin instance
        instance = this;
        plugin = this;

        manaTracker = new ManaCostTracker(1.5, 5_000L);


        // Load configuration files
        loadConfigFiles();

        // Prepare player configuration before managers that depend on it
        playerConfig = new PlayerConfig(this);

        // Initialize managers and other components
        initializeManagers();

        // Now that LevelManager is ready, load player data
        playerConfig.loadAllPlayers();

        // Initialize ItemConfig and load items
        itemConfig = new ItemConfig(this);
        itemConfig.loadItems();

        storageEvents = new StorageEvents();    // Create it here
        getServer().getPluginManager().registerEvents(storageEvents, this);

        // Managers that depend on PlayerConfig
        environmentManager = new me.nakilex.levelplugin.environment.EnvironmentManager(playerConfig, townStageManager, buildingStageManager, fakeBlockManager);
        upgradeGUI = new me.nakilex.levelplugin.environment.UpgradeGUI(environmentManager);
        buildingUpgradeGUI = new me.nakilex.levelplugin.environment.BuildingUpgradeGUI(environmentManager);


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
        new me.nakilex.levelplugin.items.tools.gui.ToolBrowser(this);
        new RerollBrowser(this);
        new me.nakilex.levelplugin.potions.gui.PotionBrowser(this, potionManager);

        EffectRegistry.registerAll();

        if (leaderboardManager != null) {
            leaderboardManager.addAll();
        }

        // Log success message
        getLogger().info("LevelPlugin has been enabled successfully!");
    }


    private void loadConfigFiles() {
        saveResource("potions.yml", false);
        File configFile = new File(getDataFolder(), "potions.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        potionManager = new PotionManager(config);

        horseConfigManager = new HorseConfigManager(getDataFolder());

        saveResource("field_bosses.yml", false);
        bossConfigFile = new File(getDataFolder(), "field_bosses.yml");
        bossConfig = YamlConfiguration.loadConfiguration(bossConfigFile);
    }

    private void initializeManagers() {

        itemManager = new ItemManager(this);
        toolManager = new me.nakilex.levelplugin.items.tools.ToolManager();

        configManager = new ConfigManager(this);
        cooldownManager = new CooldownManager(this, configManager, null);
        lootChestManager = new LootChestManager(this, configManager, cooldownManager, potionManager);
        dmgNumberToggleManager = new DmgNumberToggleManager();
        upgradeKey = new NamespacedKey(this, "upgrade_level");
        levelManager = new LevelManager(this);
        miningManager = new me.nakilex.levelplugin.player.mining.managers.MiningManager(this);
        miningRewardsConfig = new me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig(this);
        effectManager = new EffectManager(this);
        economyManager = new EconomyManager(this);
        itemUpgradeManager = new ItemUpgradeManager(this);
        itemRepairManager = new ItemRepairManager();
        runesManager = new RunesManager(this);
        spellmanager = new SpellManager(this, runesManager);
        partyManager = new PartyManager();
        guildManager = me.nakilex.levelplugin.guild.GuildManager.getInstance();
        guildGUI = new me.nakilex.levelplugin.guild.GuildGUI(guildManager);
        identifyRunesGUI = new IdentifyRunesGUI(this, runesManager);
        identifyGui = identifyRunesGUI;
        gemsManager = new GemsManager();
        gemGui = new GemExchangeGUI(this, gemsManager);
        auctionHouseManager = new me.nakilex.levelplugin.auctionhouse.AuctionHouseManager(this, economyManager);
        auctionHouseGUI = new me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI(this, auctionHouseManager, economyManager);
        tipsCfg = new TipsConfigManager(this);
        broadcastMgr = new BroadcastManager(this, this.tipsCfg);
        broadcastMgr.start();
        settingsManager = new SettingsManager();
        questManager = new QuestManager(this, partyManager);
        dialogManager = new me.nakilex.levelplugin.npc.dialog.NPCDialogManager();
        scoreboardManager = new me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager(
                this, economyManager, gemsManager, partyManager, questManager);
        duelStatsManager = new me.nakilex.levelplugin.leaderboards.DuelStatsManager(this);
        leaderboardManager = new me.nakilex.levelplugin.leaderboards.LeaderboardManager(this, economyManager, playerConfig, duelStatsManager, settingsManager);
        partyGlowManager = new PartyGlowManager(this, partyManager, scoreboardManager::getBoard);
        beaconManager = new me.nakilex.levelplugin.quests.managers.BeaconManager();
        fastTravelManager = new me.nakilex.levelplugin.fasttravel.FastTravelManager(this);
        modelGateManager = new me.nakilex.levelplugin.fakeblock.ModelGateManager(this);
        fastTravelGUI = new me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI(fastTravelManager, economyManager, modelGateManager);
        motdManager = new me.nakilex.levelplugin.motd.MotdManager(this);
        fakeBlockManager = new me.nakilex.levelplugin.fakeblock.FakeBlockManager();
        questGateManager = new me.nakilex.levelplugin.fakeblock.QuestGateManager(this, fakeBlockManager);
        townStageManager = new me.nakilex.levelplugin.environment.stage.TownStageManager(this);
        buildingStageManager = new me.nakilex.levelplugin.environment.stage.BuildingStageManager(this);
        cooldownManager.setLootChestManager(lootChestManager);
        equipGui = new EquipRunesGUI(this, runesManager, identifyRunesGUI);
        enchantManager = new me.nakilex.levelplugin.enchanting.managers.EnchantManager();
        enchantGUI = new me.nakilex.levelplugin.enchanting.gui.EnchantGUI(enchantManager, economyManager);

        StatsManager.getInstance().setLevelManager(levelManager);
    }

    private void setupCustomConfig() {
        createCustomConfig();
        configValues = new ConfigValues(this.customConfigFile);
        dealMaker = new DealMaker();
        messageStrings = new MessageStrings();
    }

    private void registerCommandsAndListeners() {
        BlacksmithGUI blacksmithGUI = new BlacksmithGUI(economyManager, itemUpgradeManager, itemManager, itemRepairManager);
        horseManager = new HorseManager(horseConfigManager);
        HorseGUI horseGUI = new HorseGUI(horseManager, economyManager);
        // use the already-created settingsManager
        settingsGUI = new SettingsGUI(settingsManager); // Initialize the field properly


        // 1) Assign the field so it’s not null.
        this.storageManager = new StorageManager();

        CommandRegistry.registerCommands(
            this,
            blacksmithGUI,
            horseGUI,
            levelManager,
            miningManager,
            economyManager,
            partyManager,
            guildManager,
            guildGUI,
            potionManager,
            lootChestManager,
            configManager,
            horseManager,
            storageManager,
            dmgNumberToggleManager,
            settingsGUI,
            gemsManager,
            gemGui,
            auctionHouseManager,
            auctionHouseGUI,
            tipsCfg,
            identifyRunesGUI,
            runesManager,
            equipGui,
            enchantGUI,
            broadcastMgr,
            questManager,
            fastTravelManager,
            motdManager,
            upgradeGUI
        );


        ListenerRegistry.registerListeners(
            this,
            blacksmithGUI,
            horseGUI,
            lootChestManager,
            potionManager,
            partyManager,
            economyManager,
            mobRewardsConfig,
            dmgNumberToggleManager,
            pickupCustomItemListener,
            settingsGUI,
            rogueSpell,
            projectileFriendlyFireListener,
            bossConfig,
            archerSpell,
            meteorListener,
            gemsManager,
            identifyRunesGUI,
            runesManager,
            equipGui,
            enchantGUI,
            chestHologramListener,
            questManager,
            dialogManager,
            scoreboardManager,
            fastTravelManager,
            fastTravelGUI,
            motdManager,
            upgradeGUI,
            buildingUpgradeGUI,
            new me.nakilex.levelplugin.environment.listeners.BuildingHologramListener(buildingUpgradeGUI)
        );

        getServer().getPluginManager().registerEvents(beaconManager, this);


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

        if (auctionHouseManager != null) {
            auctionHouseManager.saveAuctions();
        }

        if (lootChestManager != null) {
            lootChestManager.removeAllChests(); // Remove holograms and clean up
        }

        if (me.nakilex.levelplugin.player.mining.listeners.OreMiningListener.getInstance() != null) {
            me.nakilex.levelplugin.player.mining.listeners.OreMiningListener.getInstance().removeAllHolograms();
        }

        if (questManager != null) {
            questManager.saveProgress();
        }

        if (modelGateManager != null) {
            modelGateManager.removeAllGates();
        }

        if (environmentManager != null) {
            environmentManager.saveAll();
        }

        if (leaderboardManager != null) {
            leaderboardManager.removeAll();
        }

        if (duelStatsManager != null) {
            duelStatsManager.save();
        }

        if (townStageManager != null) {
            townStageManager.despawnAll();
        }
        if (buildingStageManager != null) {
            buildingStageManager.despawnAll();
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
        return super.getConfig();   // calls JavaPlugin's implementation, not yourself
    }

    public PlayerConfig getPlayerConfig() {
        return playerConfig;
    }

    public SpellManager getSpellManager() {
        return spellmanager;
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

    public ManaCostTracker getManaTracker() {
        return manaTracker;
    }

    public void reloadConfigValues() {
        this.configValues = new ConfigValues(this.customConfigFile);
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public me.nakilex.levelplugin.player.mining.managers.MiningManager getMiningManager() {
        return miningManager;
    }

    public me.nakilex.levelplugin.items.tools.ToolManager getToolManager() {
        return toolManager;
    }

    public me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig getMiningRewardsConfig() {
        return miningRewardsConfig;
    }

    public GemsManager getGemsManager() {
        return gemsManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public me.nakilex.levelplugin.auctionhouse.AuctionHouseManager getAuctionHouseManager() {
        return auctionHouseManager;
    }

    public FileConfiguration getBossConfig() {
        return bossConfig;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public RunesManager getRunesManager() {
        return runesManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public me.nakilex.levelplugin.quests.managers.BeaconManager getBeaconManager() {
        return beaconManager;
    }

    public me.nakilex.levelplugin.fasttravel.FastTravelManager getFastTravelManager() {
        return fastTravelManager;
    }

    public me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI getFastTravelGUI() {
        return fastTravelGUI;
    }

    public me.nakilex.levelplugin.motd.MotdManager getMotdManager() {
        return motdManager;
    }

    public me.nakilex.levelplugin.fakeblock.QuestGateManager getQuestGateManager() {
        return questGateManager;
    }

    public me.nakilex.levelplugin.fakeblock.FakeBlockManager getFakeBlockManager() {
        return fakeBlockManager;
    }

    public me.nakilex.levelplugin.fakeblock.ModelGateManager getModelGateManager() {
        return modelGateManager;
    }

    public me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public PartyGlowManager getPartyGlowManager() {
        return partyGlowManager;
    }

    public me.nakilex.levelplugin.environment.EnvironmentManager getEnvironmentManager() {
        return environmentManager;
    }

    public me.nakilex.levelplugin.environment.stage.TownStageManager getTownStageManager() {
        return townStageManager;
    }

    public me.nakilex.levelplugin.environment.stage.BuildingStageManager getBuildingStageManager() {
        return buildingStageManager;
    }
    public me.nakilex.levelplugin.leaderboards.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public me.nakilex.levelplugin.leaderboards.DuelStatsManager getDuelStatsManager() {
        return duelStatsManager;
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
