package me.nakilex.levelplugin.core;

import de.slikey.effectlib.EffectManager;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import me.nakilex.levelplugin.Main;
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
import me.nakilex.levelplugin.friend.FriendManager;
import me.nakilex.levelplugin.friend.FriendGlowManager;
import me.nakilex.levelplugin.friend.PlayerVisibilityManager;
import me.nakilex.levelplugin.friend.IgnoreManager;
import me.nakilex.levelplugin.friend.FriendRequestListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PluginBootstrap {
    private final Main plugin;

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
    private me.nakilex.levelplugin.guild.GuildMemberGUI guildMemberGUI;
    private PartyGlowManager partyGlowManager;
    private me.nakilex.levelplugin.friend.FriendManager friendManager;
    private me.nakilex.levelplugin.friend.FriendGlowManager friendGlowManager;
    private me.nakilex.levelplugin.friend.PlayerVisibilityManager visibilityManager;
    private IgnoreManager ignoreManager;
    private FriendRequestListener friendRequestListener;
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
    private me.nakilex.levelplugin.mob.config.ModelSetManager modelSetManager;
    private StorageEvents storageEvents;
    private StorageManager storageManager;
    private ItemConfig itemConfig;
    private PlayerConfig playerConfig;
    private DmgNumberToggleManager dmgNumberToggleManager;
    private ManaCostTracker manaTracker;
    private ProjectileFriendlyFireListener projectileFriendlyFireListener;
    private FileConfiguration bossConfig;
    private File bossConfigFile;
    private GemsManager gemsManager;
    private GemExchangeGUI gemGui;
    private me.nakilex.levelplugin.enchanting.managers.EnchantManager enchantManager;
    private me.nakilex.levelplugin.enchanting.gui.EnchantGUI enchantGUI;
    private TipsConfigManager tipsCfg;
    private BroadcastManager broadcastMgr;
    private me.nakilex.levelplugin.quests.managers.QuestManager questManager;
    private me.nakilex.levelplugin.npc.dialog.NPCDialogManager dialogManager;
    private me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager scoreboardManager;
    private me.nakilex.levelplugin.quests.managers.BeaconManager beaconManager;
    private me.nakilex.levelplugin.fasttravel.FastTravelManager fastTravelManager;
    private me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI fastTravelGUI;
    private me.nakilex.levelplugin.motd.MotdManager motdManager;
    private me.nakilex.levelplugin.calendar.CalendarManager calendarManager;
    private me.nakilex.levelplugin.cutscene.CutsceneManager cutsceneManager;
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
    private me.nakilex.levelplugin.auctionhouse.AuctionHouseManager auctionHouseManager;
    private me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI auctionHouseGUI;
    private SettingsManager settingsManager;
    private SettingsGUI settingsGUI;
    private MeteorListener meteorListener;

    public PluginBootstrap(Main plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        manaTracker = new ManaCostTracker(1.5, 5_000L);
        loadConfigFiles();
        playerConfig = new PlayerConfig(plugin);
        initializeManagers();
        playerConfig.loadAllPlayers();
        itemConfig = new ItemConfig(plugin);
        itemConfig.loadItems();
        storageEvents = new StorageEvents();
        plugin.getServer().getPluginManager().registerEvents(storageEvents, plugin);
        environmentManager = new me.nakilex.levelplugin.environment.EnvironmentManager(playerConfig, townStageManager, buildingStageManager, fakeBlockManager);
        upgradeGUI = new me.nakilex.levelplugin.environment.UpgradeGUI(environmentManager);
        buildingUpgradeGUI = new me.nakilex.levelplugin.environment.BuildingUpgradeGUI(environmentManager);
        CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(MetadataTrait.class).withName("MetadataTrait"));
        setupCustomConfig();
        if (!validateDependencies()) {
            plugin.getLogger().severe("Missing required dependencies. Disabling plugin..");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }
        mobRewardsConfig = new MobRewardsConfig(plugin);
        registerCommandsAndListeners();
        new ItemsBrowser(plugin);
        new me.nakilex.levelplugin.items.tools.gui.ToolBrowser(plugin);
        new RerollBrowser(plugin);
        new me.nakilex.levelplugin.potions.gui.PotionBrowser(plugin, potionManager);
        if (leaderboardManager != null) {
            leaderboardManager.addAll();
        }
        plugin.getLogger().info("LevelPlugin has been enabled successfully!");
    }

    private void loadConfigFiles() {
        plugin.saveResource("potions.yml", false);
        File configFile = new File(plugin.getDataFolder(), "potions.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        potionManager = new PotionManager(config);
        horseConfigManager = new HorseConfigManager(plugin.getDataFolder());
        plugin.saveResource("field_bosses.yml", false);
        bossConfigFile = new File(plugin.getDataFolder(), "field_bosses.yml");
        bossConfig = YamlConfiguration.loadConfiguration(bossConfigFile);
    }

    private void initializeManagers() {
        itemManager = new ItemManager(plugin);
        toolManager = new me.nakilex.levelplugin.items.tools.ToolManager();
        configManager = new ConfigManager(plugin);
        cooldownManager = new CooldownManager(plugin, configManager, null);
        lootChestManager = new LootChestManager(plugin, configManager, cooldownManager, potionManager);
        dmgNumberToggleManager = new DmgNumberToggleManager();
        upgradeKey = new NamespacedKey(plugin, "upgrade_level");
        levelManager = new LevelManager(plugin);
        miningManager = new me.nakilex.levelplugin.player.mining.managers.MiningManager(plugin);
        miningRewardsConfig = new me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig(plugin);
        effectManager = new EffectManager(plugin);
        economyManager = new EconomyManager(plugin);
        itemUpgradeManager = new ItemUpgradeManager(plugin);
        itemRepairManager = new ItemRepairManager();
        spellmanager = new SpellManager(plugin);
        partyManager = new PartyManager();
        friendManager = new FriendManager();
        guildManager = me.nakilex.levelplugin.guild.GuildManager.getInstance();
        guildGUI = new me.nakilex.levelplugin.guild.GuildGUI(guildManager);
        guildMemberGUI = new me.nakilex.levelplugin.guild.GuildMemberGUI(guildManager);
        gemsManager = new GemsManager();
        gemGui = new GemExchangeGUI(plugin, gemsManager);
        auctionHouseManager = new me.nakilex.levelplugin.auctionhouse.AuctionHouseManager(plugin, economyManager);
        auctionHouseGUI = new me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI(plugin, auctionHouseManager, economyManager);
        tipsCfg = new TipsConfigManager(plugin);
        broadcastMgr = new BroadcastManager(plugin, this.tipsCfg);
        broadcastMgr.start();
        settingsManager = new SettingsManager();
        questManager = new QuestManager(plugin, partyManager);
        dialogManager = new me.nakilex.levelplugin.npc.dialog.NPCDialogManager(plugin);
        scoreboardManager = new me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager(plugin, economyManager, gemsManager, partyManager, questManager);
        calendarManager = new me.nakilex.levelplugin.calendar.CalendarManager(plugin);
        duelStatsManager = new me.nakilex.levelplugin.leaderboards.DuelStatsManager(plugin);
        leaderboardManager = new me.nakilex.levelplugin.leaderboards.LeaderboardManager(plugin, economyManager, playerConfig, duelStatsManager, settingsManager);
        partyGlowManager = new PartyGlowManager(plugin, partyManager, scoreboardManager::getBoard);
        friendGlowManager = new FriendGlowManager(plugin, friendManager, scoreboardManager::getBoard);
        visibilityManager = new PlayerVisibilityManager(plugin, friendManager, settingsManager);
        ignoreManager = new IgnoreManager(plugin);
        friendRequestListener = new FriendRequestListener(friendManager);
        beaconManager = new me.nakilex.levelplugin.quests.managers.BeaconManager();
        fastTravelManager = new me.nakilex.levelplugin.fasttravel.FastTravelManager(plugin);
        modelGateManager = new me.nakilex.levelplugin.fakeblock.ModelGateManager(plugin);
        fastTravelGUI = new me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI(fastTravelManager, economyManager, modelGateManager);
        motdManager = new me.nakilex.levelplugin.motd.MotdManager(plugin);
        fakeBlockManager = new me.nakilex.levelplugin.fakeblock.FakeBlockManager();
        questGateManager = new me.nakilex.levelplugin.fakeblock.QuestGateManager(plugin, fakeBlockManager);
        townStageManager = new me.nakilex.levelplugin.environment.stage.TownStageManager(plugin);
        buildingStageManager = new me.nakilex.levelplugin.environment.stage.BuildingStageManager(plugin);
        cooldownManager.setLootChestManager(lootChestManager);
        enchantManager = new me.nakilex.levelplugin.enchanting.managers.EnchantManager();
        enchantGUI = new me.nakilex.levelplugin.enchanting.gui.EnchantGUI(enchantManager, economyManager);
        StatsManager.getInstance().setLevelManager(levelManager);
        modelSetManager = new me.nakilex.levelplugin.mob.config.ModelSetManager(plugin);
        cutsceneManager = new me.nakilex.levelplugin.cutscene.CutsceneManager(plugin);
        cutsceneManager.loadCutscenes();
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
        settingsGUI = new SettingsGUI(settingsManager);
        this.storageManager = new StorageManager();
        CommandRegistry.registerCommands(
            plugin,
            blacksmithGUI,
            horseGUI,
            levelManager,
            miningManager,
            economyManager,
            partyManager,
            guildManager,
            guildGUI,
            guildMemberGUI,
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
            enchantGUI,
            broadcastMgr,
            questManager,
            fastTravelManager,
            motdManager,
            upgradeGUI
        );
        ListenerRegistry.registerListeners(
            plugin,
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
            projectileFriendlyFireListener,
            bossConfig,
            meteorListener,
            gemsManager,
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
            new me.nakilex.levelplugin.environment.listeners.BuildingHologramListener(buildingUpgradeGUI),
            new me.nakilex.levelplugin.environment.listeners.StageBlockInteractListener(plugin, fakeBlockManager)
        );
        plugin.getServer().getPluginManager().registerEvents(beaconManager, plugin);
        TaskRegistry.startTasks(plugin, horseConfigManager, horseManager);
    }

    private boolean validateDependencies() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            plugin.getLogger().severe("Citizens is installed but disabled! Check for errors.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }
        return true;
    }

    public void disable() {
        if (economyManager != null) economyManager.saveBalances();
        if (dealMaker != null) dealMaker.closeAllTrades();
        if (itemConfig != null) itemConfig.saveItems();
        if (playerConfig != null) playerConfig.saveAllPlayers();
        if (storageManager != null) storageManager.saveAllStorages();
        if (auctionHouseManager != null) auctionHouseManager.saveAuctionsSync();
        if (lootChestManager != null) lootChestManager.removeAllChests();
        if (me.nakilex.levelplugin.player.mining.listeners.OreMiningListener.getInstance() != null) me.nakilex.levelplugin.player.mining.listeners.OreMiningListener.getInstance().removeAllHolograms();
        if (questManager != null) questManager.saveProgress();
        if (modelGateManager != null) modelGateManager.removeAllGates();
        if (environmentManager != null) environmentManager.saveAll();
        if (leaderboardManager != null) leaderboardManager.removeAll();
        if (duelStatsManager != null) duelStatsManager.save();
        if (townStageManager != null) townStageManager.despawnAll();
        if (buildingStageManager != null) buildingStageManager.despawnAll();
        if (dealMaker != null) dealMaker.closeAllTrades();
        plugin.getLogger().info("LevelPlugin has been disabled!");
    }

    public Map<UUID, List<NPC>> getActiveBowDrones() { return activeBowDrones; }
    public BukkitAPIHelper getMythicHelper() { return mythicHelper; }
    public LevelManager getLevelManager() { return levelManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public ItemManager getItemManager() { return itemManager; }
    public ItemUpgradeManager getItemUpgradeManager() { return itemUpgradeManager; }
    public ItemRepairManager getItemRepairManager() { return itemRepairManager; }
    public me.nakilex.levelplugin.items.tools.ToolManager getToolManager() { return toolManager; }
    public me.nakilex.levelplugin.player.mining.managers.MiningManager getMiningManager() { return miningManager; }
    public me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig getMiningRewardsConfig() { return miningRewardsConfig; }
    public SpellManager getSpellmanager() { return spellmanager; }
    public HorseManager getHorseManager() { return horseManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public me.nakilex.levelplugin.guild.GuildManager getGuildManager() { return guildManager; }
    public me.nakilex.levelplugin.guild.GuildGUI getGuildGUI() { return guildGUI; }
    public me.nakilex.levelplugin.guild.GuildMemberGUI getGuildMemberGUI() { return guildMemberGUI; }
    public PartyGlowManager getPartyGlowManager() { return partyGlowManager; }
    public me.nakilex.levelplugin.friend.FriendManager getFriendManager() { return friendManager; }
    public me.nakilex.levelplugin.friend.FriendGlowManager getFriendGlowManager() { return friendGlowManager; }
    public me.nakilex.levelplugin.friend.PlayerVisibilityManager getVisibilityManager() { return visibilityManager; }
    public IgnoreManager getIgnoreManager() { return ignoreManager; }
    public FriendRequestListener getFriendRequestListener() { return friendRequestListener; }
    public DealMaker getDealMaker() { return dealMaker; }
    public FileConfiguration getCustomConfig() { return customConfig; }
    public ConfigValues getConfigValues() { return configValues; }
    public MessageStrings getMessageStrings() { return messageStrings; }
    public ConfigManager getConfigManager() { return configManager; }
    public PickupCustomItemListener getPickupCustomItemListener() { return pickupCustomItemListener; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public LootChestManager getLootChestManager() { return lootChestManager; }
    public PotionManager getPotionManager() { return potionManager; }
    public HorseConfigManager getHorseConfigManager() { return horseConfigManager; }
    public NamespacedKey getUpgradeKey() { return upgradeKey; }
    public MobRewardsConfig getMobRewardsConfig() { return mobRewardsConfig; }
    public me.nakilex.levelplugin.mob.config.ModelSetManager getModelSetManager() { return modelSetManager; }
    public StorageEvents getStorageEvents() { return storageEvents; }
    public StorageManager getStorageManager() { return storageManager; }
    public ItemConfig getItemConfig() { return itemConfig; }
    public PlayerConfig getPlayerConfig() { return playerConfig; }
    public DmgNumberToggleManager getDmgNumberToggleManager() { return dmgNumberToggleManager; }
    public ManaCostTracker getManaTracker() { return manaTracker; }
    public ProjectileFriendlyFireListener getProjectileFriendlyFireListener() { return projectileFriendlyFireListener; }
    public FileConfiguration getBossConfig() { return bossConfig; }
    public File getBossConfigFile() { return bossConfigFile; }
    public GemsManager getGemsManager() { return gemsManager; }
    public GemExchangeGUI getGemGui() { return gemGui; }
    public me.nakilex.levelplugin.enchanting.managers.EnchantManager getEnchantManager() { return enchantManager; }
    public me.nakilex.levelplugin.enchanting.gui.EnchantGUI getEnchantGUI() { return enchantGUI; }
    public TipsConfigManager getTipsCfg() { return tipsCfg; }
    public BroadcastManager getBroadcastMgr() { return broadcastMgr; }
    public me.nakilex.levelplugin.quests.managers.QuestManager getQuestManager() { return questManager; }
    public me.nakilex.levelplugin.npc.dialog.NPCDialogManager getDialogManager() { return dialogManager; }
    public me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public me.nakilex.levelplugin.quests.managers.BeaconManager getBeaconManager() { return beaconManager; }
    public me.nakilex.levelplugin.fasttravel.FastTravelManager getFastTravelManager() { return fastTravelManager; }
    public me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI getFastTravelGUI() { return fastTravelGUI; }
    public me.nakilex.levelplugin.motd.MotdManager getMotdManager() { return motdManager; }
    public me.nakilex.levelplugin.fakeblock.FakeBlockManager getFakeBlockManager() { return fakeBlockManager; }
    public me.nakilex.levelplugin.fakeblock.QuestGateManager getQuestGateManager() { return questGateManager; }
    public me.nakilex.levelplugin.fakeblock.ModelGateManager getModelGateManager() { return modelGateManager; }
    public me.nakilex.levelplugin.environment.EnvironmentManager getEnvironmentManager() { return environmentManager; }
    public me.nakilex.levelplugin.environment.UpgradeGUI getUpgradeGUI() { return upgradeGUI; }
    public me.nakilex.levelplugin.environment.BuildingUpgradeGUI getBuildingUpgradeGUI() { return buildingUpgradeGUI; }
    public me.nakilex.levelplugin.environment.stage.TownStageManager getTownStageManager() { return townStageManager; }
    public me.nakilex.levelplugin.environment.stage.BuildingStageManager getBuildingStageManager() { return buildingStageManager; }
    public me.nakilex.levelplugin.leaderboards.LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public me.nakilex.levelplugin.leaderboards.DuelStatsManager getDuelStatsManager() { return duelStatsManager; }
    public ChestHologramListener getChestHologramListener() { return chestHologramListener; }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseManager getAuctionHouseManager() { return auctionHouseManager; }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI getAuctionHouseGUI() { return auctionHouseGUI; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public SettingsGUI getSettingsGUI() { return settingsGUI; }
    public MeteorListener getMeteorListener() { return meteorListener; }
    public me.nakilex.levelplugin.cutscene.CutsceneManager getCutsceneManager() { return cutsceneManager; }
    public me.nakilex.levelplugin.calendar.CalendarManager getCalendarManager() { return calendarManager; }

    private void createCustomConfig() {
        customConfigFile = new File(plugin.getDataFolder(), "config.yml");
        if (!customConfigFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

        // Initialize debug feature toggles with defaults
        if (!customConfig.contains("features.profiles")) {
            customConfig.set("features.profiles", true);
        }
        if (!customConfig.contains("features.environment")) {
            customConfig.set("features.environment", true);
        }
        if (!customConfig.contains("features.trade")) {
            customConfig.set("features.trade", true);
        }
        if (!customConfig.contains("features.auction-house")) {
            customConfig.set("features.auction-house", true);
        }
        if (!customConfig.contains("features.quests")) {
            customConfig.set("features.quests", true);
        }
        if (!customConfig.contains("debug.chunk-loading")) {
            customConfig.set("debug.chunk-loading", false);
        }
        // MOTD, tips and leaderboard defaults are defined in config.yml
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
