package me.nakilex.levelplugin;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import me.nakilex.levelplugin.core.PluginBootstrap;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.config.ItemConfig;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import me.nakilex.levelplugin.party.PartyGlowManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.utils.DealMaker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {
    private static Main instance;
    private static Main plugin;
    private PluginBootstrap bootstrap;
    public static final String PREFIX = "";

    @Override
    public void onEnable() {
        instance = this;
        plugin = this;
        bootstrap = new PluginBootstrap(this);
        bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) bootstrap.disable();
    }

    public static Main getInstance() { return instance; }
    public static Main getPlugin() { return plugin; }

    public BukkitAPIHelper getMythicHelper() { return bootstrap.getMythicHelper(); }
    public PotionManager getPotionManager() { return bootstrap.getPotionManager(); }
    @Override
    public FileConfiguration getConfig() { return super.getConfig(); }
    public PlayerConfig getPlayerConfig() { return bootstrap.getPlayerConfig(); }
    public me.nakilex.levelplugin.spells.managers.SpellManager getSpellManager() { return bootstrap.getSpellmanager(); }
    public FileConfiguration getCustomConfig() { return bootstrap.getCustomConfig(); }
    public PartyManager getPartyManager() { return bootstrap.getPartyManager(); }
    public EconomyManager getEconomyManager() { return bootstrap.getEconomyManager(); }
    public ConfigValues getConfigValues() { return bootstrap.getConfigValues(); }
    public me.nakilex.levelplugin.storage.events.StorageEvents getStorageEvents() { return bootstrap.getStorageEvents(); }
    public DealMaker getDealMaker() { return bootstrap.getDealMaker(); }
    public ItemConfig getItemConfig() { return bootstrap.getItemConfig(); }
    public MessageStrings getMessageStrings() { return bootstrap.getMessageStrings(); }
    public me.nakilex.levelplugin.spells.managers.ManaCostTracker getManaTracker() { return bootstrap.getManaTracker(); }
    public void reloadConfigValues() { }
    public LevelManager getLevelManager() { return bootstrap.getLevelManager(); }
    public me.nakilex.levelplugin.player.mining.managers.MiningManager getMiningManager() { return bootstrap.getMiningManager(); }
    public me.nakilex.levelplugin.items.tools.ToolManager getToolManager() { return bootstrap.getToolManager(); }
    public me.nakilex.levelplugin.player.mining.config.MiningRewardsConfig getMiningRewardsConfig() { return bootstrap.getMiningRewardsConfig(); }
    public me.nakilex.levelplugin.economy.managers.GemsManager getGemsManager() { return bootstrap.getGemsManager(); }
    public ItemManager getItemManager() { return bootstrap.getItemManager(); }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseManager getAuctionHouseManager() { return bootstrap.getAuctionHouseManager(); }
    public FileConfiguration getBossConfig() { return bootstrap.getBossConfig(); }
    public me.nakilex.levelplugin.utils.registeries.CommandRegistry getCommandRegistry() { return null; }
    public me.nakilex.levelplugin.utils.registeries.ListenerRegistry getListenerRegistry() { return null; }
    public me.nakilex.levelplugin.spells.registry.EffectRegistry getEffectRegistry() { return null; }
    public me.nakilex.levelplugin.environment.EnvironmentManager getEnvironmentManager() { return bootstrap.getEnvironmentManager(); }
    public me.nakilex.levelplugin.environment.stage.TownStageManager getTownStageManager() { return bootstrap.getTownStageManager(); }
    public me.nakilex.levelplugin.environment.stage.BuildingStageManager getBuildingStageManager() { return bootstrap.getBuildingStageManager(); }
    public me.nakilex.levelplugin.leaderboards.LeaderboardManager getLeaderboardManager() { return bootstrap.getLeaderboardManager(); }
    public me.nakilex.levelplugin.leaderboards.DuelStatsManager getDuelStatsManager() { return bootstrap.getDuelStatsManager(); }
    public Map<UUID, List<net.citizensnpcs.api.npc.NPC>> getActiveBowDrones() { return bootstrap.getActiveBowDrones(); }
    public PartyGlowManager getPartyGlowManager() { return bootstrap.getPartyGlowManager(); }
    public me.nakilex.levelplugin.friend.FriendManager getFriendManager() { return bootstrap.getFriendManager(); }
    public me.nakilex.levelplugin.friend.FriendGlowManager getFriendGlowManager() { return bootstrap.getFriendGlowManager(); }
    public me.nakilex.levelplugin.friend.PlayerVisibilityManager getPlayerVisibilityManager() { return bootstrap.getVisibilityManager(); }
    public me.nakilex.levelplugin.friend.IgnoreManager getIgnoreManager() { return bootstrap.getIgnoreManager(); }
    public me.nakilex.levelplugin.friend.FriendRequestListener getFriendRequestListener() { return bootstrap.getFriendRequestListener(); }
    public me.nakilex.levelplugin.mob.config.ModelSetManager getModelSetManager() { return bootstrap.getModelSetManager(); }
    public me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager getScoreboardManager() { return bootstrap.getScoreboardManager(); }
    public DmgNumberToggleManager getDmgNumberToggleManager() { return bootstrap.getDmgNumberToggleManager(); }
    public me.nakilex.levelplugin.fasttravel.FastTravelManager getFastTravelManager() { return bootstrap.getFastTravelManager(); }
    public me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI getFastTravelGUI() { return bootstrap.getFastTravelGUI(); }
    public me.nakilex.levelplugin.motd.MotdManager getMotdManager() { return bootstrap.getMotdManager(); }
    public me.nakilex.levelplugin.fakeblock.QuestGateManager getQuestGateManager() { return bootstrap.getQuestGateManager(); }
    public me.nakilex.levelplugin.fakeblock.FakeBlockManager getFakeBlockManager() { return bootstrap.getFakeBlockManager(); }
    public me.nakilex.levelplugin.fakeblock.ModelGateManager getModelGateManager() { return bootstrap.getModelGateManager(); }
    public me.nakilex.levelplugin.dungeon.DungeonManager getDungeonManager() { return bootstrap.getDungeonManager(); }
    public me.nakilex.levelplugin.quests.managers.BeaconManager getBeaconManager() { return bootstrap.getBeaconManager(); }
    public me.nakilex.levelplugin.quests.managers.QuestManager getQuestManager() { return bootstrap.getQuestManager(); }
    public me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI getAuctionHouseGUI() { return bootstrap.getAuctionHouseGUI(); }
    public me.nakilex.levelplugin.settings.managers.SettingsManager getSettingsManager() { return bootstrap.getSettingsManager(); }
    public SettingsGUI getSettingsGUI() { return bootstrap.getSettingsGUI(); }
    public me.nakilex.levelplugin.environment.UpgradeGUI getUpgradeGUI() { return bootstrap.getUpgradeGUI(); }
    public me.nakilex.levelplugin.environment.BuildingUpgradeGUI getBuildingUpgradeGUI() { return bootstrap.getBuildingUpgradeGUI(); }
    public me.nakilex.levelplugin.npc.dialog.NPCDialogManager getDialogManager() { return bootstrap.getDialogManager(); }
    public me.nakilex.levelplugin.calendar.CalendarManager getCalendarManager() { return bootstrap.getCalendarManager(); }
    public me.nakilex.levelplugin.cutscene.CutsceneManager getCutsceneManager() { return bootstrap.getCutsceneManager(); }
}
