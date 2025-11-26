package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.boss.FieldBossListener;
import me.nakilex.levelplugin.doublejump.listeners.DoubleJumpListener;
import me.nakilex.levelplugin.duels.listeners.DuelListener;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.arena.gui.ArenaQueueGUI;
import me.nakilex.levelplugin.arena.match.ArenaMatchManager;
import me.nakilex.levelplugin.arena.match.ArenaTeamMatchManager;
import me.nakilex.levelplugin.lootchests.listeners.ChestHologramListener;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.items.listeners.*;
import me.nakilex.levelplugin.lootchests.listeners.LootChestCloseListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestShutdownListener;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.dps.DpsDummyManager;
import me.nakilex.levelplugin.mob.listeners.*;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.mob.managers.MythicMobNameManager;
import me.nakilex.levelplugin.npc.listeners.NPCClickListener;
import me.nakilex.levelplugin.npc.listeners.NPCCommandListener;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.chat.ChatChannelListener;
import me.nakilex.levelplugin.chat.games.ChatGameListener;
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.booster.GlobalBoosterManager;
import me.nakilex.levelplugin.booster.BoosterItemListener;
import me.nakilex.levelplugin.party.PartyInviteListener;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.attributes.listeners.StatsMenuListener;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.listener.*;
import me.nakilex.levelplugin.player.utils.ArrowUtils;
import me.nakilex.levelplugin.potions.listeners.PotionUseListener;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.salvage.listeners.SalvageListener;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.spells.listener.ClassSpellListener;
import me.nakilex.levelplugin.spells.gui.SpellGUIListener;
import me.nakilex.levelplugin.spells.listener.*;
import me.nakilex.levelplugin.player.classes.gui.SubclassGUI;
import me.nakilex.levelplugin.player.classes.essence.listener.ClassEssenceMenuListener;
import me.nakilex.levelplugin.player.classes.essence.listener.ClassEssenceBoundListener;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceUpgradeGUI;
import me.nakilex.levelplugin.trade.listeners.PlayerRightClicksPlayerListener;
import me.nakilex.levelplugin.guild.GuildGUIListener;
import me.nakilex.levelplugin.guild.quests.GuildQuestGUIListener;
import me.nakilex.levelplugin.utils.*;
import me.nakilex.levelplugin.quests.listeners.QuestKillListener;
import me.nakilex.levelplugin.quests.listeners.QuestCraftListener;
import me.nakilex.levelplugin.quests.gui.QuestGUIListener;
import me.nakilex.levelplugin.npc.listeners.NPCDialogMoveListener;
import me.nakilex.levelplugin.dungeon.DungeonMobSpawnListener;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.fasttravel.listeners.WaystoneListener;
import me.nakilex.levelplugin.fasttravel.listeners.ExplorationListener;
import me.nakilex.levelplugin.fasttravel.listeners.FastTravelRespawnListener;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fakeblock.ModelGateManager;
import me.nakilex.levelplugin.motd.MotdManager;
import me.nakilex.levelplugin.environment.UpgradeGUI;
import me.nakilex.levelplugin.environment.BuildingUpgradeGUI;
import me.nakilex.levelplugin.environment.listeners.BuildingHologramListener;
import me.nakilex.levelplugin.environment.listeners.StageBlockInteractListener;
import me.nakilex.levelplugin.codex.*;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantListener;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;

public class ListenerRegistry {

    public static void registerListeners(Main plugin,
                                         BlacksmithGUI blacksmithGUI,
                                         HorseGUI horseGUI,
                                         LootChestManager lootChestManager,
                                         PotionManager potionManager,
                                         PartyManager partyManager,
                                         GlobalBoosterManager boosterManager,
                                         EconomyManager economyManager,
                                         MobRewardsConfig mobRewardsConfig,
                                         PlayerToggleManager dmgToggleManager,
                                         PlayerToggleManager mobDebugToggleManager,
                                         PickupCustomItemListener pickupCustomItemListener,
                                         SettingsGUI settingsGUI,
                                         me.nakilex.levelplugin.debug.gui.DebugGUI debugGUI,
                                         FileConfiguration bossConfig,
                                         MeteorListener meteorListener,
                                         GemsManager gemsManager,
                                        me.nakilex.levelplugin.enchanting.gui.EnchantGUI enchantGUI,
                                        AuctionHouseGUI auctionGUI,
                                        ChestHologramListener chestHologramListener,
                                        QuestManager questManager,
                                        NPCDialogManager dialogManager,
                                         PlayerScoreboardManager scoreboardManager,
                                        FastTravelManager fastTravelManager,
                                        FastTravelGUI fastTravelGUI,
                                        me.nakilex.levelplugin.dungeon.gui.DungeonListGUI dungeonListGUI,
                                        MotdManager motdManager,
                                        UpgradeGUI upgradeGUI,
                                        BuildingUpgradeGUI buildingUpgradeGUI,
                                        BuildingHologramListener hologramListener,
                                        StageBlockInteractListener stageBlockInteractListener,
                                        CodexMainGUI codexGUI,
                                        MobCodexGUI mobCodexGUI,
                                        NpcCodexGUI npcCodexGUI,
                                        LocationCodexGUI locationCodexGUI,
                                        me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager wmManager,
                                        ArenaQueueGUI arenaQueueGUI,
                                        ArenaMatchManager arenaMatchManager,
                                        ArenaTeamMatchManager arenaTeamMatchManager,
                                        ChatGameManager chatGameManager,
                                        DpsDummyManager dpsDummyManager) {


        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new MobDamageListener(), plugin);
        MythicMobDamageTracker dmgTracker = new MythicMobDamageTracker();
        pm.registerEvents(dmgTracker, plugin);
        BattlePassManager battlePassManager = plugin.getBattlePassManager();

