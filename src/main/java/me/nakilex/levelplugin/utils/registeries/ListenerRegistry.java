package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.boss.FieldBossListener;
import me.nakilex.levelplugin.doublejump.listeners.DoubleJumpListener;
import me.nakilex.levelplugin.duels.listeners.DuelListener;
import me.nakilex.levelplugin.economy.managers.CoinDropManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.arena.gui.ArenaQueueGUI;
import me.nakilex.levelplugin.arena.match.ArenaMatchManager;
import me.nakilex.levelplugin.arena.match.ArenaTeamMatchManager;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.items.listeners.*;
import me.nakilex.levelplugin.lootchests.listeners.LootChestCloseListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestChunkListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestWandListener;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.dps.DpsDummyManager;
import me.nakilex.levelplugin.mob.listeners.*;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.mob.custom.CustomMobAnimationListener;
import me.nakilex.levelplugin.mob.custom.CustomMobRewardListener;
import me.nakilex.levelplugin.mob.utils.MobRewardService;
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
import me.nakilex.levelplugin.spells.gui.SpellKeybindGUI;
import me.nakilex.levelplugin.spells.gui.SpellUpgradeGUI;
import me.nakilex.levelplugin.spells.gui.SpellSummonGUI;
import me.nakilex.levelplugin.spells.listeners.SpellCastListener;
import me.nakilex.levelplugin.spells.listeners.SpellInputListener;
import me.nakilex.levelplugin.player.classes.gui.SubclassGUI;
import me.nakilex.levelplugin.player.classes.gui.ClassSelectionGUI;
import me.nakilex.levelplugin.player.classes.essence.listener.ClassEssenceMenuListener;
import me.nakilex.levelplugin.player.classes.essence.listener.ClassEssenceBoundListener;
import me.nakilex.levelplugin.player.classes.essence.listener.ClassEssenceSwapListener;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceUpgradeGUI;
import me.nakilex.levelplugin.trade.listeners.PlayerRightClicksPlayerListener;
import me.nakilex.levelplugin.guild.GuildGUI;
import me.nakilex.levelplugin.guild.GuildGUIListener;
import me.nakilex.levelplugin.guild.quests.GuildQuestGUIListener;
import me.nakilex.levelplugin.utils.*;
import me.nakilex.levelplugin.utils.FeatureFlagUtil;
import me.nakilex.levelplugin.quests.listeners.QuestKillListener;
import me.nakilex.levelplugin.quests.listeners.QuestCraftListener;
import me.nakilex.levelplugin.quests.gui.QuestGUIListener;
import me.nakilex.levelplugin.npc.listeners.NPCDialogMoveListener;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.debug.BeaconEntityDebugManager;
import me.nakilex.levelplugin.debug.listeners.MobStatusDebugListener;
import me.nakilex.levelplugin.debug.listeners.SpellInputDebugListener;
import me.nakilex.levelplugin.fasttravel.listeners.WaystoneListener;
import me.nakilex.levelplugin.fasttravel.listeners.ExplorationListener;
import me.nakilex.levelplugin.fasttravel.listeners.FastTravelRespawnListener;
import me.nakilex.levelplugin.fasttravel.gui.FastTravelGUI;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.motd.MotdManager;
import me.nakilex.levelplugin.environment.UpgradeGUI;
import me.nakilex.levelplugin.environment.BuildingUpgradeGUI;
import me.nakilex.levelplugin.environment.listeners.BuildingHologramListener;
import me.nakilex.levelplugin.environment.listeners.StageBlockInteractListener;
import me.nakilex.levelplugin.environment.listeners.LeafDecayBlocker;
import me.nakilex.levelplugin.codex.*;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantListener;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.gui.PetGUI;
import me.nakilex.levelplugin.pet.gui.PetSummonGUI;
import me.nakilex.levelplugin.pet.summon.PetSummonManager;
import me.nakilex.levelplugin.spells.summon.SpellSummonManager;
import me.nakilex.levelplugin.pet.listeners.PetPlayerListener;
import me.nakilex.levelplugin.pet.listeners.PetCombatEffectListener;
import me.nakilex.levelplugin.pet.listeners.PetMovementListener;
import me.nakilex.levelplugin.pet.listeners.PetProtectionListener;
import me.nakilex.levelplugin.pet.listeners.PetUtilityEffectListener;
import me.nakilex.levelplugin.server.LevelPluginCommandGuard;
import me.nakilex.levelplugin.server.ServerSelectionManager;
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
                                         SpellKeybindGUI spellKeybindGUI,
                                         SpellUpgradeGUI spellUpgradeGUI,
                                         me.nakilex.levelplugin.debug.gui.DebugGUI debugGUI,
                                         GuildGUI guildGUI,
                                         FileConfiguration bossConfig,
                                        GemsManager gemsManager,
                                       me.nakilex.levelplugin.enchanting.gui.EnchantGUI enchantGUI,
                                       AuctionHouseGUI auctionGUI,
                                        QuestManager questManager,
                                        NPCDialogManager dialogManager,
                                         PlayerScoreboardManager scoreboardManager,
                                        FastTravelManager fastTravelManager,
                                        FastTravelGUI fastTravelGUI,
                                        me.nakilex.levelplugin.dungeon.gui.DungeonListGUI dungeonListGUI,
                                        me.nakilex.levelplugin.dungeon.gui.DungeonLeaveGUI dungeonLeaveGUI,
                                        MotdManager motdManager,
                                        UpgradeGUI upgradeGUI,
                                        BuildingUpgradeGUI buildingUpgradeGUI,
                                        BuildingHologramListener hologramListener,
                                        StageBlockInteractListener stageBlockInteractListener,
                                        CodexMainGUI codexGUI,
                                        MobCodexGUI mobCodexGUI,
                                        NpcCodexGUI npcCodexGUI,
                                        LocationCodexGUI locationCodexGUI,
                                        FoodCodexGUI foodCodexGUI,
                                        me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager wmManager,
                                        ArenaQueueGUI arenaQueueGUI,
                                         ArenaMatchManager arenaMatchManager,
                                         ArenaTeamMatchManager arenaTeamMatchManager,
                                        ChatGameManager chatGameManager,
                                        DpsDummyManager dpsDummyManager,
                                        BeaconEntityDebugManager beaconEntityDebugManager,
                                        ServerSelectionManager serverSelectionManager,
                                        PetManager petManager,
                                        PetGUI petGUI,
                                        me.nakilex.levelplugin.pet.gui.PetSettingsGUI petSettingsGUI,
                                        me.nakilex.levelplugin.pet.gui.PetMergeGUI petMergeGUI,
                                        PetSummonGUI petSummonGUI,
                                        PetSummonManager petSummonManager,
                                        SpellSummonGUI spellSummonGUI,
                                        SpellSummonManager spellSummonManager,
                                        CustomMobManager customMobManager,
                                        me.nakilex.levelplugin.debug.ArcSlashDebugManager arcSlashDebugManager,
                                        me.nakilex.levelplugin.debug.WieldStyleDebugManager wieldStyleDebugManager,
                                        me.nakilex.levelplugin.debug.gui.WieldStyleDebugGUI wieldStyleDebugGUI,
                                        me.nakilex.levelplugin.debug.gui.ArcSlashDebugGUI arcSlashDebugGUI) {


        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new MobDamageListener(), plugin);
        pm.registerEvents(new SlimeSplitListener(), plugin);
        BattlePassManager battlePassManager = plugin.getBattlePassManager();

        MobRewardService rewardService = new MobRewardService(
                plugin,
                mobRewardsConfig,
                plugin.getLevelManager(),
                economyManager,
                lootChestManager,
                plugin.getModelSetManager(),
                mobDebugToggleManager,
                battlePassManager,
                plugin.getDropDebugManager()
        );
        if (customMobManager != null) {
            pm.registerEvents(customMobManager.getNameManager(), plugin);
            pm.registerEvents(new CustomMobRewardListener(customMobManager, rewardService), plugin);
            pm.registerEvents(new CustomMobAnimationListener(plugin, customMobManager), plugin);
            pm.registerEvents(customMobManager.getSpawnerManager(), plugin);
            pm.registerEvents(customMobManager.getAdminGui(), plugin);
            pm.registerEvents(new MobStatusDebugListener(customMobManager), plugin);
        }
        pm.registerEvents(new me.nakilex.levelplugin.player.mining.listeners.KingdomMineRegenListener(
                plugin,
                me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager.getInstance(plugin),
                plugin.getMiningRewardsConfig()), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.player.farming.listeners.WheatHarvestListener(plugin.getFarmingManager(), plugin.getFarmingRewardsConfig()), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.player.fishing.listeners.FishingListener(
                plugin,
                plugin.getFishingRewardsConfig(),
                plugin.getFishingManager()), plugin);
        pm.registerEvents(new LevelPluginCommandGuard(plugin, serverSelectionManager), plugin);
        pm.registerEvents(new PlayerJoinListener(
                plugin.getLevelManager(),
                plugin.getMiningManager(),
                plugin.getFarmingManager(),
                plugin.getFishingManager(),
                plugin.getWoodcuttingManager(),
                plugin.getEnvironmentManager(),
                serverSelectionManager), plugin);
        pm.registerEvents(new PlayerQuitListener(plugin.getPlayerConfig(), plugin.getEnvironmentManager()), plugin);
        pm.registerEvents(me.nakilex.levelplugin.mail.MailCommand.getInstance(), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.mail.MailJoinNotifier(), plugin);
        pm.registerEvents(me.nakilex.levelplugin.mail.MailAdminCommand.getInstance(), plugin);
        if (petManager != null) {
            pm.registerEvents(new PetPlayerListener(petManager), plugin);
            pm.registerEvents(new PetCombatEffectListener(petManager), plugin);
            pm.registerEvents(new PetMovementListener(petManager), plugin);
            pm.registerEvents(new PetProtectionListener(petManager), plugin);
        }
        if (petGUI != null) {
            pm.registerEvents(petGUI, plugin);
        }
        if (petSettingsGUI != null) {
            pm.registerEvents(petSettingsGUI, plugin);
        }
        if (petMergeGUI != null) {
            pm.registerEvents(petMergeGUI, plugin);
        }
        if (petSummonGUI != null) {
            pm.registerEvents(petSummonGUI, plugin);
        }
        if (petSummonManager != null) {
            pm.registerEvents(petSummonManager, plugin);
        }
        if (spellSummonGUI != null) {
            pm.registerEvents(spellSummonGUI, plugin);
        }
        if (spellSummonManager != null) {
            pm.registerEvents(spellSummonManager, plugin);
        }
        pm.registerEvents(me.nakilex.levelplugin.spells.input.SpellInputHudManager.getInstance(), plugin);
        pm.registerEvents(new StatsMenuListener(codexGUI), plugin);
        pm.registerEvents(new StatsEffectListener(), plugin);
        boolean boosterSystemEnabled = FeatureFlagUtil.isEnabled("features.booster-system", false);
        if (boosterSystemEnabled && boosterManager != null) {
            pm.registerEvents(new BoosterItemListener(boosterManager), plugin);
        }
        pm.registerEvents(new ArmorListener(), plugin);
        pm.registerEvents(new ArmorStatsListener(), plugin);
        pm.registerEvents(new WeaponListener(), plugin);
        pm.registerEvents(new WeaponStatsListener(), plugin);
        pm.registerEvents(new ItemNameDisplayListener(), plugin);
        pm.registerEvents(new StaticItemListener(), plugin);
        pm.registerEvents(new ItemDropProtectionListener(plugin), plugin);
        pm.registerEvents(new FireworkDamageListener(), plugin);
        pm.registerEvents(blacksmithGUI, plugin);
        pm.registerEvents(horseGUI, plugin);
        pm.registerEvents(new NPCClickListener(
                economyManager,
                questManager,
                dialogManager,
                horseGUI,
                enchantGUI,
                auctionGUI,
                plugin.getStorageManager()), plugin);
        pm.registerEvents(new NPCCommandListener(), plugin);
        pm.registerEvents(new PlayerRightClicksPlayerListener(), plugin);
        pm.registerEvents(new TradingWindow(), plugin);
        boolean arenaSystemEnabled = FeatureFlagUtil.isEnabled("features.arena-system", false);
        if (arenaSystemEnabled && arenaQueueGUI != null && arenaMatchManager != null && arenaTeamMatchManager != null) {
            pm.registerEvents(arenaQueueGUI, plugin);
            pm.registerEvents(arenaMatchManager, plugin);
            pm.registerEvents(arenaTeamMatchManager, plugin);
        }
        pm.registerEvents(new ChatChannelListener(), plugin);
        pm.registerEvents(new ChatGameListener(chatGameManager), plugin);
        pm.registerEvents(new PartyInviteListener(partyManager), plugin);
        LootChestListener lootChestListener = new LootChestListener(lootChestManager, battlePassManager);
        pm.registerEvents(lootChestListener, plugin);
        if (petManager != null) {
            pm.registerEvents(new PetUtilityEffectListener(petManager, lootChestListener, lootChestManager), plugin);
        }
        pm.registerEvents(new LootChestCloseListener(lootChestManager, economyManager,
                plugin.getDungeonManager()), plugin);
        pm.registerEvents(new CoinDropManager(economyManager, plugin.getDropDebugManager()), plugin);
        pm.registerEvents(new LootChestChunkListener(lootChestManager), plugin);
        pm.registerEvents(new LootChestWandListener(lootChestManager), plugin);
        pm.registerEvents(new PotionUseListener(potionManager, plugin), plugin);
        if (dpsDummyManager != null) {
            pm.registerEvents(dpsDummyManager, plugin);
        }
        pm.registerEvents(new FallDamageDisabler(), plugin);
        pm.registerEvents(new HungerDisabler(), plugin);
        pm.registerEvents(new CropTrampleListener(), plugin);
        boolean duelSystemEnabled = FeatureFlagUtil.isEnabled("features.duel-system", false);
        if (duelSystemEnabled) {
            pm.registerEvents(new DuelListener(), plugin);
        }
        pm.registerEvents(new PickupCustomItemListener(plugin), plugin);
        pm.registerEvents(new CustomItemUpdateListener(), plugin);
        pm.registerEvents(new SalvageListener(economyManager, gemsManager), plugin);
        pm.registerEvents(new DoubleJumpListener(), plugin);
        pm.registerEvents(new DamageIndicatorListener(dmgToggleManager), plugin);
        new DamageIndicatorPacketBlocker(plugin);
        pm.registerEvents(new SpellInputListener(plugin.getSettingsManager()), plugin);
        pm.registerEvents(new SpellCastListener(plugin), plugin);
        pm.registerEvents(new SpellInputDebugListener(), plugin);
        pm.registerEvents(arcSlashDebugManager, plugin);
        pm.registerEvents(wieldStyleDebugManager, plugin);
        pm.registerEvents(wieldStyleDebugGUI, plugin);
        pm.registerEvents(arcSlashDebugGUI, plugin);
        pm.registerEvents(settingsGUI, plugin);
        pm.registerEvents(spellKeybindGUI, plugin);
        pm.registerEvents(spellUpgradeGUI, plugin);
        pm.registerEvents(debugGUI, plugin);
        pm.registerEvents(new GuildGUIListener(guildGUI), plugin);
        pm.registerEvents(new GuildQuestGUIListener(), plugin);
        boolean classSystemEnabled = FeatureFlagUtil.isEnabled("features.class-system", false);
        if (classSystemEnabled) {
            pm.registerEvents(new SubclassGUI(), plugin);
            pm.registerEvents(ClassSelectionGUI.getInstance(), plugin);
        }
        boolean essenceSystemEnabled = classSystemEnabled
                && FeatureFlagUtil.isEnabled("features.essence-system", false);
        if (essenceSystemEnabled) {
            pm.registerEvents(new ClassEssenceMenuListener(), plugin);
            pm.registerEvents(new ClassEssenceBoundListener(), plugin);
            pm.registerEvents(new ClassEssenceSwapListener(), plugin);
            pm.registerEvents(new ClassEssenceUpgradeGUI(), plugin);
        }
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
        if (dungeonListGUI != null) {
            pm.registerEvents(dungeonListGUI, plugin);
        }
        if (dungeonLeaveGUI != null) {
            pm.registerEvents(dungeonLeaveGUI, plugin);
        }
        pm.registerEvents(motdManager, plugin);
        pm.registerEvents(upgradeGUI, plugin);
        pm.registerEvents(buildingUpgradeGUI, plugin);
        pm.registerEvents(new CodexListener(mobRewardsConfig, bossConfig, plugin.getCodexManager(), customMobManager), plugin);
        pm.registerEvents(codexGUI, plugin);
        pm.registerEvents(mobCodexGUI, plugin);
        pm.registerEvents(npcCodexGUI, plugin);
        pm.registerEvents(locationCodexGUI, plugin);
        pm.registerEvents(foodCodexGUI, plugin);
        pm.registerEvents(hologramListener, plugin);
        pm.registerEvents(stageBlockInteractListener, plugin);
        pm.registerEvents(new me.nakilex.levelplugin.environment.listeners.EnvironmentInventoryListener(plugin.getEnvironmentManager()), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.environment.listeners.EnvironmentDistanceListener(plugin.getEnvironmentManager()), plugin);
        pm.registerEvents(new LeafDecayBlocker(), plugin);
        pm.registerEvents(new me.nakilex.levelplugin.calendar.WeatherBlockListener(), plugin);
        pm.registerEvents(new WanderingMerchantListener(wmManager), plugin);
        pm.registerEvents(beaconEntityDebugManager, plugin);
        if (serverSelectionManager != null) {
            pm.registerEvents(serverSelectionManager.getSelectorGUI(), plugin);
        }
        if (plugin.getCustomConfig().getBoolean("features.profiles", true)) {
            pm.registerEvents(new me.nakilex.levelplugin.player.profile.ProfileSelectionGUI(), plugin);
        }

        pm.registerEvents(new me.nakilex.levelplugin.cutscene.editor.EditorListener(plugin.getCutsceneManager()), plugin);

        ArrowUtils arrowUtils = new ArrowUtils(plugin);
        pm.registerEvents(arrowUtils, plugin);
        arrowUtils.startArrowCleanupTask();
    }
}
