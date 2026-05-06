package me.nakilex.levelplugin.core;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.blacksmith.managers.ItemRepairManager;
import me.nakilex.levelplugin.blacksmith.managers.ItemUpgradeManager;
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.booster.GlobalBoosterManager;
import me.nakilex.levelplugin.economy.gui.GemExchangeGUI;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.arena.ArenaMode;
import me.nakilex.levelplugin.arena.ArenaQueueManager;
import me.nakilex.levelplugin.arena.match.ArenaCombatTracker;
import me.nakilex.levelplugin.arena.match.ArenaMatchManager;
import me.nakilex.levelplugin.arena.match.ArenaTeamMatchManager;
import me.nakilex.levelplugin.arena.rating.ArenaRatingManager;
import me.nakilex.levelplugin.arena.gui.ArenaQueueGUI;
import me.nakilex.levelplugin.arena.instance.ArenaInstanceManager;
import me.nakilex.levelplugin.stronghold.StrongholdQueueManager;
import me.nakilex.levelplugin.stronghold.StrongholdShrineManager;
import me.nakilex.levelplugin.stronghold.StrongholdStartupProfiler;
import me.nakilex.levelplugin.stronghold.gui.StrongholdQueueGUI;
import me.nakilex.levelplugin.stronghold.run.StrongholdRunManager;
import org.bukkit.scheduler.BukkitTask;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.horse.managers.HorseConfigManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.items.config.ItemConfig;
import me.nakilex.levelplugin.items.gui.ItemsBrowser;
import me.nakilex.levelplugin.blacksmith.gui.RerollBrowser;
import me.nakilex.levelplugin.potions.gui.PotionBrowser;
import me.nakilex.levelplugin.items.listeners.PickupCustomItemListener;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.v2.ItemRegistry;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceUpgradeGUI;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.lootchests.managers.CooldownManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.dps.DpsDummyManager;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.party.PartyGlowManager;
import me.nakilex.levelplugin.friend.FriendManager;
import me.nakilex.levelplugin.friend.FriendGlowManager;
import me.nakilex.levelplugin.friend.PlayerVisibilityManager;
import me.nakilex.levelplugin.codex.*;
import me.nakilex.levelplugin.cursormenu.CursorMenuManager;
import me.nakilex.levelplugin.utils.BlockGlowUtil;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager;
import me.nakilex.levelplugin.friend.IgnoreManager;
import me.nakilex.levelplugin.friend.FriendRequestListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.pathfinding.PathfindingManager;
import me.nakilex.levelplugin.chat.ChatManager;
import me.nakilex.levelplugin.pathfinding.MercenaryManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.settings.gui.PersonalEnvironmentSettingsGUI;
import me.nakilex.levelplugin.spells.gui.SpellKeybindGUI;
import me.nakilex.levelplugin.spells.gui.SpellUpgradeGUI;
import me.nakilex.levelplugin.spells.SpellCatalog;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.settings.environment.PersonalEnvironmentKeys;
import me.nakilex.levelplugin.settings.environment.PlayerEnvironmentService;
import me.nakilex.levelplugin.settings.environment.PlayerPreferenceService;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.tips.BroadcastManager;
import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.battlepass.gui.BattlePassGUI;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.utils.LevelPlaceholderExpansion;
import me.nakilex.levelplugin.utils.NakiPlaceholderExpansion;
import me.nakilex.levelplugin.utils.EntityTextDisplay;
import me.nakilex.levelplugin.utils.MetadataTrait;
import me.nakilex.levelplugin.utils.HologramUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.utils.registeries.CommandRegistry;
import me.nakilex.levelplugin.utils.registeries.ListenerRegistry;
import me.nakilex.levelplugin.utils.registeries.TaskRegistry;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NpcRegistry;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PluginBootstrap {
    private final Main plugin;

    private LevelManager levelManager;
    private EconomyManager economyManager;
    private ItemManager itemManager;
    private ItemRegistry itemRegistryV2;
    private ItemUpgradeManager itemUpgradeManager;
    private ItemRepairManager itemRepairManager;
    private me.nakilex.levelplugin.items.tools.ToolManager toolManager;
    private me.nakilex.levelplugin.player.mining.managers.MiningManager miningManager;
    private me.nakilex.levelplugin.player.farming.managers.FarmingManager farmingManager;
    private me.nakilex.levelplugin.player.fishing.managers.FishingManager fishingManager;
    private me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager woodcuttingManager;
    private me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager lifeSkillRewardManager;
    private me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig miningRewardsConfig;
    private me.nakilex.levelplugin.player.farming.config.FarmingRewardsConfig farmingRewardsConfig;
    private me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig fishingRewardsConfig;
    private me.nakilex.levelplugin.player.woodcutting.config.WoodcuttingConfig woodcuttingConfig;
    private GlobalBoosterManager boosterManager;
    private HorseManager horseManager;
    private PartyManager partyManager;
    private ArenaQueueManager arenaQueueManager;
    private ArenaRatingManager arenaRatingManager;
    private ArenaMatchManager arenaMatchManager;
    private ArenaTeamMatchManager arenaTeamMatchManager;
    private ArenaCombatTracker arenaCombatTracker;
    private ArenaQueueGUI arenaQueueGUI;
    private ArenaInstanceManager arenaInstanceManager;
    private me.nakilex.levelplugin.stageddungeon.StagedDungeonManager stagedDungeonManager;
    private me.nakilex.levelplugin.stageddungeon.StagedDungeonGUI gemDungeonGUI;
    private StrongholdQueueManager strongholdQueueManager;
    private StrongholdQueueGUI strongholdQueueGUI;
    private StrongholdShrineManager strongholdShrineManager;
    private StrongholdRunManager strongholdRunManager;
    private BukkitTask strongholdQueueTickTask;
    private me.nakilex.levelplugin.guild.GuildManager guildManager;
    private me.nakilex.levelplugin.guild.GuildGUI guildGUI;
    private me.nakilex.levelplugin.guild.GuildMemberGUI guildMemberGUI;
    private me.nakilex.levelplugin.guild.GuildSettingsGUI guildSettingsGUI;
    private me.nakilex.levelplugin.guild.GuildApplicantsGUI guildApplicantsGUI;
    private me.nakilex.levelplugin.guild.siege.GuildSiegeManager guildSiegeManager;
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
    private CustomMobManager customMobManager;
    private PetManager petManager;
    private me.nakilex.levelplugin.pet.gui.PetGUI petGUI;
    private me.nakilex.levelplugin.pet.gui.PetSettingsGUI petSettingsGUI;
    private me.nakilex.levelplugin.pet.gui.PetMergeGUI petMergeGUI;
    private me.nakilex.levelplugin.pet.gui.PetSummonGUI petSummonGUI;
    private me.nakilex.levelplugin.pet.summon.PetSummonManager petSummonManager;
    private StorageEvents storageEvents;
    private StorageManager storageManager;
    private me.nakilex.levelplugin.guild.GuildVaultManager guildVaultManager;
    private ItemConfig itemConfig;
    private PlayerConfig playerConfig;
    private PlayerToggleManager dmgNumberToggleManager;
    private PlayerToggleManager mobDebugToggleManager;
    private me.nakilex.levelplugin.debug.DropDebugManager dropDebugManager;
    private me.nakilex.levelplugin.debug.ArcSlashDebugManager arcSlashDebugManager;
    private me.nakilex.levelplugin.debug.gui.ArcSlashDebugGUI arcSlashDebugGUI;
    private me.nakilex.levelplugin.debug.BeaconEntityDebugManager beaconEntityDebugManager;
    private DpsDummyManager dpsDummyManager;
    private FileConfiguration bossConfig;
    private File bossConfigFile;
    private GemsManager gemsManager;
    private GemExchangeGUI gemGui;
    private me.nakilex.levelplugin.enchanting.managers.EnchantManager enchantManager;
    private me.nakilex.levelplugin.enchanting.gui.EnchantGUI enchantGUI;
    private TipsConfigManager tipsCfg;
    private BroadcastManager broadcastMgr;
    private me.nakilex.levelplugin.chat.games.ChatGameManager chatGameManager;
    private me.nakilex.levelplugin.quests.managers.QuestManager questManager;
    private BattlePassManager battlePassManager;
    private BattlePassGUI battlePassGUI;
    private me.nakilex.levelplugin.npc.dialog.NPCDialogManager dialogManager;
    private me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager scoreboardManager;
    private me.nakilex.levelplugin.quests.managers.BeaconManager beaconManager;
    private me.nakilex.levelplugin.fasttravel.FastTravelManager fastTravelManager;
    private me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI fastTravelGUI;
    private me.nakilex.levelplugin.music.LocationMusicManager locationMusicManager;
    private me.nakilex.levelplugin.dungeon.gui.DungeonListGUI dungeonListGUI;
    private me.nakilex.levelplugin.dungeon.gui.DungeonLeaveGUI dungeonLeaveGUI;
    private me.nakilex.levelplugin.motd.MotdManager motdManager;
    private me.nakilex.levelplugin.maintenance.MaintenanceManager maintenanceManager;
    private me.nakilex.levelplugin.calendar.CalendarManager calendarManager;
    private me.nakilex.levelplugin.cutscene.CutsceneManager cutsceneManager;
    private me.nakilex.levelplugin.fakeblock.FakeBlockManager fakeBlockManager;
    private me.nakilex.levelplugin.fakeblock.QuestGateManager questGateManager;
    private me.nakilex.levelplugin.fakeblock.ModelGateManager modelGateManager;
    private me.nakilex.levelplugin.dungeon.rating.DungeonRatingManager dungeonRatingManager;
    private me.nakilex.levelplugin.dungeon.DungeonManager dungeonManager;
    private me.nakilex.levelplugin.world.WorldManager worldManager;
    private me.nakilex.levelplugin.server.ServerSelectionManager serverSelectionManager;
    private me.nakilex.levelplugin.environment.EnvironmentManager environmentManager;
    private me.nakilex.levelplugin.environment.UpgradeGUI upgradeGUI;
    private me.nakilex.levelplugin.environment.BuildingUpgradeGUI buildingUpgradeGUI;
    private me.nakilex.levelplugin.environment.stage.TownStageManager townStageManager;
    private me.nakilex.levelplugin.environment.stage.BuildingStageManager buildingStageManager;
    private me.nakilex.levelplugin.leaderboards.LeaderboardManager leaderboardManager;
    private me.nakilex.levelplugin.leaderboards.DuelStatsManager duelStatsManager;
    private me.nakilex.levelplugin.animatedlb.LeaderboardManager animatedLbManager;
    private final Map<UUID, List<NPC>> activeBowDrones = new HashMap<>();
    private me.nakilex.levelplugin.auctionhouse.AuctionHouseManager auctionHouseManager;
    private me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI auctionHouseGUI;
    private SettingsManager settingsManager;
    private SettingsGUI settingsGUI;
    private PersonalEnvironmentSettingsGUI personalEnvironmentSettingsGUI;
    private PlayerPreferenceService playerPreferenceService;
    private PlayerEnvironmentService playerEnvironmentService;
    private SpellKeybindGUI spellKeybindGUI;
    private SpellUpgradeGUI spellUpgradeGUI;
    private me.nakilex.levelplugin.debug.gui.DebugGUI debugGUI;
    private CodexManager codexManager;
    private me.nakilex.levelplugin.codex.mastery.CodexMasteryManager codexMasteryManager;
    private CodexMainGUI codexGUI;
    private MobCodexGUI mobCodexGUI;
    private NpcCodexGUI npcCodexGUI;
    private LocationCodexGUI locationCodexGUI;
    private me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager wanderingMerchantManager;
    private PathfindingManager pathfindingManager;
    private MercenaryManager mercenaryManager;
    private me.nakilex.levelplugin.mercenary.MercenaryAffinityManager mercenaryAffinityManager;
    private me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager mercenaryExpeditionManager;
    private me.nakilex.levelplugin.mercenary.board.ExpeditionBoardManager expeditionBoardManager;
    private me.nakilex.levelplugin.mercenary.gui.MercenaryGiftBrowserGUI mercenaryGiftBrowserGUI;
    private me.nakilex.levelplugin.mercenary.gui.MercenaryFriendshipGUI mercenaryFriendshipGUI;
    private me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionGUI mercenaryExpeditionGUI;
    private me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI mercenaryExpeditionRewardsGUI;
    private me.nakilex.levelplugin.pathfinding.DungeonExpeditionManager dungeonExpeditionManager;
    private me.nakilex.levelplugin.transmog.TransmogManager transmogManager;
    private me.nakilex.levelplugin.catacombs.CatacombsManager catacombsManager;
    private me.nakilex.levelplugin.catacombs.CatacombsGUI catacombsGUI;
    private me.nakilex.levelplugin.nexo.FurnitureGuiMapper furnitureGuiMapper;
    private CursorMenuManager cursorMenuManager;
    private BlockGlowUtil blockGlowUtil;

    public PluginBootstrap(Main plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!validateDependencies()) {
            plugin.getLogger().severe("Missing required dependencies. Disabling plugin..");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        initializePacketEvents();
        loadConfigFiles();
        setupCustomConfig();
        playerConfig = new PlayerConfig(plugin);
        initializeManagers();
        HologramUtil.removeMobHolograms();
        playerConfig.loadAllPlayers();
        itemConfig = new ItemConfig(plugin);
        itemConfig.loadItems();
        storageEvents = new StorageEvents();
        plugin.getServer().getPluginManager().registerEvents(storageEvents, plugin);
        environmentManager = new me.nakilex.levelplugin.environment.EnvironmentManager(playerConfig, townStageManager, buildingStageManager);
        upgradeGUI = new me.nakilex.levelplugin.environment.UpgradeGUI(environmentManager);
        buildingUpgradeGUI = new me.nakilex.levelplugin.environment.BuildingUpgradeGUI(environmentManager);
        leaderboardManager = new me.nakilex.levelplugin.leaderboards.LeaderboardManager(
                plugin,
                economyManager,
                playerConfig,
                duelStatsManager,
                settingsManager,
                environmentManager);
        CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(MetadataTrait.class).withName("MetadataTrait"));
        mobRewardsConfig = new MobRewardsConfig(plugin);
        customMobManager = new CustomMobManager(plugin);
        petManager = new PetManager(plugin);
        GuildQuestManager.getInstance().reloadMobCategories();
        codexManager = new CodexManager(playerConfig, customMobManager, mobRewardsConfig, bossConfig);
        codexMasteryManager = new me.nakilex.levelplugin.codex.mastery.CodexMasteryManager(economyManager);
        mobCodexGUI = new MobCodexGUI(codexManager, null);
        npcCodexGUI = new NpcCodexGUI(plugin, codexManager, null, mercenaryAffinityManager, mercenaryFriendshipGUI);
        locationCodexGUI = new LocationCodexGUI(codexManager, null);
        codexGUI = new CodexMainGUI(mobCodexGUI, npcCodexGUI, locationCodexGUI);
        mobCodexGUI.setMainGui(codexGUI);
        npcCodexGUI.setMainGui(codexGUI);
        locationCodexGUI.setMainGui(codexGUI);
        registerCommandsAndListeners();
        registerPlaceholders();
        me.nakilex.levelplugin.transmog.gui.TransmogBrowser tBrowser =
                new me.nakilex.levelplugin.transmog.gui.TransmogBrowser(plugin, transmogManager);
        new me.nakilex.levelplugin.transmog.gui.TransmogGUI(plugin, transmogManager, tBrowser);
        new ItemsBrowser(plugin);
        new me.nakilex.levelplugin.items.v2.gui.ItemFactoryGUI(plugin);
        new me.nakilex.levelplugin.items.tools.gui.ToolBrowser(plugin);
        new RerollBrowser(plugin);
        new me.nakilex.levelplugin.potions.gui.PotionBrowser(plugin, potionManager);
        if (leaderboardManager != null) {
            leaderboardManager.addAll();
        }
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> ModelEngineUtil.warmupModelAnimations(plugin),
                40L);
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
        // World-dependent managers like gates or fast travel require target
        // worlds to be loaded. Ensure the necessary worlds are available
        // before other managers are initialized.
        worldManager = new me.nakilex.levelplugin.world.WorldManager(plugin);
        me.nakilex.levelplugin.debug.StrongholdDebugGenerator.cleanupGeneratedWorlds(plugin);
        String hubWorld = customConfig != null
                ? customConfig.getString("server.hub-world", "hub")
                : "hub";
        worldManager.ensureWorldsLoaded("flatland", "redrocks", hubWorld);
        NpcApi.initialize(new NpcRegistry(plugin));
        serverSelectionManager = new me.nakilex.levelplugin.server.ServerSelectionManager(plugin);

        itemManager = new ItemManager(plugin);
        itemRegistryV2 = new ItemRegistry(plugin);
        itemRegistryV2.load();
        toolManager = new me.nakilex.levelplugin.items.tools.ToolManager();
        configManager = new ConfigManager(plugin);
        cooldownManager = new CooldownManager(plugin, configManager, null);
        lootChestManager = new LootChestManager(plugin, configManager, cooldownManager, potionManager);
        dmgNumberToggleManager = new PlayerToggleManager();
        mobDebugToggleManager = new PlayerToggleManager();
        dropDebugManager = new me.nakilex.levelplugin.debug.DropDebugManager(plugin);
        beaconEntityDebugManager = new me.nakilex.levelplugin.debug.BeaconEntityDebugManager(plugin);
        dpsDummyManager = new DpsDummyManager(plugin);
        upgradeKey = new NamespacedKey(plugin, "upgrade_level");
        levelManager = new LevelManager(plugin);
        miningManager = new me.nakilex.levelplugin.player.mining.managers.MiningManager(plugin);
        farmingManager = new me.nakilex.levelplugin.player.farming.managers.FarmingManager(plugin);
        fishingManager = new me.nakilex.levelplugin.player.fishing.managers.FishingManager(plugin);
        woodcuttingManager = new me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager(plugin);
        miningRewardsConfig = new me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig(plugin);
        farmingRewardsConfig = new me.nakilex.levelplugin.player.farming.config.FarmingRewardsConfig(plugin);
        fishingRewardsConfig = new me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig(plugin);
        woodcuttingConfig = new me.nakilex.levelplugin.player.woodcutting.config.WoodcuttingConfig(plugin);
        boolean boosterSystemEnabled = customConfig.getBoolean("features.booster-system", false);
        if (boosterSystemEnabled) {
            boosterManager = new GlobalBoosterManager(plugin, 2.0);
        } else {
            plugin.getLogger().info("Booster system is archived and will not be initialized.");
        }
        economyManager = new EconomyManager(plugin);
        lifeSkillRewardManager = new me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager(plugin);
        itemUpgradeManager = new ItemUpgradeManager(plugin);
        itemRepairManager = new ItemRepairManager();
        partyManager = new PartyManager();
        boolean arenaSystemEnabled = customConfig.getBoolean("features.arena-system", false);
        if (arenaSystemEnabled) {
            arenaRatingManager = new ArenaRatingManager(playerConfig);
            arenaQueueManager = new ArenaQueueManager(arenaRatingManager, partyManager);
            arenaQueueGUI = new ArenaQueueGUI(arenaQueueManager, arenaRatingManager);
        } else {
            plugin.getLogger().info("Arena system is archived and will not be initialized.");
        }
        strongholdQueueManager = new StrongholdQueueManager(partyManager);
        strongholdQueueGUI = new StrongholdQueueGUI(strongholdQueueManager);
        strongholdShrineManager = new StrongholdShrineManager(plugin);
        strongholdRunManager = new StrongholdRunManager(plugin, strongholdShrineManager);
        arenaInstanceManager = new ArenaInstanceManager(plugin);
        stagedDungeonManager = new me.nakilex.levelplugin.stageddungeon.StagedDungeonManager(plugin, arenaInstanceManager);
        gemDungeonGUI = stagedDungeonManager.getDefinition("gem")
                .map(definition -> new me.nakilex.levelplugin.stageddungeon.StagedDungeonGUI(stagedDungeonManager, definition))
                .orElse(null);
        friendManager = new FriendManager();
        guildManager = me.nakilex.levelplugin.guild.GuildManager.getInstance();
        guildManager.init(plugin);
        ChatManager.init(partyManager, guildManager);
        guildGUI = new me.nakilex.levelplugin.guild.GuildGUI(guildManager);
        guildApplicantsGUI = new me.nakilex.levelplugin.guild.GuildApplicantsGUI(guildManager);
        guildSettingsGUI = new me.nakilex.levelplugin.guild.GuildSettingsGUI(guildManager);
        guildMemberGUI = new me.nakilex.levelplugin.guild.GuildMemberGUI(guildManager, guildGUI, guildApplicantsGUI, guildSettingsGUI);
        guildApplicantsGUI.setMemberGUI(guildMemberGUI);
        guildSettingsGUI.setMemberGUI(guildMemberGUI);
        guildSiegeManager = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance();
        guildSiegeManager.init(plugin);
        gemsManager = new GemsManager();
        gemGui = new GemExchangeGUI(plugin, gemsManager);
        auctionHouseManager = new me.nakilex.levelplugin.auctionhouse.AuctionHouseManager(plugin, economyManager);
        auctionHouseGUI = new me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI(plugin, auctionHouseManager, economyManager);
        tipsCfg = new TipsConfigManager(plugin);
        broadcastMgr = new BroadcastManager(plugin, this.tipsCfg);
        broadcastMgr.start();
        settingsManager = new SettingsManager();
        PersonalEnvironmentKeys.init(plugin);
        playerPreferenceService = new PlayerPreferenceService();
        playerEnvironmentService = new PlayerEnvironmentService(playerPreferenceService);
        SpellCatalog.registerDefaults(plugin);
        questManager = new QuestManager(plugin, partyManager);
        battlePassManager = new BattlePassManager(plugin, questManager, itemManager);
        battlePassGUI = battlePassManager.getGui();
        dialogManager = new me.nakilex.levelplugin.npc.dialog.NPCDialogManager(plugin);
        scoreboardManager = new me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager(plugin, partyManager, questManager, arenaQueueManager, arenaRatingManager);
        if (arenaQueueManager != null) {
            arenaQueueManager.setScoreboardManager(scoreboardManager);
        }
        scoreboardManager.setStrongholdQueueManager(strongholdQueueManager);
        scoreboardManager.setStrongholdRunManager(strongholdRunManager);
        scoreboardManager.setStagedDungeonManager(stagedDungeonManager);
        if (arenaSystemEnabled && arenaQueueManager != null && arenaRatingManager != null && arenaInstanceManager != null && arenaQueueGUI != null) {
            arenaCombatTracker = new ArenaCombatTracker();
            arenaMatchManager = new ArenaMatchManager(plugin, arenaQueueManager, arenaInstanceManager, arenaRatingManager, scoreboardManager, arenaCombatTracker);
            arenaTeamMatchManager = new ArenaTeamMatchManager(plugin, arenaQueueManager, arenaInstanceManager, arenaRatingManager, scoreboardManager, arenaCombatTracker);
            arenaQueueManager.setMatchCheck(arenaMatchManager::isInMatch);
            arenaQueueManager.addMatchCheck(arenaTeamMatchManager::isInMatch);
            arenaQueueManager.setMatchHandler(ArenaMode.ONE_VS_ONE, arenaMatchManager::startMatch);
            arenaQueueManager.setMatchHandler(ArenaMode.TWO_VS_TWO, arenaTeamMatchManager::startMatch);
            arenaQueueManager.setQueueUpdateListener(arenaQueueGUI::refresh);
        }
        strongholdQueueManager.setQueueUpdateListener(() -> {
            strongholdQueueGUI.refresh();
            if (scoreboardManager != null) {
                scoreboardManager.updateAll();
            }
        });
        strongholdQueueManager.setSoloStartHandler(request -> {
            org.bukkit.entity.Player soloPlayer = Bukkit.getPlayer(request.playerId());
            if (soloPlayer == null || !soloPlayer.isOnline()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                StrongholdStartupProfiler profiler = StrongholdStartupProfiler.startOrContinue(plugin, soloPlayer);
                long stepStart = profiler == null ? 0L : profiler.stepStarted("Capture return location");
                strongholdRunManager.captureReturnLocation(soloPlayer);
                if (profiler != null) {
                    profiler.stepFinished("Capture return location", stepStart);
                }
                stepStart = profiler == null ? 0L : profiler.stepStarted("Generate stronghold world/templates");
                boolean started = me.nakilex.levelplugin.debug.StrongholdDebugGenerator.generateTest(soloPlayer);
                if (profiler != null) {
                    profiler.stepFinished("Generate stronghold world/templates", stepStart);
                }
                if (!started) {
                    me.nakilex.levelplugin.utils.ChatMessageUtil.send(
                            soloPlayer,
                            me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType.ERROR,
                            "Failed to start a solo Stronghold run.");
                    return;
                }
                final long waitStart = profiler == null ? 0L : profiler.stepStarted("Wait before wave startup (40 ticks)");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (profiler != null) {
                        profiler.stepFinished("Wait before wave startup (40 ticks)", waitStart);
                    }
                    strongholdRunManager.startSoloRun(soloPlayer, strongholdRunManager.consumeQueuedStartingStage(soloPlayer), profiler);
                }, 40L);
            });
        });
        strongholdQueueTickTask = Bukkit.getScheduler().runTaskTimer(plugin, strongholdQueueManager::tick, 20L, 20L);
        calendarManager = new me.nakilex.levelplugin.calendar.CalendarManager(plugin);
        duelStatsManager = new me.nakilex.levelplugin.leaderboards.DuelStatsManager(plugin);
        animatedLbManager = new me.nakilex.levelplugin.animatedlb.LeaderboardManager(plugin);
        partyGlowManager = new PartyGlowManager(plugin, partyManager, scoreboardManager::getBoard);
        friendGlowManager = new FriendGlowManager(plugin, friendManager, scoreboardManager::getBoard);
        visibilityManager = new PlayerVisibilityManager(plugin, friendManager, settingsManager);
        ignoreManager = new IgnoreManager(plugin);
        friendRequestListener = new FriendRequestListener(friendManager);
        beaconManager = new me.nakilex.levelplugin.quests.managers.BeaconManager();
        fastTravelManager = new me.nakilex.levelplugin.fasttravel.FastTravelManager(plugin);
        modelGateManager = new me.nakilex.levelplugin.fakeblock.ModelGateManager(plugin);
        fastTravelGUI = new me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI(fastTravelManager, economyManager, modelGateManager);
        locationMusicManager = new me.nakilex.levelplugin.music.LocationMusicManager();
        maintenanceManager = new me.nakilex.levelplugin.maintenance.MaintenanceManager(plugin);
        motdManager = new me.nakilex.levelplugin.motd.MotdManager(plugin);
        fakeBlockManager = new me.nakilex.levelplugin.fakeblock.FakeBlockManager();
        questGateManager = new me.nakilex.levelplugin.fakeblock.QuestGateManager(plugin, fakeBlockManager);
        dungeonRatingManager = new me.nakilex.levelplugin.dungeon.rating.DungeonRatingManager(plugin);
        dungeonManager = new me.nakilex.levelplugin.dungeon.DungeonManager(plugin, lootChestManager);
        dungeonManager.cleanupOldInstanceWorlds();
        dungeonManager.getBuilder().cleanupOrphans();
        dungeonListGUI = new me.nakilex.levelplugin.dungeon.gui.DungeonListGUI(dungeonManager);
        dungeonLeaveGUI = new me.nakilex.levelplugin.dungeon.gui.DungeonLeaveGUI(dungeonManager);
        catacombsManager = new me.nakilex.levelplugin.catacombs.CatacombsManager(plugin, dungeonManager);
        scoreboardManager.setCatacombsManager(catacombsManager);
        catacombsGUI = new me.nakilex.levelplugin.catacombs.CatacombsGUI(catacombsManager);
        plugin.getServer().getPluginManager().registerEvents(catacombsGUI, plugin);
        townStageManager = new me.nakilex.levelplugin.environment.stage.TownStageManager(plugin);
        buildingStageManager = new me.nakilex.levelplugin.environment.stage.BuildingStageManager(plugin);
        cooldownManager.setLootChestManager(lootChestManager);
        enchantManager = new me.nakilex.levelplugin.enchanting.managers.EnchantManager();
        enchantGUI = new me.nakilex.levelplugin.enchanting.gui.EnchantGUI(enchantManager, economyManager);
        StatsManager.getInstance().setLevelManager(levelManager);
        chatGameManager = new ChatGameManager(plugin, economyManager, levelManager, StatsManager.getInstance());
        modelSetManager = new me.nakilex.levelplugin.mob.config.ModelSetManager(plugin);
        transmogManager = new me.nakilex.levelplugin.transmog.TransmogManager(plugin, modelSetManager);
        cutsceneManager = new me.nakilex.levelplugin.cutscene.CutsceneManager(plugin);
        cutsceneManager.loadCutscenes();
        wanderingMerchantManager = new me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager(plugin);
        pathfindingManager = new PathfindingManager(plugin);
        mercenaryManager = new MercenaryManager(plugin);
        dungeonExpeditionManager = new me.nakilex.levelplugin.pathfinding.DungeonExpeditionManager(
                plugin,
                dungeonManager,
                mercenaryAffinityManager);
        mercenaryAffinityManager = new me.nakilex.levelplugin.mercenary.MercenaryAffinityManager(plugin);
        mercenaryExpeditionManager = new me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager(
                plugin,
                mercenaryAffinityManager,
                dungeonManager,
                economyManager,
                lootChestManager);
        expeditionBoardManager = new me.nakilex.levelplugin.mercenary.board.ExpeditionBoardManager(plugin);
        mercenaryGiftBrowserGUI = new me.nakilex.levelplugin.mercenary.gui.MercenaryGiftBrowserGUI(plugin, mercenaryAffinityManager);
        mercenaryFriendshipGUI = new me.nakilex.levelplugin.mercenary.gui.MercenaryFriendshipGUI(plugin, mercenaryAffinityManager);
        mercenaryExpeditionRewardsGUI = new me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI(plugin, mercenaryExpeditionManager);
        mercenaryExpeditionGUI = new me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionGUI(plugin, mercenaryAffinityManager, mercenaryExpeditionManager, mercenaryFriendshipGUI, mercenaryExpeditionRewardsGUI);
        mercenaryFriendshipGUI.setExpeditionGUI(mercenaryExpeditionGUI);
        blockGlowUtil = new BlockGlowUtil(plugin);
        cursorMenuManager = new CursorMenuManager(plugin);
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
        personalEnvironmentSettingsGUI = new PersonalEnvironmentSettingsGUI(playerEnvironmentService);
        personalEnvironmentSettingsGUI.setSettingsGUI(settingsGUI);
        settingsGUI.setPersonalEnvironmentSettingsGUI(personalEnvironmentSettingsGUI);
        spellKeybindGUI = new SpellKeybindGUI(settingsManager, settingsGUI);
        spellUpgradeGUI = new SpellUpgradeGUI();
        settingsGUI.setSpellKeybindGUI(spellKeybindGUI);
        settingsGUI.setSpellUpgradeGUI(spellUpgradeGUI);
        debugGUI = new me.nakilex.levelplugin.debug.gui.DebugGUI(
                mobDebugToggleManager,
                scoreboardManager,
                chatGameManager,
                mercenaryExpeditionManager,
                dropDebugManager,
                lootChestManager.getCooldownManager());
        arcSlashDebugManager = new me.nakilex.levelplugin.debug.ArcSlashDebugManager(plugin);
        arcSlashDebugGUI = new me.nakilex.levelplugin.debug.gui.ArcSlashDebugGUI(arcSlashDebugManager);
        petSettingsGUI = new me.nakilex.levelplugin.pet.gui.PetSettingsGUI(petManager);
        petGUI = new me.nakilex.levelplugin.pet.gui.PetGUI(petManager, petSettingsGUI);
        petMergeGUI = new me.nakilex.levelplugin.pet.gui.PetMergeGUI(petManager);
        petGUI.setPetMergeGUI(petMergeGUI);
        petMergeGUI.setPetGUI(petGUI);
        petSettingsGUI.setPetGUI(petGUI);
        petSummonManager = new me.nakilex.levelplugin.pet.summon.PetSummonManager(plugin, petManager, plugin.getCutsceneManager());
        petSummonGUI = new me.nakilex.levelplugin.pet.gui.PetSummonGUI(petSummonManager);
        petSummonManager.setSummonGUI(petSummonGUI);
        this.storageManager = new StorageManager();
        this.guildVaultManager = new me.nakilex.levelplugin.guild.GuildVaultManager(storageEvents, guildMemberGUI);
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
            mobDebugToggleManager,
            settingsGUI,
            spellUpgradeGUI,
            debugGUI,
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
            upgradeGUI,
            codexGUI,
            wanderingMerchantManager,
            pathfindingManager,
            mercenaryManager,
            battlePassManager,
            chatGameManager,
            dpsDummyManager,
            beaconEntityDebugManager,
            dungeonExpeditionManager,
            serverSelectionManager,
            petManager,
            petGUI,
            petSettingsGUI,
            petMergeGUI,
            petSummonGUI,
            customMobManager,
            arcSlashDebugManager,
            arcSlashDebugGUI
        );
        plugin.getCommand("pweather").setExecutor(new me.nakilex.levelplugin.settings.commands.PersonalWeatherCommand(playerEnvironmentService));
        plugin.getCommand("ptime").setExecutor(new me.nakilex.levelplugin.settings.commands.PersonalTimeCommand(playerEnvironmentService));
        me.nakilex.levelplugin.catacombs.CatacombsCommand catacombsCommand =
                new me.nakilex.levelplugin.catacombs.CatacombsCommand(catacombsManager, catacombsGUI);
        plugin.getCommand("catacombs").setExecutor(catacombsCommand);
        plugin.getCommand("catacombs").setTabCompleter(catacombsCommand);
        me.nakilex.levelplugin.mercenary.board.ExpeditionBoardCommand expeditionBoardCommand =
                new me.nakilex.levelplugin.mercenary.board.ExpeditionBoardCommand(expeditionBoardManager);
        plugin.getCommand("expeditionboard").setExecutor(expeditionBoardCommand);
        plugin.getCommand("expeditionboard").setTabCompleter(expeditionBoardCommand);
        me.nakilex.levelplugin.maintenance.MaintenanceCommand maintenanceCmd =
                new me.nakilex.levelplugin.maintenance.MaintenanceCommand(maintenanceManager);
        plugin.getCommand("maintenance").setExecutor(maintenanceCmd);
        plugin.getCommand("maintenance").setTabCompleter(maintenanceCmd);
        me.nakilex.levelplugin.animatedlb.AnimatedLeaderboardPlugin animatedLbCmd =
                new me.nakilex.levelplugin.animatedlb.AnimatedLeaderboardPlugin(animatedLbManager);
        plugin.getCommand("animatedlb").setExecutor(animatedLbCmd);
        plugin.getCommand("animatedlb").setTabCompleter(animatedLbCmd);
        if (gemDungeonGUI != null) {
            stagedDungeonManager.getDefinition("gem").ifPresent(definition ->
                    plugin.getCommand("gemdungeon").setExecutor(
                            new me.nakilex.levelplugin.stageddungeon.GemDungeonCommand(gemDungeonGUI, stagedDungeonManager, definition)));
        }

        me.nakilex.levelplugin.guild.siege.GuildSiegeCommand siegeCmd =
                new me.nakilex.levelplugin.guild.siege.GuildSiegeCommand(guildSiegeManager);
        plugin.getCommand("siege").setExecutor(siegeCmd);
        plugin.getCommand("siege").setTabCompleter(siegeCmd);

        furnitureGuiMapper = new me.nakilex.levelplugin.nexo.FurnitureGuiMapper();
        furnitureGuiMapper.register("quest_board", player -> mercenaryExpeditionGUI.open(player));
        boolean essenceSystemEnabled = me.nakilex.levelplugin.utils.FeatureFlagUtil.isEnabled("features.class-system", false)
                && me.nakilex.levelplugin.utils.FeatureFlagUtil.isEnabled("features.essence-system", false);
        if (essenceSystemEnabled) {
            furnitureGuiMapper.register("altar", player -> ClassEssenceUpgradeGUI.openInvest(player, null));
        }
        java.util.List.of(
                "portal_decoration_animated_v1_portal_1",
                "portal_decoration_animated_v1_portal_2",
                "portal_decoration_animated_v1_portal_3",
                "portal_decoration_animated_v1_portal_4",
                "portal_decoration_animated_v1_portal_5",
                "portal_decoration_animated_v1_portal_6",
                "portal_decoration_animated_v1_portal_7",
                "portal_decoration_animated_v1_portal_8",
                "portal_decoration_animated_v1_portal_9",
                "portal_decoration_animated_v1_portal_10"
        ).forEach(id -> furnitureGuiMapper.registerProximity(id, player -> {
            if ("world".equalsIgnoreCase(player.getWorld().getName())) {
                dungeonListGUI.open(player);
            } else {
                dungeonLeaveGUI.open(player);
            }
        }));
        plugin.getServer().getPluginManager().registerEvents(furnitureGuiMapper, plugin);
        plugin.getServer().getPluginManager().registerEvents(cursorMenuManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(blockGlowUtil, plugin);
        plugin.getServer().getPluginManager().registerEvents(strongholdQueueGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(strongholdShrineManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(strongholdRunManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(stagedDungeonManager, plugin);
        if (gemDungeonGUI != null) {
            plugin.getServer().getPluginManager().registerEvents(gemDungeonGUI, plugin);
        }

        ListenerRegistry.registerListeners(
            plugin,
            blacksmithGUI,
            horseGUI,
            lootChestManager,
            potionManager,
            partyManager,
            boosterManager,
            economyManager,
            mobRewardsConfig,
            dmgNumberToggleManager,
            mobDebugToggleManager,
            pickupCustomItemListener,
            settingsGUI,
            spellKeybindGUI,
            spellUpgradeGUI,
            debugGUI,
            guildGUI,
            bossConfig,
            gemsManager,
            enchantGUI,
            auctionHouseGUI,
            questManager,
            dialogManager,
            scoreboardManager,
            fastTravelManager,
            fastTravelGUI,
            dungeonListGUI,
            dungeonLeaveGUI,
            motdManager,
            upgradeGUI,
            buildingUpgradeGUI,
            new me.nakilex.levelplugin.environment.listeners.BuildingHologramListener(buildingUpgradeGUI),
            new me.nakilex.levelplugin.environment.listeners.StageBlockInteractListener(),
            codexGUI,
            mobCodexGUI,
            npcCodexGUI,
            locationCodexGUI,
            wanderingMerchantManager,
            arenaQueueGUI,
            arenaMatchManager,
            arenaTeamMatchManager,
            chatGameManager,
            dpsDummyManager,
            beaconEntityDebugManager,
            serverSelectionManager,
            petManager,
            petGUI,
            petSettingsGUI,
            petMergeGUI,
            petSummonGUI,
            petSummonManager,
            customMobManager,
            arcSlashDebugManager,
            arcSlashDebugGUI
        );
        plugin.getServer().getPluginManager().registerEvents(
                new me.nakilex.levelplugin.mercenary.board.ExpeditionBoardWandListener(expeditionBoardManager),
                plugin);
        plugin.getServer().getPluginManager().registerEvents(battlePassGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new me.nakilex.levelplugin.guild.siege.GuildSiegeListener(guildSiegeManager),
                plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new me.nakilex.levelplugin.guild.GuildMembershipListener(),
                plugin);
        plugin.getServer().getPluginManager().registerEvents(personalEnvironmentSettingsGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new me.nakilex.levelplugin.settings.listeners.PersonalEnvironmentJoinListener(plugin, playerEnvironmentService),
                plugin);
        plugin.getServer().getPluginManager().registerEvents(beaconManager, plugin);
        if (chatGameManager != null) {
            chatGameManager.start();
        }
        TaskRegistry.startTasks(plugin, horseConfigManager, horseManager, wanderingMerchantManager);
    }

    private void initializePacketEvents() {
        try {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
            PacketEvents.getAPI().load();
            PacketEvents.getAPI().init();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to initialize PacketEvents: " + ex.getMessage());
        }
    }

    private void shutdownPacketEvents() {
        try {
            if (PacketEvents.getAPI() != null) {
                PacketEvents.getAPI().terminate();
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to terminate PacketEvents cleanly: " + ex.getMessage());
        }
    }

    /**
     * Registers PlaceholderAPI expansions if the plugin is present.
     * Provided placeholders:
     * <ul>
     *   <li><code>%naki_level%</code></li>
     *   <li><code>%naki_class%</code></li>
     *   <li><code>%naki_coins%</code></li>
     *   <li><code>%naki_gems%</code></li>
     *   <li><code>%naki_currentMana%</code></li>
     *   <li><code>%naki_maxMana%</code></li>
     *   <li><code>%naki_currentXP%</code></li>
     *   <li><code>%naki_xpNextLevel%</code></li>
     *   <li><code>%naki_seasonDate%</code></li>
     *   <li><code>%level_spell_combo_active%</code></li>
     *   <li><code>%level_spell_combo_glyphs%</code></li>
     *   <li><code>%level_spell_combo_slot1%</code></li>
     *   <li><code>%level_spell_combo_slot2%</code></li>
     *   <li><code>%level_spell_combo_slot3%</code></li>
     * </ul>
     */
    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NakiPlaceholderExpansion(plugin).register();
            new LevelPlaceholderExpansion(plugin).register();
        }
    }

    private boolean validateDependencies() {
        return ensureDependency("Citizens", null);
    }

    private boolean ensureDependency(String pluginName, String requiredClassName) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled(pluginName)) {
            plugin.getLogger().severe(pluginName + " is installed but disabled! Check for errors.");
            return false;
        }

        if (requiredClassName == null) {
            return true;
        }

        try {
            Class.forName(requiredClassName);
            return true;
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().severe("Required class '" + requiredClassName + "' from " + pluginName + " is missing. Make sure the plugin is updated and loaded.");
            return false;
        }
    }

    public void disable() {
        TaskRegistry.stopTasks();
        if (chatGameManager != null) chatGameManager.stop();
        if (mercenaryManager != null) mercenaryManager.unbindAll();
        if (economyManager != null) economyManager.saveBalances();
        if (dealMaker != null) dealMaker.closeAllTrades();
        if (arenaQueueManager != null) arenaQueueManager.clear();
        if (strongholdQueueManager != null) strongholdQueueManager.clear();
        if (strongholdShrineManager != null) strongholdShrineManager.cleanup();
        if (strongholdRunManager != null) strongholdRunManager.stopAll();
        if (strongholdQueueTickTask != null) strongholdQueueTickTask.cancel();
        if (stagedDungeonManager != null) stagedDungeonManager.stopAll();
        if (arenaInstanceManager != null) arenaInstanceManager.cleanup();
        if (itemConfig != null) itemConfig.saveItems();
        if (guildManager != null) guildManager.save();
        if (playerConfig != null) {
            FileConfiguration cfg = plugin.getCustomConfig();
            boolean profilesEnabled = cfg == null || cfg.getBoolean("features.profiles", true);
            me.nakilex.levelplugin.player.profile.ProfileManager pm =
                    me.nakilex.levelplugin.player.profile.ProfileManager.getInstance();
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (profilesEnabled) {
                    pm.saveActiveProfile(p);
                } else {
                    pm.saveProfile(p, 0);
                }
            }
            playerConfig.saveAllPlayers();
        }
        if (storageManager != null) storageManager.saveAllStorages();
        if (guildVaultManager != null) guildVaultManager.saveAll();
        if (auctionHouseManager != null) auctionHouseManager.saveAuctionsSync();
        if (lootChestManager != null) lootChestManager.removeAllChests();
        if (dpsDummyManager != null) dpsDummyManager.shutdown();
        if (customMobManager != null) customMobManager.getSpawnerManager().shutdown();
        if (petManager != null) petManager.shutdown();
        if (horseManager != null) horseManager.shutdown();
        if (dungeonManager != null) {
            dungeonManager.cleanupInstances();
            dungeonManager.cleanupOldInstanceWorlds();
            dungeonManager.saveLayoutsSync();
            dungeonManager.getBuilder().cancelAll();
        }
        if (me.nakilex.levelplugin.player.mining.listeners.OreMiningListener.getInstance() != null) me.nakilex.levelplugin.player.mining.listeners.OreMiningListener.getInstance().removeAllHolograms();
        if (questManager != null) questManager.saveProgress();
        if (modelGateManager != null) modelGateManager.removeAllGates();
        if (environmentManager != null) {
            environmentManager.removeAllHolograms();
            environmentManager.saveAll();
        }
        MultiLineHologram.removeAll("farming_special_crop");
        HologramUtil.removeMobHolograms();
        EntityTextDisplay.removeAllDisplays();
        if (guildSiegeManager != null) {
            guildSiegeManager.cleanup();
            guildSiegeManager.save();
        }
        if (leaderboardManager != null) leaderboardManager.removeAll();
        if (animatedLbManager != null) animatedLbManager.remove();
        if (duelStatsManager != null) duelStatsManager.save();
        if (townStageManager != null) townStageManager.despawnAll();
        if (buildingStageManager != null) buildingStageManager.despawnAll();
        if (wanderingMerchantManager != null) wanderingMerchantManager.despawn();
        if (beaconManager != null) beaconManager.removeAll();
        if (beaconEntityDebugManager != null) beaconEntityDebugManager.removeAll();
        if (serverSelectionManager != null) serverSelectionManager.shutdown();
        if (cursorMenuManager != null) cursorMenuManager.shutdown();
        if (blockGlowUtil != null) blockGlowUtil.shutdown();
        if (worldManager != null) {
            me.nakilex.levelplugin.debug.StrongholdDebugGenerator.cleanupGeneratedWorlds(plugin);
        }
        if (dealMaker != null) dealMaker.closeAllTrades();
        shutdownPacketEvents();
        plugin.getLogger().info("LevelPlugin has been disabled!");
    }

    public Map<UUID, List<NPC>> getActiveBowDrones() { return activeBowDrones; }
    public LevelManager getLevelManager() { return levelManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public ItemManager getItemManager() { return itemManager; }
    public ItemRegistry getItemRegistryV2() { return itemRegistryV2; }
    public ItemUpgradeManager getItemUpgradeManager() { return itemUpgradeManager; }
    public ItemRepairManager getItemRepairManager() { return itemRepairManager; }
    public me.nakilex.levelplugin.items.tools.ToolManager getToolManager() { return toolManager; }
    public me.nakilex.levelplugin.player.mining.managers.MiningManager getMiningManager() { return miningManager; }
    public me.nakilex.levelplugin.player.farming.managers.FarmingManager getFarmingManager() { return farmingManager; }
    public me.nakilex.levelplugin.player.fishing.managers.FishingManager getFishingManager() { return fishingManager; }
    public me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager getWoodcuttingManager() { return woodcuttingManager; }
    public me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager getLifeSkillRewardManager() { return lifeSkillRewardManager; }
    public me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig getMiningRewardsConfig() { return miningRewardsConfig; }
    public me.nakilex.levelplugin.player.farming.config.FarmingRewardsConfig getFarmingRewardsConfig() { return farmingRewardsConfig; }
    public me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig getFishingRewardsConfig() { return fishingRewardsConfig; }
    public me.nakilex.levelplugin.player.woodcutting.config.WoodcuttingConfig getWoodcuttingConfig() { return woodcuttingConfig; }
    public GlobalBoosterManager getBoosterManager() { return boosterManager; }
    public HorseManager getHorseManager() { return horseManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public ArenaQueueManager getArenaQueueManager() { return arenaQueueManager; }
    public ArenaRatingManager getArenaRatingManager() { return arenaRatingManager; }
    public ArenaMatchManager getArenaMatchManager() { return arenaMatchManager; }
    public ArenaTeamMatchManager getArenaTeamMatchManager() { return arenaTeamMatchManager; }
    public ArenaQueueGUI getArenaQueueGUI() { return arenaQueueGUI; }
    public ArenaInstanceManager getArenaInstanceManager() { return arenaInstanceManager; }
    public me.nakilex.levelplugin.stageddungeon.StagedDungeonManager getStagedDungeonManager() { return stagedDungeonManager; }
    public StrongholdQueueManager getStrongholdQueueManager() { return strongholdQueueManager; }
    public StrongholdQueueGUI getStrongholdQueueGUI() { return strongholdQueueGUI; }
    public StrongholdShrineManager getStrongholdShrineManager() { return strongholdShrineManager; }
    public StrongholdRunManager getStrongholdRunManager() { return strongholdRunManager; }
    public me.nakilex.levelplugin.guild.GuildManager getGuildManager() { return guildManager; }
    public me.nakilex.levelplugin.guild.GuildGUI getGuildGUI() { return guildGUI; }
    public me.nakilex.levelplugin.guild.GuildMemberGUI getGuildMemberGUI() { return guildMemberGUI; }
    public me.nakilex.levelplugin.guild.GuildSettingsGUI getGuildSettingsGUI() { return guildSettingsGUI; }
    public me.nakilex.levelplugin.guild.GuildApplicantsGUI getGuildApplicantsGUI() { return guildApplicantsGUI; }
    public me.nakilex.levelplugin.guild.siege.GuildSiegeManager getGuildSiegeManager() { return guildSiegeManager; }
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
    public CustomMobManager getCustomMobManager() { return customMobManager; }
    public PetManager getPetManager() { return petManager; }
    public StorageEvents getStorageEvents() { return storageEvents; }
    public StorageManager getStorageManager() { return storageManager; }
    public me.nakilex.levelplugin.guild.GuildVaultManager getGuildVaultManager() { return guildVaultManager; }
    public ItemConfig getItemConfig() { return itemConfig; }
    public PlayerConfig getPlayerConfig() { return playerConfig; }
    public ChatGameManager getChatGameManager() { return chatGameManager; }
    public PlayerToggleManager getDmgNumberToggleManager() { return dmgNumberToggleManager; }
    public PlayerToggleManager getMobDebugToggleManager() { return mobDebugToggleManager; }
    public me.nakilex.levelplugin.debug.DropDebugManager getDropDebugManager() { return dropDebugManager; }
    public me.nakilex.levelplugin.debug.BeaconEntityDebugManager getBeaconEntityDebugManager() { return beaconEntityDebugManager; }
    public FileConfiguration getBossConfig() { return bossConfig; }
    public File getBossConfigFile() { return bossConfigFile; }
    public GemsManager getGemsManager() { return gemsManager; }
    public GemExchangeGUI getGemGui() { return gemGui; }
    public me.nakilex.levelplugin.enchanting.managers.EnchantManager getEnchantManager() { return enchantManager; }
    public me.nakilex.levelplugin.enchanting.gui.EnchantGUI getEnchantGUI() { return enchantGUI; }
    public TipsConfigManager getTipsCfg() { return tipsCfg; }

    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
            return;
        }
        try {
            customConfig.save(customConfigFile);
        } catch (java.io.IOException ex) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to save custom config!", ex);
        }
    }
    public BroadcastManager getBroadcastMgr() { return broadcastMgr; }
    public me.nakilex.levelplugin.quests.managers.QuestManager getQuestManager() { return questManager; }
    public BattlePassManager getBattlePassManager() { return battlePassManager; }
    public BattlePassGUI getBattlePassGUI() { return battlePassGUI; }
    public me.nakilex.levelplugin.npc.dialog.NPCDialogManager getDialogManager() { return dialogManager; }
    public me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public me.nakilex.levelplugin.quests.managers.BeaconManager getBeaconManager() { return beaconManager; }
    public me.nakilex.levelplugin.fasttravel.FastTravelManager getFastTravelManager() { return fastTravelManager; }
    public me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI getFastTravelGUI() { return fastTravelGUI; }
    public me.nakilex.levelplugin.music.LocationMusicManager getLocationMusicManager() { return locationMusicManager; }
    public me.nakilex.levelplugin.motd.MotdManager getMotdManager() { return motdManager; }
    public me.nakilex.levelplugin.maintenance.MaintenanceManager getMaintenanceManager() { return maintenanceManager; }
    public me.nakilex.levelplugin.fakeblock.FakeBlockManager getFakeBlockManager() { return fakeBlockManager; }
    public me.nakilex.levelplugin.fakeblock.QuestGateManager getQuestGateManager() { return questGateManager; }
    public me.nakilex.levelplugin.fakeblock.ModelGateManager getModelGateManager() { return modelGateManager; }
    public me.nakilex.levelplugin.dungeon.rating.DungeonRatingManager getDungeonRatingManager() { return dungeonRatingManager; }
    public me.nakilex.levelplugin.dungeon.DungeonManager getDungeonManager() { return dungeonManager; }
    public me.nakilex.levelplugin.catacombs.CatacombsManager getCatacombsManager() { return catacombsManager; }
    public me.nakilex.levelplugin.catacombs.CatacombsGUI getCatacombsGUI() { return catacombsGUI; }
    public me.nakilex.levelplugin.world.WorldManager getWorldManager() { return worldManager; }
    public me.nakilex.levelplugin.server.ServerSelectionManager getServerSelectionManager() { return serverSelectionManager; }
    public me.nakilex.levelplugin.environment.EnvironmentManager getEnvironmentManager() { return environmentManager; }
    public me.nakilex.levelplugin.environment.UpgradeGUI getUpgradeGUI() { return upgradeGUI; }
    public me.nakilex.levelplugin.environment.BuildingUpgradeGUI getBuildingUpgradeGUI() { return buildingUpgradeGUI; }
    public me.nakilex.levelplugin.environment.stage.TownStageManager getTownStageManager() { return townStageManager; }
    public me.nakilex.levelplugin.environment.stage.BuildingStageManager getBuildingStageManager() { return buildingStageManager; }
    public me.nakilex.levelplugin.leaderboards.LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public me.nakilex.levelplugin.leaderboards.DuelStatsManager getDuelStatsManager() { return duelStatsManager; }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseManager getAuctionHouseManager() { return auctionHouseManager; }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI getAuctionHouseGUI() { return auctionHouseGUI; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public SettingsGUI getSettingsGUI() { return settingsGUI; }
    public me.nakilex.levelplugin.debug.gui.DebugGUI getDebugGUI() { return debugGUI; }
    public me.nakilex.levelplugin.cutscene.CutsceneManager getCutsceneManager() { return cutsceneManager; }
    public me.nakilex.levelplugin.calendar.CalendarManager getCalendarManager() { return calendarManager; }
    public CodexManager getCodexManager() { return codexManager; }
    public me.nakilex.levelplugin.codex.mastery.CodexMasteryManager getCodexMasteryManager() { return codexMasteryManager; }
    public CodexMainGUI getCodexGUI() { return codexGUI; }
    public me.nakilex.levelplugin.dungeon.gui.DungeonListGUI getDungeonListGUI() { return dungeonListGUI; }
    public me.nakilex.levelplugin.dungeon.gui.DungeonLeaveGUI getDungeonLeaveGUI() { return dungeonLeaveGUI; }
    public me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager getWanderingMerchantManager() { return wanderingMerchantManager; }
    public PathfindingManager getPathfindingManager() { return pathfindingManager; }
    public MercenaryManager getMercenaryManager() { return mercenaryManager; }
    public me.nakilex.levelplugin.mercenary.MercenaryAffinityManager getMercenaryAffinityManager() { return mercenaryAffinityManager; }
    public me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager getMercenaryExpeditionManager() { return mercenaryExpeditionManager; }
    public me.nakilex.levelplugin.mercenary.board.ExpeditionBoardManager getExpeditionBoardManager() { return expeditionBoardManager; }
    public CursorMenuManager getCursorMenuManager() { return cursorMenuManager; }
    public BlockGlowUtil getBlockGlowUtil() { return blockGlowUtil; }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryGiftBrowserGUI getMercenaryGiftBrowserGUI() { return mercenaryGiftBrowserGUI; }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryFriendshipGUI getMercenaryFriendshipGUI() { return mercenaryFriendshipGUI; }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionGUI getMercenaryExpeditionGUI() { return mercenaryExpeditionGUI; }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI getMercenaryExpeditionRewardsGUI() { return mercenaryExpeditionRewardsGUI; }
    public me.nakilex.levelplugin.transmog.TransmogManager getTransmogManager() { return transmogManager; }

    public void reloadPluginConfig() {
        plugin.reloadConfig();
        createCustomConfig();
        configValues = new ConfigValues(this.customConfigFile);
        if (messageStrings != null) {
            messageStrings.reloadConfig();
        }
        me.nakilex.levelplugin.storage.data.StorageConfig storageConfig = me.nakilex.levelplugin.storage.data.StorageConfig.getInstance();
        if (storageConfig != null) {
            storageConfig.reloadConfig();
        }
        if (mobRewardsConfig != null) {
            mobRewardsConfig.reloadConfig();
            GuildQuestManager.getInstance().reloadMobCategories();
        }
        if (bossConfigFile != null && bossConfigFile.exists()) {
            bossConfig = YamlConfiguration.loadConfiguration(bossConfigFile);
        }
        if (codexManager != null && mobRewardsConfig != null) {
            codexManager.reload(mobRewardsConfig, bossConfig);
        }
        File potionsFile = new File(plugin.getDataFolder(), "potions.yml");
        if (potionManager != null && potionsFile.exists()) {
            FileConfiguration potionCfg = YamlConfiguration.loadConfiguration(potionsFile);
            potionManager.reload(potionCfg);
        }
        if (miningRewardsConfig != null) {
            miningRewardsConfig.reloadConfig();
        }
        if (farmingRewardsConfig != null) {
            farmingRewardsConfig.reloadConfig();
        }
        if (fishingRewardsConfig != null) {
            fishingRewardsConfig.reloadConfig();
        }
        if (woodcuttingConfig != null) {
            woodcuttingConfig.reload();
        }
        if (configManager != null) {
            configManager.reloadLootChestsConfig();
        }
        if (cooldownManager != null) {
            cooldownManager.reloadSettings();
        }
        if (lootChestManager != null) {
            lootChestManager.reloadFromConfig();
        }
        if (economyManager != null) {
            economyManager.loadBalances();
        }
        if (fastTravelManager != null) {
            fastTravelManager.reload();
        }
        if (worldManager != null) {
            worldManager.reload();
        }
        if (serverSelectionManager != null) {
            serverSelectionManager.reload();
        }
        if (pathfindingManager != null) {
            pathfindingManager.reload();
        }
        if (broadcastMgr != null) {
            broadcastMgr.start();
        }
        if (chatGameManager != null) {
            chatGameManager.reload();
        }
    }

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
        if (!customConfig.contains("features.booster-system")) {
            customConfig.set("features.booster-system", false);
        }
        if (!customConfig.contains("features.class-system")) {
            customConfig.set("features.class-system", false);
        }
        if (!customConfig.contains("features.arena-system")) {
            customConfig.set("features.arena-system", false);
        }
        if (!customConfig.contains("features.duel-system")) {
            customConfig.set("features.duel-system", false);
        }
        if (!customConfig.contains("debug.chunk-loading")) {
            customConfig.set("debug.chunk-loading", false);
        }
        if (!customConfig.contains("debug.mythic-skill-damage")) {
            customConfig.set("debug.mythic-skill-damage", false);
        }
        if (!customConfig.contains("debug.instant-expeditions")) {
            customConfig.set("debug.instant-expeditions", false);
        }
        if (!customConfig.contains("debug.beacon-entity")) {
            customConfig.set("debug.beacon-entity", false);
        }
        if (!customConfig.contains("debug.mob-gear-drop-rate")) {
            customConfig.set("debug.mob-gear-drop-rate", 10.0);
        }
        if (!customConfig.contains("chat-games.interval-minutes")) {
            customConfig.set("chat-games.interval-minutes", 15);
        }
        if (!customConfig.contains("server.hub-world")) {
            customConfig.set("server.hub-world", "hub");
        }
        if (!customConfig.contains("server.alpha-world")) {
            customConfig.set("server.alpha-world", "world");
        }
        if (!customConfig.contains("server.build-world")) {
            customConfig.set("server.build-world", "flatland");
        }
        if (!customConfig.contains("server.build-permission")) {
            customConfig.set("server.build-permission", "group.staff");
        }
        if (!customConfig.contains("server.build-min-weight")) {
            customConfig.set("server.build-min-weight", 51);
        }
        if (!customConfig.contains("stronghold.generated-world-template")) {
            customConfig.set("stronghold.generated-world-template", "");
        }
        java.util.List<String> excluded = customConfig.getStringList("levelplugin.excluded-worlds");
        if (excluded == null || excluded.isEmpty()) {
            excluded = new java.util.ArrayList<>(java.util.List.of("flatland"));
        } else {
            excluded = new java.util.ArrayList<>(excluded);
        }
        String buildWorld = customConfig.getString("server.build-world", "flatland");
        if (buildWorld != null && !buildWorld.isBlank()) {
            String lowered = buildWorld.toLowerCase(java.util.Locale.ROOT);
            boolean exists = excluded.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(lowered));
            if (!exists) {
                excluded.add(buildWorld);
            }
        }
        customConfig.set("levelplugin.excluded-worlds", excluded);
        if (!customConfig.contains("tips.delay")) {
            customConfig.set("tips.delay", 120);
        }
        if (!customConfig.contains("tips.messages")) {
            customConfig.set("tips.messages", Arrays.asList(
                    "You can type [item] to link what you're holding in chat!",
                    "Don\u2019t forget to spend your skill points!",
                    "You can trade &d<glyph:purple_orb_icon> &rat a Gem Exchanger.",
                    "You can change your class at anytime using /class",
                    "Type /settings to toggle different visual features",
                    "Every mobility spell scales with agility!",
                    "You can trade with others using /trade <username>",
                    "You can sell your unwanted items at a Scrapper for <glyph:coins_icon> &6coins & <glyph:purple_orb_icon> &dgems&f!"
            ));
        }
        if (!customConfig.contains("leaderboards.level.world")) {
            customConfig.set("leaderboards.level.world", "flatland");
            customConfig.set("leaderboards.level.x", 100);
            customConfig.set("leaderboards.level.y", -50);
            customConfig.set("leaderboards.level.z", 100);
            customConfig.set("leaderboards.level.type", "LEVEL");
            customConfig.set("leaderboards.duels.world", "flatland");
            customConfig.set("leaderboards.duels.x", 105);
            customConfig.set("leaderboards.duels.y", -50);
            customConfig.set("leaderboards.duels.z", 100);
            customConfig.set("leaderboards.duels.type", "DUELS");
            customConfig.set("leaderboards.balance.world", "flatland");
            customConfig.set("leaderboards.balance.x", 110);
            customConfig.set("leaderboards.balance.y", -50);
            customConfig.set("leaderboards.balance.z", 100);
            customConfig.set("leaderboards.balance.type", "BALANCE");
        }
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