        pm.registerEvents(new MythicMobRewardListener(
                dmgTracker,
                mobRewardsConfig,
                plugin.getLevelManager(),
                economyManager,
                lootChestManager,
                plugin.getModelSetManager(),
                mobDebugToggleManager,
                battlePassManager
        ), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.player.mining.listeners.OreMiningListener(plugin, plugin.getMiningRewardsConfig(), plugin.getMiningManager()), plugin);
        pm.registerEvents(new PlayerJoinListener(plugin.getLevelManager(), plugin.getMiningManager(), plugin.getPlayerConfig(), plugin.getEnvironmentManager()), plugin);
        pm.registerEvents(new PlayerQuitListener(plugin.getPlayerConfig(), plugin.getEnvironmentManager()), plugin);
        pm.registerEvents(new StatsMenuListener(codexGUI), plugin);
        pm.registerEvents(new StatsEffectListener(), plugin);
        pm.registerEvents(new BoosterItemListener(boosterManager), plugin);
        pm.registerEvents(new ArmorListener(), plugin);
        pm.registerEvents(new ArmorStatsListener(), plugin);
        pm.registerEvents(new WeaponListener(), plugin);
        pm.registerEvents(new WeaponStatsListener(), plugin);
        pm.registerEvents(new ItemNameDisplayListener(), plugin);
        pm.registerEvents(new StaticItemListener(), plugin);
        pm.registerEvents(blacksmithGUI, plugin);
        pm.registerEvents(horseGUI, plugin);
        pm.registerEvents(new NPCClickListener(economyManager, questManager, dialogManager, horseGUI, enchantGUI, auctionGUI), plugin);
        pm.registerEvents(new NPCCommandListener(), plugin);
        pm.registerEvents(new PlayerRightClicksPlayerListener(), plugin);
        pm.registerEvents(new TradingWindow(), plugin);
        pm.registerEvents(arenaQueueGUI, plugin);
        pm.registerEvents(arenaMatchManager, plugin);
        pm.registerEvents(arenaTeamMatchManager, plugin);
        pm.registerEvents(plugin.getTowerManager(), plugin);
        pm.registerEvents(plugin.getTowerGUI(), plugin);
        pm.registerEvents(new ChatChannelListener(), plugin);
        pm.registerEvents(new ChatGameListener(chatGameManager), plugin);
        pm.registerEvents(new PartyInviteListener(partyManager), plugin);
        pm.registerEvents(new LootChestListener(lootChestManager, battlePassManager), plugin);
        pm.registerEvents(new LootChestCloseListener(lootChestManager, economyManager,
                plugin.getDungeonManager()), plugin);
        pm.registerEvents(new PotionUseListener(potionManager, plugin), plugin);
        pm.registerEvents(new MythicMobNameManager(plugin), plugin);
        pm.registerEvents(new MythicMobDamageListener(), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.mob.listeners.MythicSkillDamageScaler(), plugin);
        pm.registerEvents(dpsDummyManager, plugin);
        pm.registerEvents(new FallDamageDisabler(), plugin);
        pm.registerEvents(new HungerDisabler(), plugin);
        pm.registerEvents(new CropTrampleListener(), plugin);
        pm.registerEvents(new DuelListener(), plugin);
        pm.registerEvents(new PickupCustomItemListener(plugin), plugin);
        pm.registerEvents(new CustomItemUpdateListener(), plugin);
        pm.registerEvents(new SalvageListener(economyManager, gemsManager), plugin);
        pm.registerEvents(new SpellGUIListener(), plugin);
        pm.registerEvents(new DoubleJumpListener(), plugin);
        pm.registerEvents(new DamageIndicatorListener(dmgToggleManager), plugin);
        pm.registerEvents(new DamageChatListener(), plugin);
        pm.registerEvents(settingsGUI, plugin);
        pm.registerEvents(debugGUI, plugin);
        pm.registerEvents(new GuildGUIListener(), plugin);
        pm.registerEvents(new GuildQuestGUIListener(), plugin);
        pm.registerEvents(new MeteorListener(), plugin);
        pm.registerEvents(new ShockwaveListener(), plugin);
        pm.registerEvents(new ClassSpellListener(), plugin);
        pm.registerEvents(new SubclassGUI(), plugin);
        pm.registerEvents(new ClassEssenceMenuListener(), plugin);
        pm.registerEvents(new ClassEssenceBoundListener(), plugin);
        pm.registerEvents(new ClassEssenceUpgradeGUI(), plugin);
        pm.registerEvents(new ChestHologramListener(lootChestManager), plugin);
        pm.registerEvents(new LootChestShutdownListener(plugin, lootChestManager), plugin);

