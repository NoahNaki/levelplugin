package me.nakilex.levelplugin.utils.registeries;

import de.slikey.effectlib.EffectManager;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.commands.BlacksmithCommand;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.arena.commands.ArenaCommand;
import me.nakilex.levelplugin.duels.commands.DuelCommand;
import me.nakilex.levelplugin.economy.commands.*;
import me.nakilex.levelplugin.economy.gui.GemExchangeGUI;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.friend.IgnoreCommand;
import me.nakilex.levelplugin.friend.UnignoreCommand;
import me.nakilex.levelplugin.horse.commands.HorseCommand;
import me.nakilex.levelplugin.horse.gui.HorseGUI;
import me.nakilex.levelplugin.items.commands.AddItemCommand;
import me.nakilex.levelplugin.items.commands.AddLoreCommand;
import me.nakilex.levelplugin.items.commands.OpSwordCommand;
import me.nakilex.levelplugin.merchants.commands.MerchantCommand;
import me.nakilex.levelplugin.mob.commands.DmgChatCommand;
import me.nakilex.levelplugin.mob.commands.DmgNumberCommand;
import me.nakilex.levelplugin.mob.commands.ToggleCommand;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.debug.commands.DebugCommand;
import me.nakilex.levelplugin.player.attributes.commands.AddPointsCommand;
import me.nakilex.levelplugin.player.attributes.commands.StatsCommand;
import me.nakilex.levelplugin.player.classes.commands.ClassCommand;
import me.nakilex.levelplugin.player.classes.commands.GenClassCommand;
import me.nakilex.levelplugin.player.classes.commands.EssenceCommand;
import me.nakilex.levelplugin.player.classes.commands.EssenceUpgradeCommand;
import me.nakilex.levelplugin.player.classes.commands.SealingCharmCommand;
import me.nakilex.levelplugin.player.level.commands.AddXPCommand;
import me.nakilex.levelplugin.player.mining.commands.AddMiningXPCommand;
import me.nakilex.levelplugin.player.mining.commands.MiningLevelCommand;
import me.nakilex.levelplugin.player.mining.commands.SetMiningLevelCommand;
import me.nakilex.levelplugin.player.level.commands.SetLevelCommand;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.battlepass.command.BattlePassCommand;
import me.nakilex.levelplugin.player.battlepass.command.BattlePassXpCommand;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.commands.WipeProfileCommand;
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
import me.nakilex.levelplugin.auctionhouse.AuctionCommand;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseGUI;
import me.nakilex.levelplugin.auctionhouse.AuctionHouseManager;
import me.nakilex.levelplugin.party.PartyCommands;
import me.nakilex.levelplugin.party.PartyGlowCommand;
import me.nakilex.levelplugin.friend.FriendCommand;
import me.nakilex.levelplugin.friend.FriendGlowCommand;
import me.nakilex.levelplugin.friend.FriendGUI;
import me.nakilex.levelplugin.friend.FriendsCommand;
import me.nakilex.levelplugin.codex.CodexMainGUI;
import me.nakilex.levelplugin.codex.CodexCommand;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantCommand;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager;
import me.nakilex.levelplugin.music.commands.SkipSongCommand;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.lootchests.config.ConfigManager;
import me.nakilex.levelplugin.quests.commands.QuestCommand;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.fasttravel.commands.LocationCommand;
import me.nakilex.levelplugin.fasttravel.commands.FastTravelCommand;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.environment.TownCommand;
import me.nakilex.levelplugin.environment.UpgradeGUI;
import me.nakilex.levelplugin.environment.stage.TownStageCommand;
import me.nakilex.levelplugin.environment.stage.BuildingStageCommand;
import me.nakilex.levelplugin.environment.stage.TownPosCommand;
import me.nakilex.levelplugin.environment.stage.StageSelectionListener;
import me.nakilex.levelplugin.utils.commands.CenterGuiCommand;
import me.nakilex.levelplugin.utils.commands.CenterTooltipCommand;
import me.nakilex.levelplugin.utils.commands.EmptyTabCompleter;
import me.nakilex.levelplugin.pathfinding.PathfindingCommand;
import me.nakilex.levelplugin.pathfinding.PathfindingManager;
import me.nakilex.levelplugin.pathfinding.MercenaryCommand;
import me.nakilex.levelplugin.chat.ChatModerationCommand;
import me.nakilex.levelplugin.chat.ChatCommand;
import me.nakilex.levelplugin.chat.RollCommand;
import org.bukkit.command.PluginCommand;
import me.nakilex.levelplugin.pathfinding.MercenaryManager;

public class CommandRegistry {

