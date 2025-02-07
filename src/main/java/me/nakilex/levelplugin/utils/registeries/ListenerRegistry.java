package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.duels.listeners.DuelListener;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.effects.listeners.StatsEffectListener;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.lootchests.listeners.LootChestCloseListener;
import me.nakilex.levelplugin.lootchests.listeners.LootChestListener;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.merchant.listeners.MerchantListener;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.listeners.MobDamageListener;
import me.nakilex.levelplugin.mob.listeners.MobDeathListener;
import me.nakilex.levelplugin.mob.listeners.MythicMobDamageListener;
import me.nakilex.levelplugin.mob.listeners.MythicMobDeathListener;
import me.nakilex.levelplugin.mob.managers.MythicMobNameManager;
import me.nakilex.levelplugin.npc.listeners.NPCClickListener;
import me.nakilex.levelplugin.npc.listeners.NPCCommandListener;
import me.nakilex.levelplugin.party.PartyChatListener;
import me.nakilex.levelplugin.party.PartyInviteListener;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.attributes.listeners.StatsMenuListener;
import me.nakilex.levelplugin.player.classes.listeners.ArcherDoubleJumpListener;
import me.nakilex.levelplugin.player.classes.listeners.ClassMenuListener;
import me.nakilex.levelplugin.player.listener.ClickComboListener;
import me.nakilex.levelplugin.player.listener.PlayerJoinListener;
import me.nakilex.levelplugin.player.listener.PlayerKillListener;
import me.nakilex.levelplugin.player.utils.ArrowUtils;
import me.nakilex.levelplugin.potions.listeners.PotionUseListener;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.items.listeners.*;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.trade.listeners.PlayerRightClicksPlayerListener;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.utils.FallDamageDisabler;
import me.nakilex.levelplugin.utils.HungerDisabler;
import me.nakilex.levelplugin.utils.TradingWindow;
import net.citizensnpcs.api.util.Storage;
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
                                         MobRewardsConfig mobRewardsConfig) {

        PluginManager pm = plugin.getServer().getPluginManager();

        // Register all listeners
        pm.registerEvents(new MobDamageListener(), plugin);
        pm.registerEvents(new MobDeathListener(plugin.getMobManager(), economyManager), plugin);
        pm.registerEvents(new PlayerKillListener(plugin.getLevelManager(), mobConfig, partyManager), plugin);
        pm.registerEvents(new MythicMobDeathListener(mobRewardsConfig, plugin.getLevelManager(), economyManager), plugin);
        pm.registerEvents(new PlayerJoinListener(plugin.getLevelManager()), plugin);
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
        pm.registerEvents(new NPCClickListener(economyManager), plugin);
        pm.registerEvents(new NPCCommandListener(), plugin);
        pm.registerEvents(new PlayerRightClicksPlayerListener(), plugin);
        pm.registerEvents(new TradingWindow(), plugin);
        pm.registerEvents(new PartyChatListener(partyManager), plugin);
        pm.registerEvents(new PartyInviteListener(partyManager), plugin);
        pm.registerEvents(new LootChestListener(lootChestManager), plugin);
        pm.registerEvents(new LootChestCloseListener(lootChestManager), plugin);
        pm.registerEvents(new PotionUseListener(potionManager, plugin), plugin);
        pm.registerEvents(new MythicMobNameManager(plugin), plugin);
        pm.registerEvents(new MythicMobDamageListener(), plugin);
        pm.registerEvents(new FallDamageDisabler(), plugin);
        pm.registerEvents(new HungerDisabler(), plugin);
        pm.registerEvents(new DuelListener(), plugin);
        pm.registerEvents(new MerchantListener(new EconomyManager(plugin)), plugin);
        pm.registerEvents(new ArcherDoubleJumpListener(), plugin);





        // Register ArrowUtils listener and start cleanup task
        ArrowUtils arrowUtils = new ArrowUtils(plugin);
        pm.registerEvents(arrowUtils, plugin);  // Register the listener
        arrowUtils.startArrowCleanupTask();    // Start the task to clean up arrows periodically
    }
}
