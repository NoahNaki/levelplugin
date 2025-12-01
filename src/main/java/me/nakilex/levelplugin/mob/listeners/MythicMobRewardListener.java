package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import me.nakilex.levelplugin.mob.utils.ItemDropper;
import me.nakilex.levelplugin.mob.utils.RewardHologramUtil;
import me.nakilex.levelplugin.mob.utils.CombatPowerUtil;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.ExperienceUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Awards XP, coins and loot to players that participated in killing MythicMobs.
 */
public class MythicMobRewardListener implements Listener {
    private final BukkitAPIHelper mythicHelper = MythicBukkit.inst().getAPIHelper();
    private final MobRewardsConfig mobRewardsConfig;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;
    private final LootChestManager lootChestManager;
    private final ModelSetManager modelSetManager;
    private final MythicMobDamageTracker tracker;
    private final BattlePassManager battlePassManager;
    private final ItemDropper itemDropper;
    private final PlayerToggleManager debugToggle;

    public MythicMobRewardListener(MythicMobDamageTracker tracker,
                                   MobRewardsConfig mobRewardsConfig,
                                   LevelManager levelManager,
                                   EconomyManager economyManager,
                                   LootChestManager lootChestManager,
                                   ModelSetManager modelSetManager,
                                   PlayerToggleManager debugToggle,
                                   BattlePassManager battlePassManager) {
        this.tracker = tracker;
        this.mobRewardsConfig = mobRewardsConfig;
        this.levelManager = levelManager;
        this.economyManager = economyManager;
        this.lootChestManager = lootChestManager;
        this.modelSetManager = modelSetManager;
        this.itemDropper = new ItemDropper(modelSetManager);
        this.debugToggle = debugToggle;
        this.battlePassManager = battlePassManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mythicMob == null) return;

        String rawMobType = mythicMob.getMobType().replaceAll("§.", "");
        Set<Player> participants = tracker.getParticipantsAndClear(event.getEntity().getUniqueId());
        if (participants.isEmpty() && event.getEntity().getKiller() instanceof Player killer) {
            participants = Set.of(killer);
        }
        if (participants.isEmpty()) {
            participants = Collections.emptySet();
        }

        ConfigurationSection node = mobRewardsConfig.getMobSection(rawMobType);
        Entity baseEntity = mythicMob.getEntity().getBukkitEntity();
        boolean numericHpName = baseEntity instanceof LivingEntity living && MobNameUtil.hasNumericHealth(living);
        if (node == null) {
            for (Player player : participants) {
                if (debugToggle.isEnabled(player)) {
                    sendDebugInfo(player, rawMobType, mythicMob, baseEntity, numericHpName);
                    player.sendMessage(ChatColor.RED + "[MobDebug] No rewards configured");
                }
            }
            return;
        }

        String mobType = node.getName();

        int combatPower = CombatPowerUtil.getCombatPower(mythicMob);
        int exp = CombatRewardCalculator.calculateXpReward(combatPower);
        int coins = CombatRewardCalculator.calculateCoinReward(combatPower);
        double tierChance = node.getDouble("tier_chance", 100.0);
        String modelSet = node.getString("model_set", null);

        Location deathLoc = event.getEntity().getLocation();
        PartyManager pm = Main.getInstance().getPartyManager();
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

        for (Player player : recipients) {
            Party party = pm.getParty(player.getUniqueId());
            int partySize = 1;
            if (party != null) {
                partySize = nearbyPartyMembers.getOrDefault(party, List.of()).size();
            }
            int scaledExp = ExperienceUtil.scaleExperience(exp, levelManager.getLevel(player), mythicMob.getLevel());
            int awardedExp = ExperienceUtil.applyPartyBonus(scaledExp, partySize);
            levelManager.addXP(player, awardedExp);
            economyManager.addCoins(player, coins);
            itemDropper.dropCustomItems(player, node, modelSet, combatPower);
            itemDropper.maybeDropEssence(player, node);
            double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
            if (roll <= tierChance) {
                ItemStack loot = lootChestManager.getRandomLootForCombatPower(combatPower, mobType, modelSet);
                if (loot != null) {
                    ItemUtil.updateTooltip(loot, player);
                    player.getInventory().addItem(loot).values()
                            .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
                }
            }
            itemDropper.maybeDropRerollScroll(player);
            var settings = Main.getInstance().getSettingsManager().getSettings(player);
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
            }
            GuildQuestManager.getInstance().handleKill(player, mobType);
            maybeAwardBattlePassXp(player, mobType, combatPower, awardedExp);
            if (debugToggle.isEnabled(player)) {
                sendDebugInfo(player, rawMobType, mythicMob, baseEntity, numericHpName);
                String expColor = ChatFormatter.experienceColor();
                player.sendMessage(ChatColor.YELLOW + "[MobDebug] Exp: " + expColor + awardedExp
                        + ChatColor.GRAY + ", Coins: " + coins);
            }
        }
    }

    private void maybeAwardBattlePassXp(Player player, String mobType, int combatPower, int awardedExp) {
        if (battlePassManager == null || player == null) {
            return;
        }
        int battlePassXp = calculateBattlePassXp(combatPower, awardedExp);
        if (battlePassXp <= 0) {
            return;
        }
        String displayName = ChatColor.stripColor(MobNameUtil.getDisplayName(mobType));
        if (displayName == null || displayName.isBlank()) {
            displayName = MobNameUtil.toPrettyName(mobType);
        }
        battlePassManager.addProgress(
                player,
                battlePassXp,
                "for defeating " + ChatColor.GOLD + displayName
        );
    }

    private int calculateBattlePassXp(int combatPower, int awardedExp) {
        int base = Math.max(25, awardedExp / 4);
        base += Math.min(200, Math.max(0, combatPower / 5));
        return Math.min(base, 500);
    }

    private void sendDebugInfo(Player player,
                               String rawMobType,
                               ActiveMob mythicMob,
                               Entity baseEntity,
                               boolean numericHpName) {
        PlaceholderString name = mythicMob.getType().getDisplayName();
        String display = name != null ? name.get() : rawMobType;
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] ID: " + rawMobType
                + ChatColor.GRAY + " Display: " + ChatColor.WHITE + display);
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Template Entity: "
                + ChatColor.WHITE + mythicMob.getType().getEntityType().name());
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Bukkit Entity: "
                + ChatColor.WHITE + baseEntity.getType()
                + ChatColor.GRAY + " (" + baseEntity.getClass().getSimpleName() + ")");
        int power = CombatPowerUtil.getCombatPower(mythicMob);
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Combat Power: " + ChatColor.AQUA + power);
        player.sendMessage(ChatColor.YELLOW + "[MobDebug] Numeric HP: " + ChatColor.WHITE + numericHpName);
    }
}