        pm.registerEvents(new FieldBossListener(plugin, plugin.getBossConfig(), plugin.getItemManager(), plugin.getGemsManager()), plugin);
        pm.registerEvents(new EquipOnJoinListener(), plugin);
        pm.registerEvents(new PlayerDeathListener(plugin), plugin);
        pm.registerEvents(new FullInventoryListener(plugin.getSettingsManager()), plugin);
        pm.registerEvents(enchantGUI, plugin);
        pm.registerEvents(new QuestKillListener(questManager), plugin);
        pm.registerEvents(new QuestCraftListener(questManager), plugin);
        pm.registerEvents(new QuestGUIListener(questManager), plugin);
        pm.registerEvents(new NPCDialogMoveListener(dialogManager), plugin);
        pm.registerEvents(plugin.getScoreboardManager(), plugin);
        pm.registerEvents(plugin.getPartyGlowManager(), plugin);
        pm.registerEvents(plugin.getFriendGlowManager(), plugin);
        pm.registerEvents(plugin.getIgnoreManager(), plugin);
        pm.registerEvents(plugin.getFriendRequestListener(), plugin);
        pm.registerEvents(plugin.getPlayerVisibilityManager(), plugin);
        pm.registerEvents(new WaystoneListener(fastTravelGUI, fastTravelManager, plugin.getModelGateManager()), plugin);
        pm.registerEvents(new ExplorationListener(fastTravelManager, plugin.getLocationMusicManager()), plugin);
        pm.registerEvents(new FastTravelRespawnListener(fastTravelManager), plugin);
        pm.registerEvents(dungeonListGUI, plugin);
        pm.registerEvents(motdManager, plugin);
        pm.registerEvents(upgradeGUI, plugin);
        pm.registerEvents(buildingUpgradeGUI, plugin);
        pm.registerEvents(codexGUI, plugin);
        pm.registerEvents(mobCodexGUI, plugin);
        pm.registerEvents(npcCodexGUI, plugin);
        pm.registerEvents(locationCodexGUI, plugin);
        pm.registerEvents(new me.nakilex.levelplugin.codex.CodexListener(
                plugin.getMobRewardsConfig(),
                plugin.getBossConfig(),
                plugin.getCodexManager()), plugin);
        pm.registerEvents(new DungeonMobSpawnListener(plugin.getDungeonManager(), plugin), plugin);
        pm.registerEvents(hologramListener, plugin);
        pm.registerEvents(stageBlockInteractListener, plugin);
        pm.registerEvents(new me.nakilex.levelplugin.environment.listeners.EnvironmentInventoryListener(plugin.getEnvironmentManager()), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.environment.listeners.EnvironmentDistanceListener(plugin.getEnvironmentManager()), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.calendar.WeatherBlockListener(), plugin);
        pm.registerEvents(new WanderingMerchantListener(wmManager), plugin);
        if (plugin.getCustomConfig().getBoolean("features.profiles", true)) {
            pm.registerEvents(new me.nakilex.levelplugin.player.profile.ProfileSelectionGUI(), plugin);
        }

        pm.registerEvents(new me.nakilex.levelplugin.cutscene.editor.EditorListener(plugin.getCutsceneManager()), plugin);

        ArrowUtils arrowUtils = new ArrowUtils(plugin);
        pm.registerEvents(arrowUtils, plugin);
        arrowUtils.startArrowCleanupTask();
    }
}
