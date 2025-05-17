package me.nakilex.levelplugin.utils.registeries;

import de.slikey.effectlib.EffectManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.commands.BlacksmithCommand;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.duels.commands.DuelCommand;
import me.nakilex.levelplugin.economy.commands.*;
import me.nakilex.levelplugin.economy.gui.GemExchangeGUI;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.horse.commands.HorseCommand;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.items.commands.AddItemCommand;
import me.nakilex.levelplugin.merchants.commands.MerchantCommand;
import me.nakilex.levelplugin.mob.commands.AddMobCommand;
import me.nakilex.levelplugin.mob.commands.DmgChatCommand;
import me.nakilex.levelplugin.mob.commands.DmgNumberCommand;
import me.nakilex.levelplugin.mob.commands.ToggleCommand;
import me.nakilex.levelplugin.mob.managers.DmgNumberToggleManager;
import me.nakilex.levelplugin.mob.managers.MobManager;
import me.nakilex.levelplugin.player.attributes.commands.AddPointsCommand;
import me.nakilex.levelplugin.player.attributes.commands.StatsCommand;
import me.nakilex.levelplugin.player.classes.commands.ClassCommand;
import me.nakilex.levelplugin.player.level.commands.AddXPCommand;
import me.nakilex.levelplugin.player.level.commands.SetLevelCommand;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.commands.AddPotionCommand;
import me.nakilex.levelplugin.lootchests.commands.LootChestCommand;
import me.nakilex.levelplugin.salvage.commands.SalvageCommand;
import me.nakilex.levelplugin.settings.commands.SettingsCommand;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.spells.commands.SpellCommand;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.storage.commands.StorageCommand;
import me.nakilex.levelplugin.tips.BroadcastManager;
import me.nakilex.levelplugin.tips.TipsConfigManager;
import me.nakilex.levelplugin.tips.TipsReloadCommand;
import me.nakilex.levelplugin.trade.commands.TradeCommand;
import me.nakilex.levelplugin.party.PartyCommands;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;

public class CommandRegistry {

    public static void registerCommands(Main plugin,
                                        BlacksmithGUI blacksmithGUI,
                                        HorseGUI horseGUI,
                                        LevelManager levelManager,
                                        EconomyManager economyManager,
                                        PartyManager partyManager,
                                        PotionManager potionManager,
                                        LootChestManager lootChestManager,
                                        ConfigManager configManager,
                                        HorseManager horseManager,
                                        MobManager mobManager,
                                        StorageManager storageManager,
                                        DmgNumberToggleManager dmgToggleManager,
                                        SettingsGUI settingsGUI,
                                        GemsManager gemsManager,
                                        GemExchangeGUI gemGui,
                                        TipsConfigManager tipsCfg,
                                        BroadcastManager broadcastMgr) { // ✅ added here


        plugin.getCommand("addpoints").setExecutor(new AddPointsCommand());
        plugin.getCommand("addxp").setExecutor(new AddXPCommand(levelManager));
        plugin.getCommand("stats").setExecutor(new StatsCommand());
        plugin.getCommand("additem").setExecutor(new AddItemCommand());
        plugin.getCommand("setlevel").setExecutor(new SetLevelCommand(plugin));
        plugin.getCommand("class").setExecutor(new ClassCommand());
        plugin.getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        plugin.getCommand("addcoins").setExecutor(new AddCoinsCommand(economyManager));
        plugin.getCommand("blacksmith").setExecutor(new BlacksmithCommand(blacksmithGUI));
        plugin.getCommand("horse").setExecutor(new HorseCommand(horseManager, horseGUI));
        plugin.getCommand("party").setExecutor(new PartyCommands(partyManager));
        plugin.getCommand("addpotion").setExecutor(new AddPotionCommand(potionManager, plugin));
        plugin.getCommand("lootchest").setExecutor(new LootChestCommand(configManager, lootChestManager));
        plugin.getCommand("trade").setExecutor(new TradeCommand());
        plugin. getCommand("addmob").setExecutor(new AddMobCommand(mobManager));
        plugin.getCommand("duel").setExecutor(new DuelCommand());
        plugin.getCommand("ps").setExecutor(new StorageCommand(storageManager));
        plugin.getCommand("merchant").setExecutor(new MerchantCommand(plugin));
        plugin.getCommand("salvage").setExecutor(new SalvageCommand(plugin));
        plugin.getCommand("spells").setExecutor(new SpellCommand());
        plugin.getCommand("dmgnumber").setExecutor(new DmgNumberCommand(dmgToggleManager));
        plugin.getCommand("dmgchat").setExecutor(new DmgChatCommand());
        plugin.getCommand("settings").setExecutor(new SettingsCommand(settingsGUI));
        plugin.getCommand("addgems").setExecutor(new AddGemsCommand(gemsManager));
        plugin.getCommand("gems").setExecutor(new GemsBalanceCommand(gemsManager));
        plugin.getCommand("gemexchange").setExecutor(new GemExchangeCommand(gemGui));
        plugin.getCommand("tipsreload").setExecutor(new TipsReloadCommand(tipsCfg, broadcastMgr));
        plugin.getCommand("toggle").setExecutor(new ToggleCommand(plugin));


    }
}
