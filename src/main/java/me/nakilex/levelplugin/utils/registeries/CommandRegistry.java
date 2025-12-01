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
import me.nakilex.levelplugin.mob.commands.DpsDummyCommand;
import me.nakilex.levelplugin.mob.commands.ToggleCommand;
import me.nakilex.levelplugin.mob.dps.DpsDummyManager;
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
import me.nakilex.levelplugin.player.battlepass.command.BattlePassUnlockCommand;
import me.nakilex.levelplugin.player.battlepass.command.BattlePassXpCommand;
import me.nakilex.levelplugin.booster.BoosterCommand;
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
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.commands.LevelPluginCommand;
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
                                        BattlePassManager battlePassManager,
                                        ChatGameManager chatGameManager,
                                        DpsDummyManager dpsDummyManager) {


        AddPointsCommand addPointsCmd = new AddPointsCommand();
        plugin.getCommand("addpoints").setExecutor(addPointsCmd);
        plugin.getCommand("addpoints").setTabCompleter(addPointsCmd);

        AddXPCommand addXpCmd = new AddXPCommand(levelManager);
        plugin.getCommand("addxp").setExecutor(addXpCmd);
        plugin.getCommand("addxp").setTabCompleter(addXpCmd);

        AddMiningXPCommand addMiningXpCmd = new AddMiningXPCommand(miningManager);
        plugin.getCommand("addminingxp").setExecutor(addMiningXpCmd);
        plugin.getCommand("addminingxp").setTabCompleter(addMiningXpCmd);

        plugin.getCommand("mininglevel").setExecutor(new MiningLevelCommand(miningManager));

        me.nakilex.levelplugin.player.mining.commands.SetMiningLevelCommand setMiningLevelCmd =
                new me.nakilex.levelplugin.player.mining.commands.SetMiningLevelCommand(miningManager);
        plugin.getCommand("setmininglevel").setExecutor(setMiningLevelCmd);
        plugin.getCommand("setmininglevel").setTabCompleter(setMiningLevelCmd);
        plugin.getCommand("stats").setExecutor(new StatsCommand());
        AddItemCommand addItemCmd = new AddItemCommand();
        plugin.getCommand("additem").setExecutor(addItemCmd);
        plugin.getCommand("additem").setTabCompleter(addItemCmd);
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
        SetLevelCommand setLevelCmd = new SetLevelCommand(plugin);
        plugin.getCommand("setlevel").setExecutor(setLevelCmd);
        plugin.getCommand("setlevel").setTabCompleter(setLevelCmd);
        ClassCommand classCmd = new ClassCommand();
        plugin.getCommand("class").setExecutor(classCmd);
        plugin.getCommand("class").setTabCompleter(classCmd);
        plugin.getCommand("subclass").setExecutor(new me.nakilex.levelplugin.player.classes.commands.SubclassCommand());
        plugin.getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        AddCoinsCommand addCoinsCmd = new AddCoinsCommand(economyManager);
        plugin.getCommand("addcoins").setExecutor(addCoinsCmd);
        plugin.getCommand("addcoins").setTabCompleter(addCoinsCmd);
        BoosterCommand boosterCommand = new BoosterCommand(2.0);
        plugin.getCommand("booster").setExecutor(boosterCommand);
        plugin.getCommand("booster").setTabCompleter(boosterCommand);
        plugin.getCommand("blacksmith").setExecutor(new BlacksmithCommand(blacksmithGUI));
        HorseCommand horseCommand = new HorseCommand(horseManager, horseGUI);
        plugin.getCommand("horse").setExecutor(horseCommand);
        plugin.getCommand("horse").setTabCompleter(horseCommand);
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
        TradeCommand tradeCmd = new TradeCommand();
        plugin.getCommand("trade").setExecutor(tradeCmd);
        plugin.getCommand("trade").setTabCompleter(tradeCmd);
        DuelCommand duelCommand = new DuelCommand();
        plugin.getCommand("duel").setExecutor(duelCommand);
        plugin.getCommand("duel").setTabCompleter(duelCommand);
        ArenaCommand arenaCmd = new ArenaCommand(plugin.getArenaQueueGUI(), plugin.getArenaQueueManager());
        plugin.getCommand("arena").setExecutor(arenaCmd);
        plugin.getCommand("arena").setTabCompleter(arenaCmd);
        plugin.getCommand("ps").setExecutor(new StorageCommand(storageManager));
        MerchantCommand merchantCommand = new MerchantCommand(plugin);
        plugin.getCommand("merchant").setExecutor(merchantCommand);
        plugin.getCommand("merchant").setTabCompleter(merchantCommand);
        plugin.getCommand("salvage").setExecutor(new SalvageCommand(plugin));
        plugin.getCommand("enchant").setExecutor(new me.nakilex.levelplugin.enchanting.commands.EnchantCommand(enchantGUI));
        plugin.getCommand("spells").setExecutor(new SpellCommand());
        BattlePassCommand battlePassCommand = new BattlePassCommand(battlePassManager);
        plugin.getCommand("battlepass").setExecutor(battlePassCommand);
        plugin.getCommand("battlepass").setTabCompleter(new EmptyTabCompleter());
        BattlePassXpCommand battlePassXpCommand = new BattlePassXpCommand(battlePassManager);
        plugin.getCommand("bpxp").setExecutor(battlePassXpCommand);
        plugin.getCommand("bpxp").setTabCompleter(battlePassXpCommand);
        BattlePassUnlockCommand battlePassUnlockCommand = new BattlePassUnlockCommand(battlePassManager);
        plugin.getCommand("bpunlock").setExecutor(battlePassUnlockCommand);
        plugin.getCommand("bpunlock").setTabCompleter(battlePassUnlockCommand);
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
        DpsDummyCommand dummyCmd = new DpsDummyCommand(dpsDummyManager);
        plugin.getCommand("dpsdummy").setExecutor(dummyCmd);
        plugin.getCommand("dpsdummy").setTabCompleter(dummyCmd);
        plugin.getCommand("skipsong").setExecutor(new SkipSongCommand(plugin));
        AuctionCommand auctionCmd = new AuctionCommand(auctionMgr, auctionGui);
        plugin.getCommand("auctionhouse").setExecutor(auctionCmd);
        plugin.getCommand("auctionhouse").setTabCompleter(auctionCmd);
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
                debugGUI,
                chatGameManager,
                plugin.getMercenaryExpeditionManager(),
                plugin.getDropDebugManager(),
                plugin.getAutoCastManager());
        plugin.getCommand("debug").setExecutor(debugCmd);
        plugin.getCommand("debug").setTabCompleter(debugCmd);

        LevelPluginCommand levelPluginCommand = new LevelPluginCommand(plugin);
        plugin.getCommand("levelplugin").setExecutor(levelPluginCommand);
        plugin.getCommand("levelplugin").setTabCompleter(levelPluginCommand);
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
        me.nakilex.levelplugin.dungeon.DungeonCommand dungeonCmd =
                new me.nakilex.levelplugin.dungeon.DungeonCommand(plugin);
        plugin.getCommand("dungeon").setExecutor(dungeonCmd);
        plugin.getCommand("dungeon").setTabCompleter(dungeonCmd);
        me.nakilex.levelplugin.world.WorldCommand worldCmd = new me.nakilex.levelplugin.world.WorldCommand(plugin.getWorldManager());
        plugin.getCommand("world").setExecutor(worldCmd);
        plugin.getCommand("world").setTabCompleter(worldCmd);
        plugin.getCommand("centertooltip").setExecutor(new CenterTooltipCommand());
        plugin.getCommand("centergui").setExecutor(new CenterGuiCommand());
        AddLoreCommand addLoreCmd = new AddLoreCommand();
        plugin.getCommand("addlore").setExecutor(addLoreCmd);
        plugin.getCommand("addlore").setTabCompleter(addLoreCmd);
        PathfindingCommand pfCmd = new PathfindingCommand(pathManager);
        plugin.getCommand("pathfinding").setExecutor(pfCmd);
        plugin.getCommand("pathfinding").setTabCompleter(pfCmd);

        MercenaryCommand mercCmd = new MercenaryCommand(mercManager);
        plugin.getCommand("mercenary").setExecutor(mercCmd);
        plugin.getCommand("mercenary").setTabCompleter(mercCmd);

        me.nakilex.levelplugin.mercenary.ExpeditionCommand expeditionCmd = new me.nakilex.levelplugin.mercenary.ExpeditionCommand(
                plugin,
                plugin.getMercenaryAffinityManager(),
                plugin.getMercenaryExpeditionManager(),
                plugin.getMercenaryFriendshipGUI(),
                plugin.getMercenaryExpeditionGUI(),
                plugin.getMercenaryExpeditionRewardsGUI());
        plugin.getCommand("expedition").setExecutor(expeditionCmd);
        plugin.getCommand("giftbrowser").setExecutor(
                new me.nakilex.levelplugin.mercenary.GiftBrowserCommand(plugin.getMercenaryGiftBrowserGUI()));

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
