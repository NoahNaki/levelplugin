package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import me.nakilex.levelplugin.mob.utils.DropDisplayToggles;
import me.nakilex.levelplugin.mob.utils.ItemDropper;
import me.nakilex.levelplugin.mob.utils.RewardHologramUtil;
import me.nakilex.levelplugin.mob.utils.CombatPowerUtil;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.Set;
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
    private final ItemDropper itemDropper;
    private final PlayerToggleManager debugToggle;

    public MythicMobRewardListener(MythicMobDamageTracker tracker,
                                   MobRewardsConfig mobRewardsConfig,
                                   LevelManager levelManager,
                                   EconomyManager economyManager,
                                   LootChestManager lootChestManager,
                                   ModelSetManager modelSetManager,
                                   PlayerToggleManager debugToggle) {
        this.tracker = tracker;
        this.mobRewardsConfig = mobRewardsConfig;
        this.levelManager = levelManager;
        this.economyManager = economyManager;
        this.lootChestManager = lootChestManager;
        this.modelSetManager = modelSetManager;
        this.itemDropper = new ItemDropper(levelManager, mobRewardsConfig, modelSetManager);
        this.debugToggle = debugToggle;
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
        LivingEntity bukkitEntity = mythicMob.getEntity().getBukkitEntity();
        boolean numericHpName = MobNameUtil.hasNumericHealth(bukkitEntity);
        if (node == null) {
            for (Player player : participants) {
                if (debugToggle.isEnabled(player)) {
                    PlaceholderString name = mythicMob.getType().getDisplayName();
                    String display = name != null ? name.get() : rawMobType;
                    player.sendMessage(ChatColor.YELLOW + "[MobDebug] ID: " + rawMobType
                            + ChatColor.GRAY + " Display: " + ChatColor.WHITE + display);
                    int power = CombatPowerUtil.getCombatPower(mythicMob);
                    player.sendMessage(ChatColor.YELLOW + "[MobDebug] Combat Power: " + ChatColor.AQUA + power);
                    player.sendMessage(ChatColor.YELLOW + "[MobDebug] Numeric HP: " + ChatColor.WHITE + numericHpName);
                    player.sendMessage(ChatColor.RED + "[MobDebug] No rewards configured");
                }
            }
            return;
        }

        String mobType = node.getName();

        int exp = node.getInt("exp", 0);
        String coinsSpec = node.getString("coins", "0-0");
        int tier = node.getInt("tier", 0);
        double tierChance = node.getDouble("tier_chance", 100.0);
        String modelSet = node.getString("model_set", null);
        String[] sp = coinsSpec.split("-");
        int minCoins = Integer.parseInt(sp[0]);
        int maxCoins = Integer.parseInt(sp[1]);

        for (Player player : participants) {
            PartyManager pm = Main.getInstance().getPartyManager();
            Party party = pm.getParty(player.getUniqueId());
            int bonusPercent = 0;
            if (party != null) {
                int size = party.getSize();
                bonusPercent = Math.min(Math.max(size - 1, 0), 3) * 10;
            }
            int awardedExp = exp + (exp * bonusPercent) / 100;
            levelManager.addXP(player, awardedExp);
            int coins = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
            economyManager.addCoins(player, coins);
            itemDropper.dropCustomItems(player, node, modelSet);
            if (tier > 0) {
                double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
                if (roll <= tierChance) {
                    ItemStack loot = lootChestManager.getRandomLootForTier(tier, mobType, modelSet);
                    if (loot != null) {
                        ItemUtil.updateTooltip(loot, player);
                        player.getInventory().addItem(loot).values()
                                .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
                    }
                }
            }
            itemDropper.maybeDropRerollScroll(player);
            if (DropDisplayToggles.isDropDetailsEnabled(player)) {
                Location deathLoc = event.getEntity().getLocation();
                RewardHologramUtil.showRewardHologram(deathLoc, awardedExp, coins);
            }
            if (DropDisplayToggles.isChatEnabled(player)) {
                String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
                player.sendMessage(ChatColor.GOLD + "You received "
                        + ChatColor.WHITE + "+" + awardedExp + " <glyph:experience_orb_icon> " + expLabel
                        + ChatColor.GOLD + " and "
                        + me.nakilex.levelplugin.utils.CurrencyMessageUtil.formatAmount(
                                me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, coins)
                        + ChatColor.GOLD + "!");
            }
            if (debugToggle.isEnabled(player)) {
                PlaceholderString name = mythicMob.getType().getDisplayName();
                String display = name != null ? name.get() : mobType;
                player.sendMessage(ChatColor.YELLOW + "[MobDebug] ID: " + mobType
                        + ChatColor.GRAY + " Display: " + ChatColor.WHITE + display);
                player.sendMessage(ChatColor.YELLOW + "[MobDebug] Exp: " + awardedExp
                        + ChatColor.GRAY + ", Coins: " + coins);
                int power = CombatPowerUtil.getCombatPower(mythicMob);
                player.sendMessage(ChatColor.YELLOW + "[MobDebug] Combat Power: " + ChatColor.AQUA + power);
                player.sendMessage(ChatColor.YELLOW + "[MobDebug] Numeric HP: " + ChatColor.WHITE + numericHpName);
            }
        }
    }
}
