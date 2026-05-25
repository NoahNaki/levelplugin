package me.nakilex.levelplugin.utils.registeries;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.blacksmith.commands.BlacksmithCommand;
import me.nakilex.levelplugin.blacksmith.gui.BlacksmithGUI;
import me.nakilex.levelplugin.arena.commands.ArenaCommand;
import me.nakilex.levelplugin.stronghold.commands.StrongholdCommand;
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
import me.nakilex.levelplugin.mob.commands.CustomMobCommand;
import me.nakilex.levelplugin.mob.dps.DpsDummyManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.commands.PetDebugCommand;
import me.nakilex.levelplugin.pet.commands.PetCommand;
import me.nakilex.levelplugin.pet.commands.PetSummonCommand;
import me.nakilex.levelplugin.pet.gui.PetGUI;
import me.nakilex.levelplugin.pet.gui.PetSummonGUI;
import me.nakilex.levelplugin.debug.BeaconEntityDebugManager;
import me.nakilex.levelplugin.debug.commands.DebugCommand;
import me.nakilex.levelplugin.debug.commands.SpawnEntityModelCommand;
import me.nakilex.levelplugin.debug.commands.MageFireballDebugCommand;
import me.nakilex.levelplugin.player.attributes.commands.AddPointsCommand;
import me.nakilex.levelplugin.player.attributes.commands.LifeSkillCommand;
import me.nakilex.levelplugin.player.attributes.commands.StatsCommand;
import me.nakilex.levelplugin.player.level.commands.AddXPCommand;
import me.nakilex.levelplugin.player.mining.commands.MiningLevelCommand;
import me.nakilex.levelplugin.player.farming.commands.FarmingLevelCommand;
import me.nakilex.levelplugin.player.mining.commands.SetMiningLevelCommand;
import me.nakilex.levelplugin.player.level.commands.SetLevelCommand;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.battlepass.command.BattlePassCommand;
import me.nakilex.levelplugin.player.battlepass.command.BattlePassUnlockCommand;
import me.nakilex.levelplugin.player.battlepass.command.BattlePassXpCommand;
import me.nakilex.levelplugin.booster.BoosterCommand;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.player.commands.WipeProfileCommand;
import me.nakilex.levelplugin.player.commands.ViewProfileCommand;
import me.nakilex.levelplugin.potions.commands.AddPotionCommand;
import me.nakilex.levelplugin.lootchests.commands.LootChestCommand;
import me.nakilex.levelplugin.salvage.commands.SalvageCommand;
import me.nakilex.levelplugin.settings.commands.SettingsCommand;
import me.nakilex.levelplugin.settings.gui.SettingsGUI;
import me.nakilex.levelplugin.spells.commands.AddSpellPointsCommand;
import me.nakilex.levelplugin.spells.commands.SpellUpgradeCommand;
import me.nakilex.levelplugin.spells.commands.SpellSummonCommand;
import me.nakilex.levelplugin.spells.gui.SpellUpgradeGUI;
import me.nakilex.levelplugin.spells.gui.SpellSummonGUI;
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
import me.nakilex.levelplugin.mail.MailAdminCommand;
import me.nakilex.levelplugin.mail.MailCommand;
import me.nakilex.levelplugin.cursormenu.CursorMenuCommand;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantCommand;
import me.nakilex.levelplugin.npc.wandering.WanderingMerchantManager;
import me.nakilex.levelplugin.npc.commands.NpcCommand;
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
import me.nakilex.levelplugin.environment.CoopCommand;
import me.nakilex.levelplugin.environment.KingdomCommand;
import me.nakilex.levelplugin.environment.UpgradeGUI;
import me.nakilex.levelplugin.environment.stage.TownStageCommand;
import me.nakilex.levelplugin.environment.stage.BuildingStageCommand;
import me.nakilex.levelplugin.environment.stage.TownPosCommand;
import me.nakilex.levelplugin.environment.stage.StageSelectionListener;
import me.nakilex.levelplugin.utils.commands.CenterGuiCommand;
import me.nakilex.levelplugin.utils.commands.BlockGlowCommand;
import me.nakilex.levelplugin.utils.commands.CenterTooltipCommand;
import me.nakilex.levelplugin.utils.commands.EndDialogCommand;
import me.nakilex.levelplugin.utils.commands.EmptyTabCompleter;
import me.nakilex.levelplugin.utils.FeatureFlagUtil;
import me.nakilex.levelplugin.pathfinding.PathfindingCommand;
import me.nakilex.levelplugin.pathfinding.PathfindingManager;
import me.nakilex.levelplugin.pathfinding.DungeonExpeditionManager;
import me.nakilex.levelplugin.pathfinding.MercenaryCommand;
import me.nakilex.levelplugin.chat.ChatModerationCommand;
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.commands.LevelPluginCommand;
import me.nakilex.levelplugin.chat.ChatCommand;
import me.nakilex.levelplugin.chat.EmotesCommand;
import me.nakilex.levelplugin.chat.RollCommand;
import me.nakilex.levelplugin.server.ConnectCommand;
import me.nakilex.levelplugin.server.HubCommand;
import me.nakilex.levelplugin.server.ServerSelectionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
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
                                        SpellUpgradeGUI spellUpgradeGUI,
                                        SpellSummonGUI spellSummonGUI,
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
                                        DpsDummyManager dpsDummyManager,
                                        BeaconEntityDebugManager beaconEntityDebugManager,
                                        DungeonExpeditionManager dungeonExpeditionManager,
                                        ServerSelectionManager serverSelectionManager,
                                        PetManager petManager,
                                        PetGUI petGUI,
                                        me.nakilex.levelplugin.pet.gui.PetSettingsGUI petSettingsGUI,
                                        me.nakilex.levelplugin.pet.gui.PetMergeGUI petMergeGUI,
                                        PetSummonGUI petSummonGUI,
                                        CustomMobManager customMobManager,
                                        me.nakilex.levelplugin.debug.ArcSlashDebugManager arcSlashDebugManager,
                                        me.nakilex.levelplugin.debug.gui.ArcSlashDebugGUI arcSlashDebugGUI) {


        AddPointsCommand addPointsCmd = new AddPointsCommand();
        plugin.getCommand("addpoints").setExecutor(addPointsCmd);
        plugin.getCommand("addpoints").setTabCompleter(addPointsCmd);
        AddSpellPointsCommand addSpellPointsCommand = new AddSpellPointsCommand();
        plugin.getCommand("addsp").setExecutor(addSpellPointsCommand);
        plugin.getCommand("addsp").setTabCompleter(addSpellPointsCommand);
        SpellUpgradeCommand spellsCommand = new SpellUpgradeCommand(spellUpgradeGUI);
        plugin.getCommand("spells").setExecutor(spellsCommand);
        plugin.getCommand("spellupgrade").setExecutor(spellsCommand);

        AddXPCommand addXpCmd = new AddXPCommand(levelManager);
        plugin.getCommand("addxp").setExecutor(addXpCmd);
        plugin.getCommand("addxp").setTabCompleter(addXpCmd);

        LifeSkillCommand lifeSkillCommand = new LifeSkillCommand(plugin);
        plugin.getCommand("lifeskill").setExecutor(lifeSkillCommand);
        plugin.getCommand("lifeskill").setTabCompleter(lifeSkillCommand);

        var miningLegacy = LifeSkillCommand.legacyHandler(me.nakilex.levelplugin.items.tools.ToolDiscipline.MINING);
        plugin.getCommand("addminingxp").setExecutor(miningLegacy);
        plugin.getCommand("addminingxp").setTabCompleter(miningLegacy);

        var farmingLegacy = LifeSkillCommand.legacyHandler(me.nakilex.levelplugin.items.tools.ToolDiscipline.FARMING);
        plugin.getCommand("addfarmingxp").setExecutor(farmingLegacy);
        plugin.getCommand("addfarmingxp").setTabCompleter(farmingLegacy);

        var fishingLegacy = LifeSkillCommand.legacyHandler(me.nakilex.levelplugin.items.tools.ToolDiscipline.FISHING);
        plugin.getCommand("addfishingxp").setExecutor(fishingLegacy);
        plugin.getCommand("addfishingxp").setTabCompleter(fishingLegacy);

        var woodcuttingLegacy = LifeSkillCommand.legacyHandler(me.nakilex.levelplugin.items.tools.ToolDiscipline.WOODCUTTING);
        plugin.getCommand("addwoodcuttingxp").setExecutor(woodcuttingLegacy);
        plugin.getCommand("addwoodcuttingxp").setTabCompleter(woodcuttingLegacy);

        plugin.getCommand("mininglevel").setExecutor(new MiningLevelCommand(miningManager));
        plugin.getCommand("farminglevel").setExecutor(new FarmingLevelCommand(plugin.getFarmingManager()));
        new me.nakilex.levelplugin.player.farming.gui.FarmingRewardsGUI(plugin, economyManager);
        new me.nakilex.levelplugin.player.farming.gui.FarmFieldsGUI(plugin, plugin.getEnvironmentManager());
        new me.nakilex.levelplugin.player.fishing.gui.FishingRewardsGUI(plugin, economyManager);

        me.nakilex.levelplugin.player.mining.commands.SetMiningLevelCommand setMiningLevelCmd =
                new me.nakilex.levelplugin.player.mining.commands.SetMiningLevelCommand(miningManager);
        plugin.getCommand("setmininglevel").setExecutor(setMiningLevelCmd);
        plugin.getCommand("setmininglevel").setTabCompleter(setMiningLevelCmd);
        plugin.getCommand("stats").setExecutor(new StatsCommand());
        ViewProfileCommand viewProfileCmd = new ViewProfileCommand(levelManager);
        plugin.getCommand("viewprofile").setExecutor(viewProfileCmd);
        plugin.getCommand("viewprofile").setTabCompleter(viewProfileCmd);
        AddItemCommand addItemCmd = new AddItemCommand();
        plugin.getCommand("additem").setExecutor(addItemCmd);
        plugin.getCommand("additem").setTabCompleter(addItemCmd);
        plugin.getCommand("opsword").setExecutor(new OpSwordCommand());
        me.nakilex.levelplugin.items.commands.GenerateItemCommand genItemCmd = new me.nakilex.levelplugin.items.commands.GenerateItemCommand();
        plugin.getCommand("genitem").setExecutor(genItemCmd);
        plugin.getCommand("genitem").setTabCompleter(genItemCmd);
        EndDialogCommand endDialogCommand = new EndDialogCommand();
        plugin.getCommand("enddialog").setExecutor(endDialogCommand);
        plugin.getCommand("enddialog").setTabCompleter(endDialogCommand);
        SetLevelCommand setLevelCmd = new SetLevelCommand(plugin);
        plugin.getCommand("setlevel").setExecutor(setLevelCmd);
        plugin.getCommand("setlevel").setTabCompleter(setLevelCmd);
        plugin.getCommand("balance").setExecutor(new BalanceCommand(economyManager));
        AddCoinsCommand addCoinsCmd = new AddCoinsCommand(economyManager);
        plugin.getCommand("addcoins").setExecutor(addCoinsCmd);
        plugin.getCommand("addcoins").setTabCompleter(addCoinsCmd);
        boolean boosterSystemEnabled = FeatureFlagUtil.isEnabled("features.booster-system", false);
        if (boosterSystemEnabled) {
            BoosterCommand boosterCommand = new BoosterCommand(2.0);
            plugin.getCommand("booster").setExecutor(boosterCommand);
            plugin.getCommand("booster").setTabCompleter(boosterCommand);
        } else {
            registerArchivedCommand(plugin.getCommand("booster"), "Booster");
        }
        plugin.getCommand("blacksmith").setExecutor(new BlacksmithCommand(blacksmithGUI));
        HorseCommand horseCommand = new HorseCommand(horseManager, horseGUI);
        plugin.getCommand("horse").setExecutor(horseCommand);
        plugin.getCommand("horse").setTabCompleter(horseCommand);
        PartyCommands partyCommands = new PartyCommands(partyManager);
        plugin.getCommand("party").setExecutor(partyCommands);
        plugin.getCommand("party").setTabCompleter(partyCommands);
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
        LootChestCommand lootChestCommand = new LootChestCommand(configManager, lootChestManager);
        plugin.getCommand("lootchest").setExecutor(lootChestCommand);
        plugin.getCommand("lootchest").setTabCompleter(lootChestCommand);
        TradeCommand tradeCmd = new TradeCommand();
        plugin.getCommand("trade").setExecutor(tradeCmd);
        plugin.getCommand("trade").setTabCompleter(tradeCmd);
        boolean duelSystemEnabled = FeatureFlagUtil.isEnabled("features.duel-system", false);
        if (duelSystemEnabled) {
            DuelCommand duelCommand = new DuelCommand();
            plugin.getCommand("duel").setExecutor(duelCommand);
            plugin.getCommand("duel").setTabCompleter(duelCommand);
        } else {
            registerArchivedCommand(plugin.getCommand("duel"), "Duel");
        }
        boolean arenaSystemEnabled = FeatureFlagUtil.isEnabled("features.arena-system", false);
        if (arenaSystemEnabled && plugin.getArenaQueueGUI() != null && plugin.getArenaQueueManager() != null) {
            ArenaCommand arenaCmd = new ArenaCommand(plugin.getArenaQueueGUI(), plugin.getArenaQueueManager());
            plugin.getCommand("arena").setExecutor(arenaCmd);
            plugin.getCommand("arena").setTabCompleter(arenaCmd);
        } else {
            registerArchivedCommand(plugin.getCommand("arena"), "Arena");
        }
        StrongholdCommand strongholdCmd = new StrongholdCommand(
                plugin,
                plugin.getStrongholdQueueGUI(),
                plugin.getStrongholdQueueManager(),
                plugin.getStrongholdShrineManager());
        plugin.getCommand("stronghold").setExecutor(strongholdCmd);
        plugin.getCommand("stronghold").setTabCompleter(strongholdCmd);
        plugin.getCommand("ps").setExecutor(new StorageCommand(storageManager));
        MerchantCommand merchantCommand = new MerchantCommand(plugin);
        plugin.getCommand("merchant").setExecutor(merchantCommand);
        plugin.getCommand("merchant").setTabCompleter(merchantCommand);
        plugin.getCommand("salvage").setExecutor(new SalvageCommand(plugin));
        plugin.getCommand("enchant").setExecutor(new me.nakilex.levelplugin.enchanting.commands.EnchantCommand(enchantGUI));
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
        if (dpsDummyManager != null) {
            DpsDummyCommand dummyCmd = new DpsDummyCommand(dpsDummyManager);
            plugin.getCommand("dpsdummy").setExecutor(dummyCmd);
            plugin.getCommand("dpsdummy").setTabCompleter(dummyCmd);
        }
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
        MailCommand mailCommand = MailCommand.getInstance();
        plugin.getCommand("mail").setExecutor(mailCommand);
        plugin.getCommand("mail").setTabCompleter(mailCommand);
        MailAdminCommand mailAdminCommand = MailAdminCommand.getInstance();
        plugin.getCommand("mailadmin").setExecutor(mailAdminCommand);
        plugin.getCommand("mailadmin").setTabCompleter(mailAdminCommand);
        plugin.getCommand("wipeprofile").setExecutor(new WipeProfileCommand());

        DebugCommand debugCmd = new DebugCommand(mobDebugToggleManager,
                plugin.getScoreboardManager(),
                debugGUI,
                chatGameManager,
                plugin.getMercenaryExpeditionManager(),
                dungeonExpeditionManager,
                plugin.getDropDebugManager(),
                plugin.getEnvironmentManager(),
                beaconEntityDebugManager,
                questManager,
                arcSlashDebugManager,
                arcSlashDebugGUI,
                petManager);
        plugin.getCommand("debug").setExecutor(debugCmd);
        plugin.getCommand("debug").setTabCompleter(debugCmd);
        SpawnEntityModelCommand spawnEntityModelCommand = new SpawnEntityModelCommand(plugin);
        plugin.getCommand("se").setExecutor(spawnEntityModelCommand);
        plugin.getCommand("se").setTabCompleter(spawnEntityModelCommand);
        MageFireballDebugCommand mageFireballDebugCommand = new MageFireballDebugCommand(plugin);
        plugin.getCommand("fireballdebug").setExecutor(mageFireballDebugCommand);
        plugin.getCommand("fireballdebug").setTabCompleter(mageFireballDebugCommand);
        CustomMobCommand customMobCommand = new CustomMobCommand(customMobManager);
        plugin.getCommand("custommob").setExecutor(customMobCommand);
        plugin.getCommand("custommob").setTabCompleter(customMobCommand);
        if (petManager != null) {
            PetDebugCommand petDebugCommand = new PetDebugCommand(petManager);
            plugin.getCommand("petdebug").setExecutor(petDebugCommand);
            plugin.getCommand("petdebug").setTabCompleter(petDebugCommand);
        }
        if (petManager != null && petGUI != null) {
            PetCommand petCommand = new PetCommand(petManager, petGUI);
            plugin.getCommand("pet").setExecutor(petCommand);
            plugin.getCommand("pet").setTabCompleter(petCommand);
        }
        if (petSummonGUI != null) {
            PetSummonCommand petSummonCommand = new PetSummonCommand(petSummonGUI);
            plugin.getCommand("petsummon").setExecutor(petSummonCommand);
        }
        if (spellSummonGUI != null) {
            SpellSummonCommand spellSummonCommand = new SpellSummonCommand(spellSummonGUI);
            plugin.getCommand("spellsummon").setExecutor(spellSummonCommand);
        }

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
        CoopCommand coopCommand = new CoopCommand(plugin.getEnvironmentManager());
        plugin.getCommand("coop").setExecutor(coopCommand);
        plugin.getCommand("coop").setTabCompleter(coopCommand);
        KingdomCommand kingdomCommand = new KingdomCommand(me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager.getInstance(plugin));
        plugin.getCommand("kingdom").setExecutor(kingdomCommand);
        plugin.getCommand("kingdom").setTabCompleter(kingdomCommand);
        new me.nakilex.levelplugin.environment.PalaceGUI(plugin, me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager.getInstance(plugin));
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

        EmotesCommand emotesCommand = new EmotesCommand();
        plugin.getCommand("emotes").setExecutor(emotesCommand);
        plugin.getCommand("emotes").setTabCompleter(emotesCommand);

        plugin.getCommand("roll").setExecutor(new RollCommand());
        NpcCommand npcCommand = new NpcCommand();
        plugin.getCommand("npc").setExecutor(npcCommand);
        plugin.getCommand("npc").setTabCompleter(npcCommand);

        if (plugin.getBlockGlowUtil() != null) {
            BlockGlowCommand blockGlowCommand = new BlockGlowCommand(plugin.getBlockGlowUtil());
            plugin.getCommand("blockglow").setExecutor(blockGlowCommand);
            plugin.getCommand("blockglow").setTabCompleter(blockGlowCommand);
        }

        if (plugin.getCursorMenuManager() != null) {
            CursorMenuCommand cursorMenuCommand = new CursorMenuCommand(plugin.getCursorMenuManager());
            plugin.getCommand("cursormenu").setExecutor(cursorMenuCommand);
            plugin.getCommand("cursormenu").setTabCompleter(cursorMenuCommand);
        }

        if (serverSelectionManager != null) {
            ConnectCommand connectCommand = new ConnectCommand(serverSelectionManager);
            plugin.getCommand("connect").setExecutor(connectCommand);
            plugin.getCommand("connect").setTabCompleter(connectCommand);
            plugin.getCommand("hub").setExecutor(new HubCommand(serverSelectionManager));
        }

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

    private static void registerArchivedCommand(PluginCommand command, String systemName) {
        TabExecutor archivedHandler = new TabExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                sender.sendMessage(ChatColor.RED + systemName + " system is currently archived.");
                return true;
            }

            @Override
            public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
                return java.util.Collections.emptyList();
            }
        };
        command.setExecutor(archivedHandler);
        command.setTabCompleter(archivedHandler);
    }
}