    public static void registerCommands(Main plugin,
                                        BlacksmithGUI blacksmithGUI,
                                        HorseGUI horseGUI,
                                        LevelManager levelManager,
                                        me.nakilex.levelplugin.player.mining.managers.MiningManager miningManager,
                                        EconomyManager economyManager,
                                        PartyManager partyManager,
                                        me.nakilex.levelplugin.guild.GuildManager guildManager,
                                        me.nakilex.levelplugin.guild.GuildGUI guildGui,
                                        me.nakilex.levelplugin.guild.GuildMemberGUI guildMemberGui,
                                        PotionManager potionManager,
                                        LootChestManager lootChestManager,
                                        ConfigManager configManager,
                                        HorseManager horseManager,
                                        StorageManager storageManager,
                                        PlayerToggleManager dmgToggleManager,
                                        PlayerToggleManager mobDebugToggleManager,
                                        SettingsGUI settingsGUI,
                                        me.nakilex.levelplugin.debug.gui.DebugGUI debugGUI,
                                        GemsManager gemsManager,
                                        GemExchangeGUI gemGui,
                                        AuctionHouseManager auctionMgr,
                                        AuctionHouseGUI auctionGui,
                                        TipsConfigManager tipsCfg,
                                        me.nakilex.levelplugin.enchanting.gui.EnchantGUI enchantGUI,
                                        BroadcastManager broadcastMgr,
                                        QuestManager questManager,
                                        FastTravelManager fastTravelManager,
                                        me.nakilex.levelplugin.motd.MotdManager motdManager,
                                        UpgradeGUI upgradeGUI,
                                        CodexMainGUI codexGUI,
                                        WanderingMerchantManager wmManager,
                                        PathfindingManager pathManager,
                                        MercenaryManager mercManager,
                                        BattlePassManager battlePassManager) {


        plugin.getCommand("addpoints").setExecutor(new AddPointsCommand());
        plugin.getCommand("addxp").setExecutor(new AddXPCommand(levelManager));
        plugin.getCommand("addminingxp").setExecutor(new AddMiningXPCommand(miningManager));
        plugin.getCommand("mininglevel").setExecutor(new MiningLevelCommand(miningManager));
        plugin.getCommand("setmininglevel").setExecutor(new me.nakilex.levelplugin.player.mining.commands.SetMiningLevelCommand(miningManager));
        plugin.getCommand("stats").setExecutor(new StatsCommand());
        plugin.getCommand("additem").setExecutor(new AddItemCommand());
        plugin.getCommand("opsword").setExecutor(new OpSwordCommand());
        me.nakilex.levelplugin.items.commands.GenerateItemCommand genItemCmd = new me.nakilex.levelplugin.items.commands.GenerateItemCommand();
        plugin.getCommand("genitem").setExecutor(genItemCmd);
        plugin.getCommand("genitem").setTabCompleter(genItemCmd);
        GenClassCommand genClassCmd = new GenClassCommand();
        plugin.getCommand("genclass").setExecutor(genClassCmd);
        plugin.getCommand("genclass").setTabCompleter(genClassCmd);
        plugin.getCommand("essence").setExecutor(new EssenceCommand());
        EssenceUpgradeCommand essenceUpgradeCmd = new EssenceUpgradeCommand();
        plugin.getCommand("essenceupgrade").setExecutor(essenceUpgradeCmd);
        plugin.getCommand("essenceupgrade").setTabCompleter(essenceUpgradeCmd);
        SealingCharmCommand sealingCharmCmd = new SealingCharmCommand();
        plugin.getCommand("sealingcharm").setExecutor(sealingCharmCmd);
        plugin.getCommand("sealingcharm").setTabCompleter(sealingCharmCmd);
        plugin.getCommand("setlevel").setExecutor(new SetLevelCommand(plugin));
        ClassCommand classCmd = new ClassCommand();
        plugin.getCommand("class").setExecutor(classCmd);
        plugin.getCommand("class").setTabCompleter(classCmd);
        plugin.getCommand("subclass").setExecutor(new me.nakilex.levelplugin.player.classes.commands.SubclassCommand());
        plugin.getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        AddCoinsCommand addCoinsCmd = new AddCoinsCommand(economyManager);
        plugin.getCommand("addcoins").setExecutor(addCoinsCmd);
        plugin.getCommand("addcoins").setTabCompleter(addCoinsCmd);
        plugin.getCommand("blacksmith").setExecutor(new BlacksmithCommand(blacksmithGUI));
        plugin.getCommand("horse").setExecutor(new HorseCommand(horseManager, horseGUI));
        plugin.getCommand("party").setExecutor(new PartyCommands(partyManager));
        me.nakilex.levelplugin.guild.GuildCommand guildCmd = new me.nakilex.levelplugin.guild.GuildCommand(guildManager, guildGui, guildMemberGui);
        plugin.getCommand("guild").setExecutor(guildCmd);
        plugin.getCommand("guild").setTabCompleter(guildCmd);
        plugin.getCommand("partyglow").setExecutor(new PartyGlowCommand(plugin.getPartyGlowManager()));
        FriendCommand friendCmd = new FriendCommand(plugin.getFriendManager());
        plugin.getCommand("friend").setExecutor(friendCmd);
        plugin.getCommand("friend").setTabCompleter(friendCmd);
        FriendGUI friendGui = new FriendGUI(plugin.getFriendManager());
        plugin.getCommand("friends").setExecutor(new FriendsCommand(friendGui));
        plugin.getCommand("friendglow").setExecutor(new FriendGlowCommand(plugin.getFriendGlowManager()));
        plugin.getCommand("ignore").setExecutor(new IgnoreCommand(plugin.getIgnoreManager()));
        plugin.getCommand("unignore").setExecutor(new UnignoreCommand(plugin.getIgnoreManager()));
        AddPotionCommand addPotionCmd = new AddPotionCommand(potionManager, plugin);
        plugin.getCommand("addpotion").setExecutor(addPotionCmd);
        plugin.getCommand("addpotion").setTabCompleter(addPotionCmd);
        plugin.getCommand("lootchest").setExecutor(new LootChestCommand(configManager, lootChestManager));
        plugin.getCommand("trade").setExecutor(new TradeCommand());
        plugin.getCommand("duel").setExecutor(new DuelCommand());
        ArenaCommand arenaCmd = new ArenaCommand(plugin.getArenaQueueGUI(), plugin.getArenaQueueManager());
        plugin.getCommand("arena").setExecutor(arenaCmd);
        plugin.getCommand("arena").setTabCompleter(arenaCmd);
        plugin.getCommand("ps").setExecutor(new StorageCommand(storageManager));
        plugin.getCommand("merchant").setExecutor(new MerchantCommand(plugin));
        plugin.getCommand("salvage").setExecutor(new SalvageCommand(plugin));
        plugin.getCommand("enchant").setExecutor(new me.nakilex.levelplugin.enchanting.commands.EnchantCommand(enchantGUI));
        plugin.getCommand("spells").setExecutor(new SpellCommand());
        BattlePassCommand battlePassCommand = new BattlePassCommand(battlePassManager);
        plugin.getCommand("battlepass").setExecutor(battlePassCommand);
        plugin.getCommand("battlepass").setTabCompleter(new EmptyTabCompleter());
        BattlePassXpCommand battlePassXpCommand = new BattlePassXpCommand(battlePassManager);
        plugin.getCommand("bpxp").setExecutor(battlePassXpCommand);
        plugin.getCommand("bpxp").setTabCompleter(battlePassXpCommand);
        plugin.getCommand("dmgnumber").setExecutor(new DmgNumberCommand(dmgToggleManager));
        plugin.getCommand("dmgchat").setExecutor(new DmgChatCommand(settingsGUI.getSettingsManager()));
        plugin.getCommand("settings").setExecutor(new SettingsCommand(settingsGUI));
        AddGemsCommand addGemsCmd = new AddGemsCommand(gemsManager);
        plugin.getCommand("addgems").setExecutor(addGemsCmd);
        plugin.getCommand("addgems").setTabCompleter(addGemsCmd);
        plugin.getCommand("gems").setExecutor(new GemsBalanceCommand(gemsManager));
        plugin.getCommand("gemexchange").setExecutor(new GemExchangeCommand(gemGui));
        plugin.getCommand("tipsreload").setExecutor(new TipsReloadCommand(tipsCfg, broadcastMgr));
        ToggleCommand toggleCmd = new ToggleCommand(plugin);
        plugin.getCommand("toggle").setExecutor(toggleCmd);
        plugin.getCommand("toggle").setTabCompleter(toggleCmd);
        plugin.getCommand("skipsong").setExecutor(new SkipSongCommand(plugin));
        plugin.getCommand("auctionhouse").setExecutor(new AuctionCommand(auctionMgr, auctionGui));
        QuestCommand questCmd = new QuestCommand(questManager);
        plugin.getCommand("quest").setExecutor(questCmd);
        plugin.getCommand("quest").setTabCompleter(questCmd);
        LocationCommand locationCmd = new LocationCommand(fastTravelManager);
        plugin.getCommand("location").setExecutor(locationCmd);
        plugin.getCommand("location").setTabCompleter(locationCmd);
        FastTravelCommand fastTravelCmd = new FastTravelCommand(plugin.getFastTravelGUI());
        plugin.getCommand("fasttravel").setExecutor(fastTravelCmd);
        plugin.getCommand("fasttravel").setTabCompleter(fastTravelCmd);
        plugin.getCommand("travel").setExecutor(fastTravelCmd);
        plugin.getCommand("travel").setTabCompleter(fastTravelCmd);
        me.nakilex.levelplugin.player.commands.ProfileCommand profileCmd =
                new me.nakilex.levelplugin.player.commands.ProfileCommand();
        plugin.getCommand("profile").setExecutor(profileCmd);
        plugin.getCommand("profile").setTabCompleter(profileCmd);
        plugin.getCommand("wipeprofile").setExecutor(new WipeProfileCommand());

        DebugCommand debugCmd = new DebugCommand(mobDebugToggleManager,
                plugin.getScoreboardManager(),
                debugGUI);
        plugin.getCommand("debug").setExecutor(debugCmd);
        plugin.getCommand("debug").setTabCompleter(debugCmd);
        plugin.getCommand("motd").setExecutor(new me.nakilex.levelplugin.motd.MotdCommand(motdManager));
        me.nakilex.levelplugin.fakeblock.FakeGateCommand fakeGateCmd =
                new me.nakilex.levelplugin.fakeblock.FakeGateCommand(plugin);
        plugin.getCommand("fakegate").setExecutor(fakeGateCmd);
        plugin.getCommand("fakegate").setTabCompleter(fakeGateCmd);
        me.nakilex.levelplugin.fakeblock.ModelGateCommand modelGateCmd =
                new me.nakilex.levelplugin.fakeblock.ModelGateCommand(plugin);
        plugin.getCommand("modelgate").setExecutor(modelGateCmd);
        plugin.getCommand("modelgate").setTabCompleter(modelGateCmd);
        plugin.getCommand("town").setExecutor(new TownCommand(upgradeGUI, plugin.getEnvironmentManager()));
        TownStageCommand townStageCmd = new TownStageCommand(plugin.getTownStageManager());
        plugin.getCommand("townstage").setExecutor(townStageCmd);
        plugin.getCommand("townstage").setTabCompleter(townStageCmd);
        plugin.getCommand("buildingstage").setExecutor(new BuildingStageCommand(plugin, plugin.getBuildingStageManager()));
        plugin.getCommand("townpos1").setExecutor(new TownPosCommand(true));
        plugin.getCommand("townpos2").setExecutor(new TownPosCommand(false));
        plugin.getServer().getPluginManager().registerEvents(new StageSelectionListener(), plugin);

        plugin.getCommand("codex").setExecutor(new CodexCommand(codexGUI));
        plugin.getCommand("wm").setExecutor(new WanderingMerchantCommand(wmManager));

        plugin.getCommand("cutscene").setExecutor(new me.nakilex.levelplugin.cutscene.commands.CutsceneCommand(plugin.getCutsceneManager()));
        plugin.getCommand("dungeon").setExecutor(new me.nakilex.levelplugin.dungeon.DungeonCommand(plugin));
        plugin.getCommand("world").setExecutor(new me.nakilex.levelplugin.world.WorldCommand(plugin.getWorldManager()));
        plugin.getCommand("centertooltip").setExecutor(new CenterTooltipCommand());
        plugin.getCommand("centergui").setExecutor(new CenterGuiCommand());
        plugin.getCommand("addlore").setExecutor(new AddLoreCommand());
        PathfindingCommand pfCmd = new PathfindingCommand(pathManager);
        plugin.getCommand("pathfinding").setExecutor(pfCmd);
        plugin.getCommand("pathfinding").setTabCompleter(pfCmd);

        MercenaryCommand mercCmd = new MercenaryCommand(mercManager);
        plugin.getCommand("mercenary").setExecutor(mercCmd);
        plugin.getCommand("mercenary").setTabCompleter(mercCmd);

        ChatCommand channelCmd = new ChatCommand(plugin);
        plugin.getCommand("chat").setExecutor(channelCmd);
        plugin.getCommand("chat").setTabCompleter(channelCmd);

        plugin.getCommand("roll").setExecutor(new RollCommand());

        ChatModerationCommand chatCmd = new ChatModerationCommand();
        plugin.getCommand("mute").setExecutor(chatCmd);
        plugin.getCommand("mute").setTabCompleter(chatCmd);
        plugin.getCommand("unmute").setExecutor(chatCmd);
        plugin.getCommand("unmute").setTabCompleter(chatCmd);
        plugin.getCommand("clearchat").setExecutor(chatCmd);
        plugin.getCommand("clearchat").setTabCompleter(chatCmd);

        // Ensure every command has a tab completer to avoid null completions
        EmptyTabCompleter empty = new EmptyTabCompleter();
        for (String name : plugin.getDescription().getCommands().keySet()) {
            PluginCommand cmd = plugin.getCommand(name);
            if (cmd != null && cmd.getTabCompleter() == null) {
                cmd.setTabCompleter(empty);
            }
        }
    }
}
