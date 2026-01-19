package me.nakilex.levelplugin;

import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.core.PluginBootstrap;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.config.ItemConfig;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.party.PartyGlowManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.DealMaker;
import me.nakilex.levelplugin.codex.CodexManager;
import me.nakilex.levelplugin.codex.CodexMainGUI;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.pathfinding.PathfindingManager;
import me.nakilex.levelplugin.pathfinding.MercenaryManager;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Main extends JavaPlugin {
    private static Main instance;
    private static Main plugin;
    private PluginBootstrap bootstrap;
    public static final String PREFIX = "";

    @Override
    public void onEnable() {
        instance = this;
        plugin = this;
        try {
            ParticleService.getInstance().initialize(ParticleNativeCore.loadAPI(this));
        } catch (Exception e) {
            getLogger().log(Level.SEVERE,
                    "Failed to initialize ParticleNativeAPI for server version "
                            + getServer().getBukkitVersion()
                            + ". Disabling LevelPlugin to avoid partial combat VFX.",
                    e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        bootstrap = new PluginBootstrap(this);
        bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) bootstrap.disable();
        ParticleService.getInstance().reset();
    }

    public static Main getInstance() { return instance; }
    public static Main getPlugin() { return plugin; }

    public PotionManager getPotionManager() { return bootstrap.getPotionManager(); }
    @Override
    public FileConfiguration getConfig() { return super.getConfig(); }
    public PlayerConfig getPlayerConfig() { return bootstrap.getPlayerConfig(); }
    public FileConfiguration getCustomConfig() { return bootstrap.getCustomConfig(); }
    public PartyManager getPartyManager() { return bootstrap.getPartyManager(); }
    public me.nakilex.levelplugin.arena.ArenaQueueManager getArenaQueueManager() { return bootstrap.getArenaQueueManager(); }
    public me.nakilex.levelplugin.arena.gui.ArenaQueueGUI getArenaQueueGUI() { return bootstrap.getArenaQueueGUI(); }
    public me.nakilex.levelplugin.arena.instance.ArenaInstanceManager getArenaInstanceManager() { return bootstrap.getArenaInstanceManager(); }
    public me.nakilex.levelplugin.arena.rating.ArenaRatingManager getArenaRatingManager() { return bootstrap.getArenaRatingManager(); }
    public me.nakilex.levelplugin.arena.match.ArenaMatchManager getArenaMatchManager() { return bootstrap.getArenaMatchManager(); }
    public me.nakilex.levelplugin.arena.match.ArenaTeamMatchManager getArenaTeamMatchManager() { return bootstrap.getArenaTeamMatchManager(); }
    public me.nakilex.levelplugin.guild.GuildManager getGuildManager() { return bootstrap.getGuildManager(); }
    public EconomyManager getEconomyManager() { return bootstrap.getEconomyManager(); }
    public ConfigValues getConfigValues() { return bootstrap.getConfigValues(); }
    public me.nakilex.levelplugin.storage.events.StorageEvents getStorageEvents() { return bootstrap.getStorageEvents(); }
    public StorageManager getStorageManager() { return bootstrap.getStorageManager(); }
    public me.nakilex.levelplugin.guild.GuildVaultManager getGuildVaultManager() { return bootstrap.getGuildVaultManager(); }
    public me.nakilex.levelplugin.guild.GuildSettingsGUI getGuildSettingsGUI() { return bootstrap.getGuildSettingsGUI(); }
    public HorseManager getHorseManager() { return bootstrap.getHorseManager(); }
    public DealMaker getDealMaker() { return bootstrap.getDealMaker(); }
    public ItemConfig getItemConfig() { return bootstrap.getItemConfig(); }
    public me.nakilex.levelplugin.lootchests.managers.LootChestManager getLootChestManager() { return bootstrap.getLootChestManager(); }
    public MessageStrings getMessageStrings() { return bootstrap.getMessageStrings(); }
    public void reloadConfigValues() {
        if (bootstrap != null) {
            bootstrap.reloadPluginConfig();
        }
    }
    public LevelManager getLevelManager() { return bootstrap.getLevelManager(); }
    public me.nakilex.levelplugin.player.mining.managers.MiningManager getMiningManager() { return bootstrap.getMiningManager(); }
    public me.nakilex.levelplugin.player.farming.managers.FarmingManager getFarmingManager() { return bootstrap.getFarmingManager(); }
    public me.nakilex.levelplugin.player.fishing.managers.FishingManager getFishingManager() { return bootstrap.getFishingManager(); }
    public me.nakilex.levelplugin.player.attributes.managers.LifeSkillRewardManager getLifeSkillRewardManager() { return bootstrap.getLifeSkillRewardManager(); }
    public me.nakilex.levelplugin.items.tools.ToolManager getToolManager() { return bootstrap.getToolManager(); }
    public me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig getMiningRewardsConfig() { return bootstrap.getMiningRewardsConfig(); }
    public me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig getFishingRewardsConfig() { return bootstrap.getFishingRewardsConfig(); }
    public me.nakilex.levelplugin.economy.managers.GemsManager getGemsManager() { return bootstrap.getGemsManager(); }
    public ItemManager getItemManager() { return bootstrap.getItemManager(); }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseManager getAuctionHouseManager() { return bootstrap.getAuctionHouseManager(); }
    public FileConfiguration getBossConfig() { return bootstrap.getBossConfig(); }
    public MobRewardsConfig getMobRewardsConfig() { return bootstrap.getMobRewardsConfig(); }
    public me.nakilex.levelplugin.utils.registeries.CommandRegistry getCommandRegistry() { return null; }
    public me.nakilex.levelplugin.utils.registeries.ListenerRegistry getListenerRegistry() { return null; }
    public me.nakilex.levelplugin.environment.EnvironmentManager getEnvironmentManager() { return bootstrap.getEnvironmentManager(); }
    public void saveCustomConfig() {
        if (bootstrap != null) {
            bootstrap.saveCustomConfig();
        }
    }
    public me.nakilex.levelplugin.environment.stage.TownStageManager getTownStageManager() { return bootstrap.getTownStageManager(); }
    public me.nakilex.levelplugin.environment.stage.BuildingStageManager getBuildingStageManager() { return bootstrap.getBuildingStageManager(); }
    public me.nakilex.levelplugin.leaderboards.LeaderboardManager getLeaderboardManager() { return bootstrap.getLeaderboardManager(); }
    public me.nakilex.levelplugin.leaderboards.DuelStatsManager getDuelStatsManager() { return bootstrap.getDuelStatsManager(); }
    public PartyGlowManager getPartyGlowManager() { return bootstrap.getPartyGlowManager(); }
    public me.nakilex.levelplugin.friend.FriendManager getFriendManager() { return bootstrap.getFriendManager(); }
    public me.nakilex.levelplugin.friend.FriendGlowManager getFriendGlowManager() { return bootstrap.getFriendGlowManager(); }
    public me.nakilex.levelplugin.friend.PlayerVisibilityManager getPlayerVisibilityManager() { return bootstrap.getVisibilityManager(); }
    public me.nakilex.levelplugin.friend.IgnoreManager getIgnoreManager() { return bootstrap.getIgnoreManager(); }
    public me.nakilex.levelplugin.friend.FriendRequestListener getFriendRequestListener() { return bootstrap.getFriendRequestListener(); }
    public me.nakilex.levelplugin.mob.config.ModelSetManager getModelSetManager() { return bootstrap.getModelSetManager(); }
    public CustomMobManager getCustomMobManager() { return bootstrap.getCustomMobManager(); }
    public me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager getScoreboardManager() { return bootstrap.getScoreboardManager(); }
    public PlayerToggleManager getDmgNumberToggleManager() { return bootstrap.getDmgNumberToggleManager(); }

    public PlayerToggleManager getMobDebugToggleManager() { return bootstrap.getMobDebugToggleManager(); }
    public me.nakilex.levelplugin.debug.DropDebugManager getDropDebugManager() { return bootstrap.getDropDebugManager(); }
    public me.nakilex.levelplugin.debug.BeaconEntityDebugManager getBeaconEntityDebugManager() { return bootstrap.getBeaconEntityDebugManager(); }
    public me.nakilex.levelplugin.fasttravel.FastTravelManager getFastTravelManager() { return bootstrap.getFastTravelManager(); }
    public me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI getFastTravelGUI() { return bootstrap.getFastTravelGUI(); }
    public me.nakilex.levelplugin.music.LocationMusicManager getLocationMusicManager() { return bootstrap.getLocationMusicManager(); }
    public me.nakilex.levelplugin.motd.MotdManager getMotdManager() { return bootstrap.getMotdManager(); }
    public me.nakilex.levelplugin.maintenance.MaintenanceManager getMaintenanceManager() { return bootstrap.getMaintenanceManager(); }
    public me.nakilex.levelplugin.fakeblock.QuestGateManager getQuestGateManager() { return bootstrap.getQuestGateManager(); }
    public me.nakilex.levelplugin.fakeblock.FakeBlockManager getFakeBlockManager() { return bootstrap.getFakeBlockManager(); }
    public me.nakilex.levelplugin.fakeblock.ModelGateManager getModelGateManager() { return bootstrap.getModelGateManager(); }
    public me.nakilex.levelplugin.dungeon.rating.DungeonRatingManager getDungeonRatingManager() { return bootstrap.getDungeonRatingManager(); }
    public me.nakilex.levelplugin.dungeon.DungeonManager getDungeonManager() { return bootstrap.getDungeonManager(); }
    public me.nakilex.levelplugin.world.WorldManager getWorldManager() { return bootstrap.getWorldManager(); }
    public me.nakilex.levelplugin.server.ServerSelectionManager getServerSelectionManager() { return bootstrap.getServerSelectionManager(); }
    public me.nakilex.levelplugin.quests.managers.BeaconManager getBeaconManager() { return bootstrap.getBeaconManager(); }
    public me.nakilex.levelplugin.quests.managers.QuestManager getQuestManager() { return bootstrap.getQuestManager(); }
    public me.nakilex.levelplugin.player.battlepass.BattlePassManager getBattlePassManager() { return bootstrap.getBattlePassManager(); }
    public me.nakilex.levelplugin.player.battlepass.gui.BattlePassGUI getBattlePassGUI() { return bootstrap.getBattlePassGUI(); }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI getAuctionHouseGUI() { return bootstrap.getAuctionHouseGUI(); }
    public me.nakilex.levelplugin.settings.managers.SettingsManager getSettingsManager() { return bootstrap.getSettingsManager(); }
    public SettingsGUI getSettingsGUI() { return bootstrap.getSettingsGUI(); }
    public me.nakilex.levelplugin.environment.UpgradeGUI getUpgradeGUI() { return bootstrap.getUpgradeGUI(); }
    public me.nakilex.levelplugin.environment.BuildingUpgradeGUI getBuildingUpgradeGUI() { return bootstrap.getBuildingUpgradeGUI(); }
    public me.nakilex.levelplugin.npc.dialog.NPCDialogManager getDialogManager() { return bootstrap.getDialogManager(); }
    public me.nakilex.levelplugin.calendar.CalendarManager getCalendarManager() { return bootstrap.getCalendarManager(); }
    public me.nakilex.levelplugin.cutscene.CutsceneManager getCutsceneManager() { return bootstrap.getCutsceneManager(); }
    public CodexManager getCodexManager() { return bootstrap.getCodexManager(); }
    public MercenaryManager getMercenaryManager() { return bootstrap.getMercenaryManager(); }
    public CodexMainGUI getCodexGUI() { return bootstrap.getCodexGUI(); }
    public me.nakilex.levelplugin.dungeon.gui.DungeonListGUI getDungeonListGUI() { return bootstrap.getDungeonListGUI(); }
    public me.nakilex.levelplugin.dungeon.gui.DungeonLeaveGUI getDungeonLeaveGUI() { return bootstrap.getDungeonLeaveGUI(); }
    public me.nakilex.levelplugin.guild.siege.GuildSiegeManager getGuildSiegeManager() { return bootstrap.getGuildSiegeManager(); }
    public PathfindingManager getPathfindingManager() { return bootstrap.getPathfindingManager(); }
    public me.nakilex.levelplugin.transmog.TransmogManager getTransmogManager() { return bootstrap.getTransmogManager(); }
    public ChatGameManager getChatGameManager() { return bootstrap.getChatGameManager(); }
    public me.nakilex.levelplugin.booster.GlobalBoosterManager getBoosterManager() { return bootstrap.getBoosterManager(); }
    public me.nakilex.levelplugin.mercenary.MercenaryAffinityManager getMercenaryAffinityManager() { return bootstrap.getMercenaryAffinityManager(); }
    public me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager getMercenaryExpeditionManager() { return bootstrap.getMercenaryExpeditionManager(); }
    public me.nakilex.levelplugin.mercenary.board.ExpeditionBoardManager getExpeditionBoardManager() { return bootstrap.getExpeditionBoardManager(); }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryGiftBrowserGUI getMercenaryGiftBrowserGUI() { return bootstrap.getMercenaryGiftBrowserGUI(); }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryFriendshipGUI getMercenaryFriendshipGUI() { return bootstrap.getMercenaryFriendshipGUI(); }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionGUI getMercenaryExpeditionGUI() { return bootstrap.getMercenaryExpeditionGUI(); }
    public me.nakilex.levelplugin.mercenary.gui.MercenaryExpeditionRewardsGUI getMercenaryExpeditionRewardsGUI() { return bootstrap.getMercenaryExpeditionRewardsGUI(); }
    public me.nakilex.levelplugin.catacombs.CatacombsManager getCatacombsManager() { return bootstrap.getCatacombsManager(); }
    public me.nakilex.levelplugin.catacombs.CatacombsGUI getCatacombsGUI() { return bootstrap.getCatacombsGUI(); }
}
