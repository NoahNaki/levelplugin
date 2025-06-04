package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.boss.FieldBossListener;
import me.nakilex.levelplugin.doublejump.listeners.DoubleJumpListener;
import me.nakilex.levelplugin.duels.listeners.DuelListener;
import me.nakilex.levelplugin.duels.listeners.ProjectileFriendlyFireListener;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.lootchests.listeners.ChestHologramListener;
import me.nakilex.levelplugin.player.attributes.listeners.StatsEffectListener;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.items.listeners.*;
import me.nakilex.levelplugin.lootchests.listeners.LootChestCloseListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestListener;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.listeners.*;
import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import me.nakilex.levelplugin.mob.managers.MythicMobNameManager;
import me.nakilex.levelplugin.npc.listeners.NPCClickListener;
import me.nakilex.levelplugin.npc.listeners.NPCCommandListener;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.party.PartyChatListener;
import me.nakilex.levelplugin.party.PartyInviteListener;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.attributes.listeners.StatsMenuListener;
import me.nakilex.levelplugin.player.classes.listeners.ClassMenuListener;
import me.nakilex.levelplugin.player.listener.*;
import me.nakilex.levelplugin.player.utils.ArrowUtils;
import me.nakilex.levelplugin.potions.listeners.PotionUseListener;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.runes.gui.EquipRunesGUI;
import me.nakilex.levelplugin.runes.gui.IdentifyRunesGUI;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.salvage.listeners.SalvageListener;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.spells.ArcherSpell;
import me.nakilex.levelplugin.spells.RogueSpell;
import me.nakilex.levelplugin.spells.gui.SpellGUIListener;
import me.nakilex.levelplugin.spells.listener.*;
import me.nakilex.levelplugin.trade.listeners.PlayerRightClicksPlayerListener;
import me.nakilex.levelplugin.utils.*;
import me.nakilex.levelplugin.quests.listeners.QuestKillListener;
import me.nakilex.levelplugin.quests.listeners.QuestCraftListener;
import me.nakilex.levelplugin.quests.gui.QuestGUIListener;
import me.nakilex.levelplugin.npc.listeners.NPCDialogMoveListener;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;

public class ListenerRegistry {

