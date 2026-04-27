package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.quests.def.GamblersGambitQuest;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.party.synergy.PartySynergyProfile;
import me.nakilex.levelplugin.party.synergy.PartySynergyUtil;
import me.nakilex.levelplugin.progression.objectives.ObjectiveProgressBus;
import me.nakilex.levelplugin.progression.objectives.ObjectiveProgressEvent;
import me.nakilex.levelplugin.progression.objectives.ObjectiveType;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ExperienceUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MobRewardService {

    public record DebugInfo(String templateEntityName, String bukkitEntityName, boolean numericHpName) {
    }

    public record MobRewardContext(String mobId,
                                   String displayName,
                                   int level,
                                   int combatPower,
                                   LivingEntity entity,
                                   Set<Player> participants,
                                   DebugInfo debugInfo) {
    }

    private final Main plugin;
    private final MobRewardsConfig mobRewardsConfig;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;
    private final LootChestManager lootChestManager;
    private final ModelSetManager modelSetManager;
    private final BattlePassManager battlePassManager;
    private final PlayerToggleManager debugToggle;
    private final me.nakilex.levelplugin.debug.DropDebugManager dropDebugManager;
    private final ItemDropper itemDropper;

    public MobRewardService(Main plugin,
                            MobRewardsConfig mobRewardsConfig,
                            LevelManager levelManager,
                            EconomyManager economyManager,
                            LootChestManager lootChestManager,
                            ModelSetManager modelSetManager,
                            PlayerToggleManager debugToggle,
                            BattlePassManager battlePassManager,
                            me.nakilex.levelplugin.debug.DropDebugManager dropDebugManager) {
        this.plugin = plugin;
        this.mobRewardsConfig = mobRewardsConfig;
        this.levelManager = levelManager;
        this.economyManager = economyManager;
        this.lootChestManager = lootChestManager;
        this.modelSetManager = modelSetManager;
        this.debugToggle = debugToggle;
        this.battlePassManager = battlePassManager;
        this.dropDebugManager = dropDebugManager;
        this.itemDropper = new ItemDropper(modelSetManager);
    }

    public void awardRewards(MobRewardContext context) {
        if (context == null || context.entity() == null) {
            return;
        }
        Set<Player> participants = context.participants() == null
                ? Collections.emptySet()
                : context.participants();

        me.nakilex.levelplugin.quests.managers.QuestManager questManager = plugin.getQuestManager();
        if (questManager != null && !participants.isEmpty()) {
            Player killer = context.entity().getKiller();
            for (Player participant : participants) {
                if (participant == null) {
                    continue;
                }
                if (killer != null && participant.getUniqueId().equals(killer.getUniqueId())) {
                    continue;
                }
                questManager.handleKill(participant, context.mobId(), false);
            }
        }

        ConfigurationSection node = mobRewardsConfig.getMobSection(context.mobId());
        if (node == null) {
            for (Player player : participants) {
                if (debugToggle.isEnabled(player)) {
                    sendDebugInfo(player, context);
                    player.sendMessage(ChatColor.RED + "[MobDebug] No rewards configured");
                }
            }
            return;
        }

        String mobType = node.getName();
        int combatPower = context.combatPower();
        int mobLevel = context.level();
        int exp = CombatRewardCalculator.calculateXpReward(combatPower);
        int coins = CombatRewardCalculator.calculateCoinReward(combatPower);
        boolean forceDrops = dropDebugManager != null && dropDebugManager.isForceMobDrops();
        double gearDropChance = dropDebugManager != null
                ? dropDebugManager.resolveDropChance(node)
                : node.getDouble("drop_override", node.getDouble("tier_chance", 12.0));
        String modelSet = node.getString("model_set", "default");

        Location deathLoc = context.entity().getLocation();
        PartyManager pm = plugin.getPartyManager();
        Set<Player> recipients = new HashSet<>();
        for (Player participant : participants) {
            Party party = pm.getParty(participant.getUniqueId());
            if (party != null) {
                for (UUID memberId : party.getMembers()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.getWorld().equals(deathLoc.getWorld())
                            && member.getLocation().distanceSquared(deathLoc) <= 10000) {
                        recipients.add(member);
                    }
                }
            } else if (participant.getWorld().equals(deathLoc.getWorld())
                    && participant.getLocation().distanceSquared(deathLoc) <= 10000) {
                recipients.add(participant);
            }
        }

        Map<Party, List<Player>> nearbyPartyMembers = new HashMap<>();
        for (Player p : recipients) {
            Party party = pm.getParty(p.getUniqueId());
            if (party != null) {
                nearbyPartyMembers.computeIfAbsent(party, k -> new java.util.ArrayList<>()).add(p);
            }
        }
        Map<Party, PartySynergyProfile> partySynergy = new HashMap<>();
        for (Map.Entry<Party, List<Player>> entry : nearbyPartyMembers.entrySet()) {
            partySynergy.put(entry.getKey(), PartySynergyUtil.profile(entry.getValue()));
        }

        me.nakilex.levelplugin.dungeon.DungeonManager dungeonManager = plugin.getDungeonManager();
        if (dungeonManager != null) {
            for (Player player : recipients) {
                dungeonManager.addCombatPowerContribution(player.getUniqueId(), combatPower);
            }
        }

        for (Player player : recipients) {
            Party party = pm.getParty(player.getUniqueId());
            int partySize = 1;
            if (party != null) {
                partySize = nearbyPartyMembers.getOrDefault(party, List.of()).size();
            }
            int scaledExp = ExperienceUtil.scaleExperience(exp, levelManager.getLevel(player), mobLevel);
            PartySynergyProfile synergyProfile = party != null
                    ? partySynergy.getOrDefault(party, PartySynergyProfile.neutral())
                    : PartySynergyProfile.neutral();
            int awardedExp = ExperienceUtil.applyPartyBonus(scaledExp, partySize, synergyProfile.multiplier());
            levelManager.addXP(player, awardedExp);
            economyManager.addCoins(player, coins);
            var settings = plugin.getSettingsManager().getSettings(player);
            itemDropper.dropCustomItems(player, node, modelSet, combatPower, mobLevel, forceDrops);
            // Essence system temporarily disabled.
            double gearDropBonus = GamblersGambitQuest.resolveDropBonus(player);
            double effectiveGearChance = gearDropChance + gearDropBonus;
            double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
            if (forceDrops || roll <= effectiveGearChance) {
                ItemStack loot = lootChestManager.getRandomLootForCombatPower(combatPower, mobLevel, mobType, modelSet, false);
                if (loot != null) {
                    ItemUtil.updateTooltip(loot, player);
                    var rarity = ItemUtil.getCustomItemRarity(loot);
                    if (rarity != null && ItemUtil.isWeaponOrArmor(loot)
                            && !settings.isLootPickupAllowed(rarity)) {
                        player.getWorld().dropItemNaturally(player.getLocation(), loot);
                    } else {
                        var runManager = Main.getInstance().getStrongholdRunManager();
                        boolean routedToStorage = runManager != null && runManager.storeLootToResultStorage(player, loot);
                        if (!routedToStorage) {
                            player.getInventory().addItem(loot).values()
                                    .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
                        }
                    }
                }
            }
            itemDropper.maybeDropRerollScroll(player);
            if (settings.isDropDetailsEnabled()) {
                RewardHologramUtil.showRewardHologram(deathLoc, awardedExp, coins);
            }
            if (settings.isDropDetailsChatEnabled()) {
                String expLabel = ChatFormatter.experienceLabel();
                String expColor = ChatFormatter.experienceColor();
                player.sendMessage(ChatColor.GOLD + "You received "
                        + expColor + "+" + awardedExp + " <glyph:experience_orb_icon> " + expLabel
                        + ChatColor.GOLD + " and "
                        + me.nakilex.levelplugin.utils.CurrencyMessageUtil.formatAmount(
                        me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, coins)
                        + ChatColor.GOLD + "!");
                if (party != null && synergyProfile.multiplier() > 1.0) {
                    player.sendMessage(ChatColor.GRAY + "Party " + ChatColor.GREEN + synergyProfile.summary()
                            + ChatColor.GRAY + " boosted your XP.");
                }
            }
            GuildQuestManager.getInstance().handleKill(player, mobType);
            ObjectiveProgressBus.getInstance().publish(new ObjectiveProgressEvent(
                    player.getUniqueId(),
                    ObjectiveType.KILL_MOB,
                    mobType,
                    1
            ));
            maybeAwardBattlePassXp(player, context.displayName(), combatPower, awardedExp);
            if (debugToggle.isEnabled(player)) {
                sendDebugInfo(player, context);
                String expColor = ChatFormatter.experienceColor();
                player.sendMessage(ChatColor.YELLOW + "[MobDebug] Exp: " + expColor + awardedExp
                        + ChatColor.GRAY + ", Coins: " + coins);
            }
        }
    }

    private void maybeAwardBattlePassXp(Player player, String displayName, int combatPower, int awardedExp) {
        if (battlePassManager == null || player == null) {
            return;
        }
        int battlePassXp = calculateBattlePassXp(combatPower, awardedExp);
        if (battlePassXp <= 0) {
            return;
        }
        String cleaned = ChatColor.stripColor(displayName);
        if (cleaned == null || cleaned.isBlank()) {
            cleaned = displayName;
        }
        battlePassManager.addProgress(
                player,
                battlePassXp,
                "for defeating " + ChatColor.GOLD + cleaned
        );
    }

    private int calculateBattlePassXp(int combatPower, int awardedExp) {
        int base = Math.max(25, awardedExp / 4);
        base += Math.min(200, Math.max(0, combatPower / 5));
        return Math.min(base, 500);
    }

    private void sendDebugInfo(Player player, MobRewardContext context) {
        DebugInfo debug = context.debugInfo();
        String template = debug != null ? debug.templateEntityName() : "?";
        String bukkitName = debug != null ? debug.bukkitEntityName() : "?";
        boolean numericHpName = debug != null && debug.numericHpName();
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] ID: " + context.mobId()
                + ChatColor.GRAY + " Display: " + ChatColor.WHITE + context.displayName());
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Template Entity: "
                + ChatColor.WHITE + template);
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Bukkit Entity: "
                + ChatColor.WHITE + bukkitName);
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Combat Power: " + ChatColor.AQUA + context.combatPower());
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Numeric HP: " + ChatColor.WHITE + numericHpName);
    }
}
