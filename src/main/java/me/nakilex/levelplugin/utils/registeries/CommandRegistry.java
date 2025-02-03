package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.commands.BlacksmithCommand;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.duels.commands.DuelCommand;
import me.nakilex.levelplugin.economy.commands.AddCoinsCommand;
import me.nakilex.levelplugin.economy.commands.BalanceCommand;
import me.nakilex.levelplugin.effects.commands.EffectCommand;
import me.nakilex.levelplugin.horse.commands.HorseCommand;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.items.commands.AddItemCommand;
import me.nakilex.levelplugin.merchant.commands.MerchantCommand;
import me.nakilex.levelplugin.mob.commands.AddMobCommand;
import me.nakilex.levelplugin.mob.managers.MobManager;
import me.nakilex.levelplugin.player.attributes.commands.AddPointsCommand;
import me.nakilex.levelplugin.player.attributes.commands.StatsCommand;
import me.nakilex.levelplugin.player.classes.commands.ClassCommand;
import me.nakilex.levelplugin.player.level.commands.AddXPCommand;
import me.nakilex.levelplugin.player.level.commands.SetLevelCommand;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.potions.commands.AddPotionCommand;
import me.nakilex.levelplugin.lootchests.commands.LootChestCommand;
import me.nakilex.levelplugin.storage.StorageManager;
import me.nakilex.levelplugin.storage.commands.StorageCommand;
import me.nakilex.levelplugin.trade.commands.TradeCommand;
import me.nakilex.levelplugin.party.PartyCommands;
import me.nakilex.levelplugin.effects.managers.EffectManager;
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
                                        EffectManager effectManager,
                                        PotionManager potionManager,
                                        LootChestManager lootChestManager,
                                        ConfigManager configManager,
                                        HorseManager horseManager,
                                        MobManager mobManager,
                                        StorageManager storageManager) {

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
        plugin.getCommand("effect").setExecutor(new EffectCommand(effectManager));
        plugin.getCommand("party").setExecutor(new PartyCommands(partyManager));
        plugin.getCommand("addpotion").setExecutor(new AddPotionCommand(potionManager, plugin));
        plugin.getCommand("lootchest").setExecutor(new LootChestCommand(configManager, lootChestManager));
        plugin.getCommand("trade").setExecutor(new TradeCommand());
        plugin. getCommand("addmob").setExecutor(new AddMobCommand(mobManager));
        plugin.getCommand("duel").setExecutor(new DuelCommand());
        plugin.getCommand("merchant").setExecutor(new MerchantCommand(plugin));
        plugin.getCommand("ps").setExecutor(new StorageCommand(storageManager));



    }
}