    public static void registerListeners(Main plugin,
                                         BlacksmithGUI blacksmithGUI,
                                         HorseGUI horseGUI,
                                         LootChestManager lootChestManager,
                                         PotionManager potionManager,
                                         PartyManager partyManager,
                                         EconomyManager economyManager,
                                         FileConfiguration mobConfig,
                                         MobRewardsConfig mobRewardsConfig,
                                         DmgNumberToggleManager dmgToggleManager,
                                         PickupCustomItemListener pickupCustomItemListener,
                                         SettingsGUI settingsGUI,
                                         RogueSpell rogueSpell,
                                         ProjectileFriendlyFireListener projectileFriendlyFireListener,
                                         FileConfiguration bossConfig,
                                         ArcherSpell archerSpell,
                                         MeteorListener meteorListener,
                                         GemsManager gemsManager,
                                         IdentifyRunesGUI identifyRunesGUI,
                                         RunesManager runesManager,
                                         EquipRunesGUI   equipGui,
                                         ChestHologramListener chestHologramListener,
                                         QuestManager questManager,
                                        NPCDialogManager dialogManager,
                                         PlayerScoreboardManager scoreboardManager) {


        PluginManager pm = plugin.getServer().getPluginManager();

        // Register all listeners
        pm.registerEvents(new MobDamageListener(), plugin);
        pm.registerEvents(new MobDeathListener(plugin.getMobManager(), economyManager), plugin);
        pm.registerEvents(new PlayerKillListener(plugin.getLevelManager(), mobConfig, partyManager), plugin);
        pm.registerEvents(new MythicMobDeathListener(mobRewardsConfig, plugin.getLevelManager(), economyManager, lootChestManager), plugin);
        pm.registerEvents(new PlayerJoinListener(plugin.getLevelManager(),plugin.getPlayerConfig()),plugin);
        pm.registerEvents(new PlayerQuitListener(plugin.getPlayerConfig()),plugin);
        pm.registerEvents(new StatsMenuListener(), plugin);
        pm.registerEvents(new StatsEffectListener(), plugin);
        pm.registerEvents(new ArmorListener(), plugin);
        pm.registerEvents(new ArmorStatsListener(), plugin);
        pm.registerEvents(new WeaponListener(), plugin);
        pm.registerEvents(new WeaponStatsListener(), plugin);
        pm.registerEvents(new ClickComboListener(), plugin);
        pm.registerEvents(new ItemNameDisplayListener(), plugin);
        pm.registerEvents(new StaticItemListener(), plugin);
        pm.registerEvents(new ClassMenuListener(), plugin);
        pm.registerEvents(blacksmithGUI, plugin);
        pm.registerEvents(horseGUI, plugin);
        pm.registerEvents(new NPCClickListener(economyManager, questManager, dialogManager), plugin);
        pm.registerEvents(new NPCCommandListener(), plugin);
        pm.registerEvents(new PlayerRightClicksPlayerListener(), plugin);
        pm.registerEvents(new TradingWindow(), plugin);
        pm.registerEvents(new PartyChatListener(partyManager), plugin);
        pm.registerEvents(new PartyInviteListener(partyManager), plugin);
        pm.registerEvents(new LootChestListener(lootChestManager), plugin);
        pm.registerEvents(new LootChestCloseListener(lootChestManager, economyManager), plugin);
        pm.registerEvents(new PotionUseListener(potionManager, plugin), plugin);
        pm.registerEvents(new MythicMobNameManager(plugin), plugin);
        pm.registerEvents(new MythicMobDamageListener(), plugin);
        pm.registerEvents(new FallDamageDisabler(), plugin);
        pm.registerEvents(new HungerDisabler(), plugin);
        pm.registerEvents(new DuelListener(), plugin);
        pm.registerEvents(new PickupCustomItemListener(plugin), plugin);
        pm.registerEvents(new CustomItemUpdateListener(), plugin);
        pm.registerEvents(new SalvageListener(economyManager, gemsManager), plugin);
        pm.registerEvents(new SpellGUIListener(), plugin);
        pm.registerEvents(new DoubleJumpListener(), plugin);
        pm.registerEvents(new DamageIndicatorListener(dmgToggleManager), plugin);
        pm.registerEvents(new DamageChatListener(), plugin);
        pm.registerEvents(settingsGUI, plugin); // ✅ No constructor call here
        pm.registerEvents(new RogueSpell(), plugin);
        pm.registerEvents(new MeteorListener(), plugin);
        pm.registerEvents(new ArcherSpell(), plugin);
        pm.registerEvents(new ChestHologramListener(lootChestManager), plugin);

        pm.registerEvents(new ProjectileFriendlyFireListener(), plugin);
        pm.registerEvents(new FieldBossListener(plugin, plugin.getBossConfig(), plugin.getItemManager(), plugin.getGemsManager()), plugin);
        pm.registerEvents(new EquipOnJoinListener(), plugin);
        pm.registerEvents(new PlayerDeathListener(plugin), plugin);
        pm.registerEvents(new FullInventoryListener(), plugin);
        pm.registerEvents(new IdentifyRunesGUI(plugin, runesManager), plugin);
        pm.registerEvents(new EquipRunesGUI(plugin, runesManager, identifyRunesGUI), plugin);
        pm.registerEvents(new QuestKillListener(questManager), plugin);
        pm.registerEvents(new QuestCraftListener(questManager), plugin);
        pm.registerEvents(new QuestGUIListener(questManager), plugin);
        pm.registerEvents(new NPCDialogMoveListener(dialogManager), plugin);
        pm.registerEvents(plugin.getScoreboardManager(), plugin);



        // Register ArrowUtils listener and start cleanup task
        ArrowUtils arrowUtils = new ArrowUtils(plugin);
        pm.registerEvents(arrowUtils, plugin);  // Register the listener
        arrowUtils.startArrowCleanupTask();    // Start the task to clean up arrows periodically
    }
}
